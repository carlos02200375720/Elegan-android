package com.example.data.repository

import com.example.data.model.Product
import com.example.data.model.PublicationStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RepositorioEstadisticasPublicaciones {

    private val lock = Any()
    private val statsMap = HashMap<String, PublicationStats>()
    private val statsMutableFlow = MutableStateFlow<Map<String, PublicationStats>>(emptyMap())

    val statsFlow: StateFlow<Map<String, PublicationStats>> = statsMutableFlow.asStateFlow()

    private fun publishStateLocked() {
        statsMutableFlow.value = statsMap.toMap()
    }

    fun adjustSaves(productId: String, delta: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedSaves = (current.savesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(savesCount = updatedSaves)
        publishStateLocked()
    }

    fun updateSavesCount(productId: String, count: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(savesCount = count.coerceAtLeast(0L))
        publishStateLocked()
    }

    fun adjustLikes(productId: String, delta: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedLikes = (current.likesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(likesCount = updatedLikes)
        publishStateLocked()
    }

    fun updateLikesCount(productId: String, count: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(likesCount = count.coerceAtLeast(0L))
        publishStateLocked()
    }

    fun incrementComments(productId: String) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(commentsCount = current.commentsCount + 1L)
        publishStateLocked()
    }

    fun updateCommentsCount(productId: String, count: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(commentsCount = count.coerceAtLeast(0L))
        publishStateLocked()
    }

    fun recordShare(productId: String) {
        adjustShares(productId, 1L)
    }

    fun adjustShares(productId: String, delta: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedShares = (current.sharesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(sharesCount = updatedShares)
        publishStateLocked()
    }

    fun updateSharesCount(productId: String, count: Long) = synchronized(lock) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(sharesCount = count.coerceAtLeast(0L))
        publishStateLocked()
    }

    fun syncCountsFromProducts(products: List<Product>) = synchronized(lock) {
        var changed = false
        products.forEach { product ->
            val id = product.id
            if (id.isBlank()) return@forEach
            val likes = product.likesCount
            val comments = product.commentsCount
            val saves = product.savesCount
            val shares = product.sharesCount
            if (likes == null && comments == null && saves == null && shares == null) return@forEach

            val current = statsMap[id] ?: PublicationStats(id = id)
            val updated = current.copy(
                likesCount = likes ?: current.likesCount,
                commentsCount = comments ?: current.commentsCount,
                savesCount = saves ?: current.savesCount,
                sharesCount = shares ?: current.sharesCount
            )
            if (updated != statsMap[id]) {
                statsMap[id] = updated
                changed = true
            }
        }
        if (changed) publishStateLocked()
    }

    fun clearUserData() = synchronized(lock) {
        statsMap.clear()
        statsMutableFlow.value = emptyMap()
    }
}
