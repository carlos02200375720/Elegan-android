package com.example.data.repository

import com.example.data.model.PublicationStats
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RepositorioGuardadosYLikes(
    private val statsRepo: RepositorioEstadisticasPublicaciones
) {
    private val savedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val savedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likedPublicationIds = ConcurrentHashMap.newKeySet<String>()
    private val likedFlow = MutableStateFlow<Set<String>>(emptySet())

    fun observeSavedProductIds(): Flow<Set<String>> = savedFlow.asStateFlow()

    fun observeLikedPublicationIds(): Flow<Set<String>> = likedFlow.asStateFlow()

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = statsRepo.statsFlow

    fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank())
        val wasSaved = productId in savedProductIds
        if (isSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
        savedFlow.value = savedProductIds.toSet()

        if (wasSaved != isSaved) {
            statsRepo.adjustSaves(productId, if (isSaved) 1L else -1L)
        }
    }

    fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank())
        val targetLiked = productId !in likedPublicationIds
        if (targetLiked) likedPublicationIds.add(productId) else likedPublicationIds.remove(productId)
        likedFlow.value = likedPublicationIds.toSet()

        statsRepo.adjustLikes(productId, if (targetLiked) 1L else -1L)
        return targetLiked
    }

    fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        statsRepo.recordShare(productId)
    }

    fun clearUserData() {
        savedProductIds.clear()
        savedFlow.value = emptySet()
        likedPublicationIds.clear()
        likedFlow.value = emptySet()
        statsRepo.clearUserData()
    }
}
