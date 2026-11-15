package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.PublicationComment
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class RepositorioComentarios(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones,
    private val ensureStatsLoaded: suspend () -> Unit
) {
    private val commentsPrefs by lazy {
        appContext.getSharedPreferences("elegan_comments_cache", Context.MODE_PRIVATE)
    }
    private val localCommentsCache = ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val localCommentsUpdated = MutableSharedFlow<String>(extraBufferCapacity = 32)

    private fun cacheKey(productId: String): String = "comments_$productId"

    private fun loadPersistedComments(productId: String): MutableList<PublicationComment> {
        val raw = commentsPrefs.getString(cacheKey(productId), null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            mutableListOf<PublicationComment>().apply {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        PublicationComment(
                            id = obj.optString("id", ""),
                            productId = obj.optString("productId", productId),
                            userId = obj.optString("userId", ""),
                            displayName = obj.optString("displayName", "Cliente"),
                            text = obj.optString("text", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer comentarios locales: ${e.message}")
            mutableListOf()
        }
    }

    private suspend fun savePersistedComments(productId: String, snapshot: List<PublicationComment>) {
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                snapshot.forEach { comment ->
                    array.put(
                        JSONObject().apply {
                            put("id", comment.id)
                            put("productId", comment.productId)
                            put("userId", comment.userId)
                            put("displayName", comment.displayName)
                            put("text", comment.text)
                            put("createdAt", comment.createdAt.orEmpty())
                        }
                    )
                }
                commentsPrefs.edit().putString(cacheKey(productId), array.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar comentarios locales: ${e.message}")
            }
        }
    }

    fun observePublicationComments(productId: String): Flow<List<PublicationComment>> = flow {
        require(productId.isNotBlank())
        val localList = localCommentsCache.computeIfAbsent(productId) {
            loadPersistedComments(productId)
        }
        while (true) {
            emit(synchronized(localList) { localList.toList() })
            localCommentsUpdated.first { it == productId }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun addPublicationComment(productId: String, displayName: String, text: String) {
        val clean = text.trim()
        require(productId.isNotBlank() && clean.isNotEmpty() && clean.length <= 1000)

        val userId = optionalUserId(appContext) ?: "cliente_${UUID.randomUUID().toString().take(8)}"
        val finalName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val newComment = PublicationComment(
            id = "local_${UUID.randomUUID()}",
            productId = productId,
            userId = userId,
            displayName = finalName,
            text = clean,
            createdAt = nowIsoUtc()
        )

        val localList = localCommentsCache.computeIfAbsent(productId) {
            loadPersistedComments(productId)
        }
        val snapshot = synchronized(localList) {
            localList.add(newComment)
            localList.toList()
        }
        savePersistedComments(productId, snapshot)
        localCommentsUpdated.tryEmit(productId)

        ensureStatsLoaded()
        statsRepo.incrementComments(productId)
    }
}
