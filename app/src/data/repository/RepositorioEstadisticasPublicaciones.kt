package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.PublicationStats
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class RepositorioEstadisticasPublicaciones(private val appContext: Context) {

    private val statsPrefs by lazy {
        appContext.getSharedPreferences("elegan_stats_cache", Context.MODE_PRIVATE)
    }
    private val statsLoadMutex = Mutex()

    @Volatile
    private var loadedStatsKey: String? = null
    private val localStatsMap = ConcurrentHashMap<String, PublicationStats>()
    private val localStatsFlow = MutableStateFlow<Map<String, PublicationStats>>(emptyMap())

    val statsFlow: StateFlow<Map<String, PublicationStats>> get() = localStatsFlow

    private fun cacheKey(userKey: String = userCacheKey(appContext)): String = "publication_stats_$userKey"

    suspend fun ensureLoaded(
        savedProductIds: () -> Set<String>,
        likedPublicationIds: () -> Set<String>
    ) {
        val currentKey = userCacheKey(appContext)
        if (loadedStatsKey == currentKey) return
        statsLoadMutex.withLock {
            if (loadedStatsKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedStats(currentKey) }
            localStatsMap.clear()
            localStatsMap.putAll(loaded)
            savedProductIds().forEach { productId ->
                val current = localStatsMap[productId] ?: PublicationStats(id = productId)
                if (current.savesCount < 1L) {
                    localStatsMap[productId] = current.copy(savesCount = 1L)
                }
            }
            likedPublicationIds().forEach { productId ->
                val current = localStatsMap[productId] ?: PublicationStats(id = productId)
                if (current.likesCount < 1L) {
                    localStatsMap[productId] = current.copy(likesCount = 1L)
                }
            }
            localStatsFlow.value = localStatsMap.toMap()
            loadedStatsKey = currentKey
        }
    }

    private fun loadPersistedStats(userKey: String): Map<String, PublicationStats> {
        val raw = statsPrefs.getString(cacheKey(userKey), null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val result = mutableMapOf<String, PublicationStats>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                val item = obj.optJSONObject(id) ?: continue
                result[id] = PublicationStats(
                    id = id,
                    likesCount = item.optLong("likesCount", 0L).coerceAtLeast(0L),
                    commentsCount = item.optLong("commentsCount", 0L).coerceAtLeast(0L),
                    sharesCount = item.optLong("sharesCount", 0L).coerceAtLeast(0L),
                    savesCount = item.optLong("savesCount", 0L).coerceAtLeast(0L)
                )
            }
            result
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer las estadísticas locales: ${e.message}")
            emptyMap()
        }
    }

    private suspend fun savePersistedStats(
        snapshot: Map<String, PublicationStats>,
        userKey: String = userCacheKey(appContext)
    ) {
        withContext(Dispatchers.IO) {
            try {
                val root = JSONObject()
                snapshot.forEach { (id, stat) ->
                    root.put(
                        id,
                        JSONObject().apply {
                            put("likesCount", stat.likesCount)
                            put("commentsCount", stat.commentsCount)
                            put("sharesCount", stat.sharesCount)
                            put("savesCount", stat.savesCount)
                        }
                    )
                }
                statsPrefs.edit().putString(cacheKey(userKey), root.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar las estadísticas localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishState() {
        val snapshot = localStatsMap.toMap()
        localStatsFlow.value = snapshot
        savePersistedStats(snapshot)
    }

    suspend fun adjustSaves(productId: String, delta: Long) {
        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        val updatedSaves = (current.savesCount + delta).coerceAtLeast(0L)
        localStatsMap[productId] = current.copy(savesCount = updatedSaves)
        publishState()
    }

    suspend fun adjustLikes(productId: String, delta: Long) {
        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        val updatedLikes = (current.likesCount + delta).coerceAtLeast(0L)
        localStatsMap[productId] = current.copy(likesCount = updatedLikes)
        publishState()
    }

    suspend fun incrementComments(productId: String) {
        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = current.copy(commentsCount = current.commentsCount + 1L)
        publishState()
    }

    suspend fun recordShare(productId: String) {
        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = current.copy(sharesCount = current.sharesCount + 1L)
        publishState()
    }

    fun clearUserData(userKey: String) {
        statsPrefs.edit().remove(cacheKey(userKey)).commit()
        localStatsMap.clear()
        localStatsFlow.value = emptyMap()
        loadedStatsKey = null
    }
}
