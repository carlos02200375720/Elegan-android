package com.example.data.repository

import android.content.Context
import com.example.data.model.PublicationComment
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

internal class RepositorioComentarios(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones
) {
    private val commentsCache = ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val commentsUpdated = MutableSharedFlow<String>(extraBufferCapacity = 32)

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        val list = commentsCache.computeIfAbsent(productId) { mutableListOf() }
        while (true) {
            emit(synchronized(list) { list.toList() })
            commentsUpdated.first { it == productId }
        }
    }

    fun addPublicationComment(productId: String, displayName: String, text: String) {
        val clean = text.trim()
        require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)

        val userId = optionalUserId(appContext) ?: "cliente_${UUID.randomUUID().toString().take(8)}"
        val finalName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val newComment = PublicationComment(
            id = "comment_${UUID.randomUUID()}",
            productId = productId,
            userId = userId,
            displayName = finalName,
            text = clean,
            createdAt = nowIsoUtc()
        )

        val list = commentsCache.computeIfAbsent(productId) { mutableListOf() }
        synchronized(list) {
            list.add(newComment)
        }
        commentsUpdated.tryEmit(productId)
        statsRepo.incrementComments(productId)
    }
}
