package com.example.data.repository

import android.content.Context
import com.example.data.model.PublicationComment
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

internal class RepositorioComentarios(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones
) {
    private val commentsCache = ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val commentsUpdated = MutableSharedFlow<String>(extraBufferCapacity = 32)

    private fun activeBackendToken(): String? {
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        refreshPublicationComments(productId)
        val list = commentsCache.computeIfAbsent(productId) { mutableListOf() }
        while (true) {
            emit(synchronized(list) { list.toList() })
            commentsUpdated.first { it == productId }
        }
    }

    suspend fun refreshPublicationComments(productId: String): List<PublicationComment> =
        withContext(Dispatchers.IO) {
            require(productId.isNotBlank())
            val token = activeBackendToken() ?: return@withContext cachedComments(productId)
            try {
                val response = ApiAutenticacionMovil.service.getPublicationComments(
                    authorization = "Bearer $token",
                    productId = productId
                )
                val remoteComments = extractPublicationComments(response)
                val list = commentsCache.computeIfAbsent(productId) { mutableListOf() }
                synchronized(list) {
                    list.clear()
                    list.addAll(remoteComments)
                }
                val count = (response["commentsCount"] as? Number)?.toLong()
                if (count != null) statsRepo.updateCommentsCount(productId, count)
                commentsUpdated.tryEmit(productId)
                remoteComments
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                MonitorErrores.registrarAdvertencia(
                    tag = "EleganComments",
                    mensaje = "No se pudieron cargar los comentarios desde el backend.",
                    error = error,
                    extras = mapOf("operation" to "refreshPublicationComments", "productId" to productId)
                )
                cachedComments(productId)
            }
        }

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) =
        withContext(Dispatchers.IO) {
            val clean = text.trim()
            require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)

            val token = activeBackendToken()
                ?: throw IllegalStateException("Debes iniciar sesión para comentar.")

            val response = ApiAutenticacionMovil.service.addPublicationComment(
                authorization = "Bearer $token",
                productId = productId,
                request = mapOf("text" to clean)
            )
            val rawItem = response["item"] as? Map<*, *>
                ?: throw IllegalStateException("El servidor no devolvió el comentario guardado.")
            val savedComment = rawItem.toPublicationComment()
                ?: throw IllegalStateException("La respuesta del comentario no es válida.")

            appendComment(savedComment)
            val count = (response["commentsCount"] as? Number)?.toLong()
            if (count != null) statsRepo.updateCommentsCount(productId, count)
            else statsRepo.incrementComments(productId)
        }

    suspend fun reportComment(
        commentId: String,
        reason: String = "Contenido inapropiado"
    ) = withContext(Dispatchers.IO) {
        val cleanCommentId = commentId.trim()
        require(cleanCommentId.isNotBlank()) { "El comentario no es válido." }
        val token = activeBackendToken()
            ?: throw IllegalStateException("Inicia sesión para reportar.")
        ApiAutenticacionMovil.service.reportComment(
            authorization = "Bearer $token",
            commentId = cleanCommentId,
            request = mapOf("reason" to reason.trim().ifBlank { "Contenido inapropiado" })
        )
    }

    suspend fun blockUser(userId: String) = withContext(Dispatchers.IO) {
        val cleanUserId = userId.trim()
        require(cleanUserId.isNotBlank()) { "El usuario no es válido." }
        val token = activeBackendToken()
            ?: throw IllegalStateException("Inicia sesión para bloquear.")
        ApiAutenticacionMovil.service.blockUser(
            authorization = "Bearer $token",
            userId = cleanUserId
        )
        commentsCache.forEach { (productId, list) ->
            val removed = synchronized(list) { list.removeAll { it.userId == cleanUserId } }
            if (removed) {
                commentsUpdated.tryEmit(productId)
            }
        }
    }

    private fun appendComment(comment: PublicationComment) {
        val list = commentsCache.computeIfAbsent(comment.productId) { mutableListOf() }
        synchronized(list) {
            list.removeAll { it.id == comment.id }
            list.add(comment)
        }
        commentsUpdated.tryEmit(comment.productId)
    }

    private fun cachedComments(productId: String): List<PublicationComment> {
        val list = commentsCache[productId] ?: return emptyList()
        return synchronized(list) { list.toList() }
    }
}

internal fun extractPublicationComments(response: Map<String, Any?>): List<PublicationComment> {
    val items = response["items"] as? List<*> ?: return emptyList()
    return items.mapNotNull { (it as? Map<*, *>)?.toPublicationComment() }
}

private fun Map<*, *>.toPublicationComment(): PublicationComment? {
    fun string(vararg keys: String): String {
        keys.forEach { key ->
            val value = this[key]?.toString()?.trim()
            if (!value.isNullOrBlank()) return value
        }
        return ""
    }

    val id = string("id", "_id")
    val productId = string("productId", "product_id")
    val text = string("text", "content")
    if (id.isBlank() || productId.isBlank() || text.isBlank()) return null

    return PublicationComment(
        id = id,
        productId = productId,
        userId = string("userId", "user_id"),
        displayName = string("displayName", "display_name").ifBlank { "Cliente" },
        text = text,
        createdAt = string("createdAt", "created_at").ifBlank { null }
    )
}
