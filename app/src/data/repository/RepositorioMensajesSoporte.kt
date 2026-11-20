package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.SupportMessage
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

internal class RepositorioMensajesSoporte(private val appContext: Context) {

    private val messagesList = CopyOnWriteArrayList<SupportMessage>()
    private val messagesFlow = MutableStateFlow<List<SupportMessage>>(emptyList())

    @Volatile
    private var initialized = false

    private fun activeBackendToken(): String? {
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    private fun welcomeMessage(): SupportMessage = SupportMessage(
        id = "welcome",
        userId = optionalUserId(appContext).orEmpty(),
        sender = "support",
        senderName = "Concierge Elegan",
        text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
    )

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            if (messagesList.isEmpty()) {
                messagesList.add(welcomeMessage())
            }
            messagesFlow.value = messagesList.toList()
            initialized = true
        }
    }

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        ensureInitialized()
        messagesFlow.collect { emit(it) }
    }

    suspend fun refreshMessages(): List<SupportMessage> = withContext(Dispatchers.IO) {
        ensureInitialized()
        val token = activeBackendToken() ?: return@withContext messagesList.toList()
        try {
            val response = ApiAutenticacionMovil.service.getMessages("Bearer $token")
            val remoteMessages = extractMessagesJson(response, optionalUserId(appContext).orEmpty())
            synchronized(this) {
                messagesList.clear()
                if (remoteMessages.isEmpty()) {
                    messagesList.add(welcomeMessage())
                } else {
                    messagesList.addAll(remoteMessages)
                }
                messagesFlow.value = messagesList.toList()
            }
            messagesList.toList()
        } catch (e: Exception) {
            Log.w("EleganMessages", "No se pudieron obtener mensajes desde el backend.", e)
            messagesList.toList()
        }
    }

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
    ) = withContext(Dispatchers.IO) {
        ensureInitialized()
        val clean = text.trim()
        val cleanMedia = mediaUri.trim()
        require((clean.isNotEmpty() && clean.length <= 4000) || cleanMedia.isNotEmpty())
        val msgId = UUID.randomUUID().toString()
        val resolvedType = if (cleanMedia.isNotEmpty()) {
            if (mediaType.equals("video", ignoreCase = true)) "video" else "image"
        } else ""

        val token = activeBackendToken()
        if (token != null) {
            if (cleanMedia.isNotBlank() &&
                !cleanMedia.startsWith("https://", ignoreCase = true) &&
                !cleanMedia.startsWith("http://", ignoreCase = true)
            ) {
                throw IllegalArgumentException(
                    "El adjunto todavía no tiene una URL permanente. Vuelve a seleccionarlo e inténtalo otra vez."
                )
            }
            try {
                val payload = mapOf(
                    "text" to clean,
                    "message" to clean,
                    "mediaUrl" to cleanMedia,
                    "media_url" to cleanMedia,
                    "mediaType" to resolvedType,
                    "media_type" to resolvedType
                )
                val response = ApiAutenticacionMovil.service.sendMessage("Bearer $token", payload)
                val remoteAfter = extractMessagesJson(response, optionalUserId(appContext).orEmpty())
                if (remoteAfter.isNotEmpty()) {
                    synchronized(this) {
                        messagesList.clear()
                        messagesList.addAll(remoteAfter)
                        messagesFlow.value = messagesList.toList()
                    }
                    return@withContext
                }
                val refreshed = refreshMessages()
                if (refreshed.any { it.text == clean || (cleanMedia.isNotBlank() && it.mediaUrl == cleanMedia) }) {
                    return@withContext
                }
            } catch (e: Exception) {
                Log.w("EleganMessages", "No se pudo enviar el mensaje al backend; usando respaldo de sesión.", e)
            }
        }

        messagesList.add(
            SupportMessage(
                id = msgId,
                userId = optionalUserId(appContext).orEmpty(),
                sender = "user",
                senderName = MobileSession.user(appContext)?.name ?: "Cliente",
                text = clean,
                mediaUrl = cleanMedia,
                mediaType = resolvedType,
                createdAt = Instant.now().toString()
            )
        )
        messagesList.add(
            SupportMessage(
                id = UUID.randomUUID().toString(),
                userId = optionalUserId(appContext).orEmpty(),
                sender = "support",
                senderName = "Elegan Concierge",
                text = generarRespuestaSoporte(clean, cleanMedia),
                createdAt = Instant.now().plusMillis(1).toString()
            )
        )
        messagesFlow.value = messagesList.toList()
    }

    private fun generarRespuestaSoporte(clean: String, mediaUrl: String): String {
        val normalized = clean.lowercase()
        return when {
            clean.isEmpty() && mediaUrl.isNotEmpty() ->
                "Hemos recibido tu archivo multimedia. Un asesor de Elegan lo revisará enseguida para ayudarte."
            normalized.contains("envio") || normalized.contains("entrega") ->
                "Gracias por tu consulta. Nuestro equipo de Elegan te ayudará con la información de entrega de tu pedido."
            normalized.contains("talla") || normalized.contains("medida") ->
                "Gracias por escribirnos. Nuestro equipo puede orientarte con las tallas y medidas disponibles."
            normalized.contains("devolucion") || normalized.contains("cambio") ->
                "Recibimos tu consulta sobre cambios o devoluciones. Un asesor de Elegan te dará seguimiento."
            normalized.contains("descuento") || normalized.contains("promocion") ->
                "Gracias por consultar nuestras promociones. Revisa los anuncios y ofertas vigentes en la tienda."
            else ->
                "Gracias por comunicarte con Elegan. Hemos recibido tu consulta y nuestro equipo te dará seguimiento."
        }
    }

    fun clearUserData() {
        messagesList.clear()
        messagesFlow.value = emptyList()
        initialized = false
    }
}

internal fun extractMessagesJson(response: Map<String, Any?>, fallbackUserId: String): List<SupportMessage> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["messages"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: (response["item"] as? Map<*, *>)?.let { listOf(it) }
        ?: return emptyList()

    return rawList.mapIndexedNotNull { index, entry ->
        val map = entry as? Map<*, *> ?: return@mapIndexedNotNull null
        val id = firstMsgString(map, "id", "_id").ifBlank { "msg_$index" }
        val text = firstMsgString(map, "text", "message", "body", "content")
        val mediaUrl = firstMsgString(map, "mediaUrl", "media_url", "attachmentUrl", "attachment_url")
            .let(::resolveBackendMediaUrl)
        if (text.isBlank() && mediaUrl.isBlank()) return@mapIndexedNotNull null

        val rawSender = firstMsgString(map, "sender", "role", "from").lowercase()
        val sender = if (rawSender == "support" || rawSender == "admin" || rawSender == "superadmin") {
            "support"
        } else {
            "user"
        }
        SupportMessage(
            id = id,
            userId = firstMsgString(map, "userId", "user_id").ifBlank { fallbackUserId },
            sender = sender,
            senderName = firstMsgString(map, "senderName", "sender_name").ifBlank {
                if (sender == "support") "Elegan Concierge" else "Cliente"
            },
            text = text,
            mediaUrl = mediaUrl,
            mediaType = firstMsgString(map, "mediaType", "media_type"),
            createdAt = firstMsgString(map, "createdAt", "created_at").ifBlank { null }
        )
    }
}

private fun firstMsgString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val v = (map[key] as? String)?.trim()
        if (!v.isNullOrBlank()) return v
    }
    return ""
}
