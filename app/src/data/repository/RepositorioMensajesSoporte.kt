package com.example.data.repository

import android.content.Context
import com.example.data.model.SupportMessage
import com.example.data.remote.MobileSession
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

internal class RepositorioMensajesSoporte(private val appContext: Context) {

    private val messagesList = CopyOnWriteArrayList<SupportMessage>()
    private val messagesFlow = MutableStateFlow<List<SupportMessage>>(emptyList())

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            if (messagesList.isEmpty()) {
                messagesList.add(
                    SupportMessage(
                        id = "welcome",
                        userId = optionalUserId(appContext).orEmpty(),
                        sender = "support",
                        senderName = "Concierge Elegan",
                        text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
                    )
                )
            }
            messagesFlow.value = messagesList.toList()
            initialized = true
        }
    }

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        ensureInitialized()
        messagesFlow.collect { emit(it) }
    }

    fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
    ) {
        ensureInitialized()
        val clean = text.trim()
        val cleanMedia = mediaUri.trim()
        require((clean.isNotEmpty() && clean.length <= 4000) || cleanMedia.isNotEmpty())
        val msgId = UUID.randomUUID().toString()
        val resolvedType = if (cleanMedia.isNotEmpty()) {
            if (mediaType.equals("video", ignoreCase = true)) "video" else "image"
        } else ""

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
