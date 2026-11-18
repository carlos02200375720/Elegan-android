package com.example.data.repository

import com.example.data.model.PublicationStats
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RepositorioEstadisticasPublicaciones {

    private val statsMap = ConcurrentHashMap<String, PublicationStats>()
    private val statsMutableFlow = MutableStateFlow<Map<String, PublicationStats>>(emptyMap())

    val statsFlow: StateFlow<Map<String, PublicationStats>> = statsMutableFlow.asStateFlow()

    private fun publishState() {
        statsMutableFlow.value = statsMap.toMap()
    }

    fun adjustSaves(productId: String, delta: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedSaves = (current.savesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(savesCount = updatedSaves)
        publishState()
    }

    fun adjustLikes(productId: String, delta: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedLikes = (current.likesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(likesCount = updatedLikes)
        publishState()
    }

    fun updateLikesCount(productId: String, count: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(likesCount = count.coerceAtLeast(0L))
        publishState()
    }

    fun incrementComments(productId: String) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(commentsCount = current.commentsCount + 1L)
        publishState()
    }

    fun updateCommentsCount(productId: String, count: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(commentsCount = count.coerceAtLeast(0L))
        publishState()
    }

    fun recordShare(productId: String) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(sharesCount = current.sharesCount + 1L)
        publishState()
    }

    fun clearUserData() {
        statsMap.clear()
        statsMutableFlow.value = emptyMap()
    }
}
