package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.SupportMessage
import com.example.data.remote.MobileSession
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class RepositorioMensajesSoporte(private val appContext: Context) {

    private val messagesPrefs by lazy {
        appContext.getSharedPreferences("elegan_support_messages", Context.MODE_PRIVATE)
    }
    private val messagesLoadMutex = Mutex()

    @Volatile
    private var loadedMessagesKey: String? = null
    private val localMessagesList = CopyOnWriteArrayList<SupportMessage>()
    private val localMessagesFlow = MutableStateFlow<List<SupportMessage>>(emptyList())

    private fun cacheKey(userKey: String = userCacheKey(appContext)): String = "support_messages_$userKey"

    suspend fun ensureLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedMessagesKey == currentKey) return
        messagesLoadMutex.withLock {
            if (loadedMessagesKey == currentKey) return
            val welcome = SupportMessage(
                id = "welcome",
                userId = optionalUserId(appContext).orEmpty(),
                sender = "support",
                senderName = "Concierge Elegan",
                text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
            )
            val loaded = withContext(Dispatchers.IO) { loadPersistedMessages(currentKey) }
            localMessagesList.clear()
            if (loaded.isNotEmpty()) localMessagesList.addAll(loaded) else localMessagesList.add(welcome)
            localMessagesFlow.value = localMessagesList.toList()
            loadedMessagesKey = currentKey
        }
    }

    private fun loadPersistedMessages(userKey: String): List<SupportMessage> {
        val raw = messagesPrefs.getString(cacheKey(userKey), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SupportMessage(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            sender = obj.optString("sender", "user"),
                            senderName = obj.optString("senderName", ""),
                            text = obj.optString("text", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            mediaType = obj.optString("mediaType", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los mensajes locales: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedMessages(userKey: String = userCacheKey(appContext)) {
        val snapshot = localMessagesList.toList()
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (msg in snapshot) {
                    array.put(
                        JSONObject().apply {
                            put("id", msg.id)
                            put("userId", msg.userId)
                            put("sender", msg.sender)
                            put("senderName", msg.senderName)
                            put("text", msg.text)
                            put("mediaUrl", msg.mediaUrl)
                            put("mediaType", msg.mediaType)
                            put("createdAt", msg.createdAt.orEmpty())
                        }
                    )
                }
                messagesPrefs.edit().putString(cacheKey(userKey), array.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los mensajes localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishState() {
        localMessagesFlow.value = localMessagesList.toList()
        savePersistedMessages()
    }

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        ensureLoaded()
        localMessagesFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
    ) {
        ensureLoaded()
        val clean = text.trim()
        val cleanMedia = mediaUri.trim()
        require((clean.isNotEmpty() && clean.length <= 4000) || cleanMedia.isNotEmpty())
        val msgId = UUID.randomUUID().toString()
        val resolvedType = if (cleanMedia.isNotEmpty()) {
            if (mediaType.equals("video", ignoreCase = true)) "video" else "image"
        } else ""
        val localMediaUrl = if (cleanMedia.isNotEmpty()) {
            persistirMediaSoporteLocal(appContext, cleanMedia, msgId, resolvedType == "video")
        } else ""

        localMessagesList.add(
            SupportMessage(
                id = msgId,
                userId = optionalUserId(appContext).orEmpty(),
                sender = "user",
                senderName = MobileSession.user(appContext)?.name ?: "Cliente",
                text = clean,
                mediaUrl = localMediaUrl,
                mediaType = resolvedType,
                createdAt = Instant.now().toString()
            )
        )
        publishState()

        localMessagesList.add(
            SupportMessage(
                id = UUID.randomUUID().toString(),
                userId = optionalUserId(appContext).orEmpty(),
                sender = "support",
                senderName = "Elegan Concierge",
                text = generarRespuestaSoporte(clean, localMediaUrl),
                createdAt = Instant.now().plusMillis(1).toString()
            )
        )
        publishState()
    }

    private fun generarRespuestaSoporte(clean: String, localMediaUrl: String): String {
        val normalized = clean.lowercase()
        return when {
            clean.isEmpty() && localMediaUrl.isNotEmpty() ->
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

    fun mediaUrls(): List<String> = localMessagesList.map { it.mediaUrl }

    fun clearUserData(userKey: String) {
        messagesPrefs.edit().remove(cacheKey(userKey)).commit()
        localMessagesList.clear()
        localMessagesFlow.value = emptyList()
        loadedMessagesKey = null
    }
}
