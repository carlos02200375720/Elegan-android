package com.example.data.repository

import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.model.productCommentCount
import com.example.data.model.productId
import com.example.data.model.productLikesCount
import com.example.data.model.productSavesCount
import com.example.data.model.productSharesCount
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

    fun updateSavesCount(productId: String, count: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(savesCount = count.coerceAtLeast(0L))
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
        adjustShares(productId, 1L)
    }

    fun adjustShares(productId: String, delta: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        val updatedShares = (current.sharesCount + delta).coerceAtLeast(0L)
        statsMap[productId] = current.copy(sharesCount = updatedShares)
        publishState()
    }

    fun updateSharesCount(productId: String, count: Long) {
        val current = statsMap[productId] ?: PublicationStats(id = productId)
        statsMap[productId] = current.copy(sharesCount = count.coerceAtLeast(0L))
        publishState()
    }

    fun syncCountsFromProducts(products: List<ProductJson>) {
        var changed = false
        products.forEach { product ->
            val id = product.productId()
            if (id.isBlank()) return@forEach
            val current = statsMap[id] ?: PublicationStats(id = id)
            val hasLikes = product.containsKey("likesCount") || product.containsKey("likes_count")
            val hasComments = product.containsKey("commentsCount") || product.containsKey("comments_count")
            val hasSaves = product.containsKey("savesCount") || product.containsKey("saves_count")
            val hasShares = product.containsKey("sharesCount") || product.containsKey("shares_count")
            if (!hasLikes && !hasComments && !hasSaves && !hasShares) return@forEach

            val updated = current.copy(
                likesCount = if (hasLikes) product.productLikesCount() else current.likesCount,
                commentsCount = if (hasComments) product.productCommentCount() else current.commentsCount,
                savesCount = if (hasSaves) product.productSavesCount() else current.savesCount,
                sharesCount = if (hasShares) product.productSharesCount() else current.sharesCount
            )
            if (updated != statsMap[id]) {
                statsMap[id] = updated
                changed = true
            }
        }
        if (changed) publishState()
    }

    fun clearUserData() {
        statsMap.clear()
        statsMutableFlow.value = emptyMap()
    }
}
