package com.example.data.repository

import android.content.Context
import com.example.data.model.PublicationStats
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioGuardadosYLikes(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones
) {
    private val savedPrefs by lazy {
        appContext.getSharedPreferences("elegan_saved_cache", Context.MODE_PRIVATE)
    }
    private val likesPrefs by lazy {
        appContext.getSharedPreferences("elegan_likes_cache", Context.MODE_PRIVATE)
    }

    private val savedLoadMutex = Mutex()
    private val likesLoadMutex = Mutex()

    @Volatile
    private var loadedSavedKey: String? = null
    @Volatile
    private var loadedLikesKey: String? = null

    private val localSavedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val localSavedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val localLikedPublicationIds = ConcurrentHashMap.newKeySet<String>()
    private val localLikedFlow = MutableStateFlow<Set<String>>(emptySet())

    private fun savedKey(userKey: String = userCacheKey(appContext)): String = "saved_ids_$userKey"
    private fun likesKey(userKey: String = userCacheKey(appContext)): String = "liked_ids_$userKey"

    private suspend fun ensureSavedLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedSavedKey == currentKey) return
        savedLoadMutex.withLock {
            if (loadedSavedKey == currentKey) return
            val loadedIds = withContext(Dispatchers.IO) { loadPersistedIdSet(savedPrefs, savedKey(currentKey)) }
            localSavedProductIds.clear()
            localSavedProductIds.addAll(loadedIds)
            localSavedFlow.value = localSavedProductIds.toSet()
            loadedSavedKey = currentKey
        }
    }

    private suspend fun ensureLikesLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedLikesKey == currentKey) return
        likesLoadMutex.withLock {
            if (loadedLikesKey == currentKey) return
            val loadedIds = withContext(Dispatchers.IO) { loadPersistedIdSet(likesPrefs, likesKey(currentKey)) }
            localLikedPublicationIds.clear()
            localLikedPublicationIds.addAll(loadedIds)
            localLikedFlow.value = localLikedPublicationIds.toSet()
            loadedLikesKey = currentKey
        }
    }

    suspend fun ensureStatsLoaded() {
        statsRepo.ensureLoaded(
            savedProductIds = { localSavedProductIds },
            likedPublicationIds = { localLikedPublicationIds }
        )
    }

    fun observeSavedProductIds(): Flow<Set<String>> = flow {
        ensureSavedLoaded()
        ensureStatsLoaded()
        localSavedFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun observeLikedPublicationIds(): Flow<Set<String>> = flow {
        ensureLikesLoaded()
        ensureStatsLoaded()
        localLikedFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = flow {
        ensureSavedLoaded()
        ensureLikesLoaded()
        ensureStatsLoaded()
        statsRepo.statsFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank())
        ensureSavedLoaded()
        ensureStatsLoaded()

        val wasSaved = productId in localSavedProductIds
        if (isSaved) localSavedProductIds.add(productId) else localSavedProductIds.remove(productId)
        val snapshot = localSavedProductIds.toSet()
        localSavedFlow.value = snapshot
        withContext(Dispatchers.IO) {
            savePersistedIdSet(savedPrefs, savedKey(), snapshot)
        }

        if (wasSaved != isSaved) {
            statsRepo.adjustSaves(productId, if (isSaved) 1L else -1L)
        }
    }

    suspend fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank())
        ensureLikesLoaded()
        ensureStatsLoaded()

        val targetLiked = productId !in localLikedPublicationIds
        if (targetLiked) localLikedPublicationIds.add(productId) else localLikedPublicationIds.remove(productId)
        val snapshot = localLikedPublicationIds.toSet()
        localLikedFlow.value = snapshot
        withContext(Dispatchers.IO) {
            savePersistedIdSet(likesPrefs, likesKey(), snapshot)
        }

        statsRepo.adjustLikes(productId, if (targetLiked) 1L else -1L)
        return targetLiked
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        ensureStatsLoaded()
        statsRepo.recordShare(productId)
    }

    fun clearUserData(userKey: String) {
        savedPrefs.edit().remove(savedKey(userKey)).commit()
        likesPrefs.edit().remove(likesKey(userKey)).commit()
        localSavedProductIds.clear()
        localSavedFlow.value = emptySet()
        localLikedPublicationIds.clear()
        localLikedFlow.value = emptySet()
        loadedSavedKey = null
        loadedLikesKey = null
        statsRepo.clearUserData(userKey)
    }
}
