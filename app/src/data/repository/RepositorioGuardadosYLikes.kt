package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.PublicationStats
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class RepositorioGuardadosYLikes(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones
) {
    private val savedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val savedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likedPublicationIds = ConcurrentHashMap.newKeySet<String>()
    private val likedFlow = MutableStateFlow<Set<String>>(emptySet())

    private fun activeBackendToken(): String? {
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    fun observeSavedProductIds(): Flow<Set<String>> = savedFlow.asStateFlow()

    fun observeLikedPublicationIds(): Flow<Set<String>> = likedFlow.asStateFlow()

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = statsRepo.statsFlow

    suspend fun refreshSavedProducts(): Set<String> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext savedProductIds.toSet()
        try {
            val response = ApiAutenticacionMovil.service.getSaved("Bearer $token")
            val remoteIds = extractSavedProductIdsJson(response)
            savedProductIds.clear()
            savedProductIds.addAll(remoteIds)
            savedFlow.value = savedProductIds.toSet()
            savedFlow.value
        } catch (e: Exception) {
            Log.w("EleganSaved", "No se pudieron obtener los productos guardados desde el backend.", e)
            savedProductIds.toSet()
        }
    }

    suspend fun setProductSaved(productId: String, isSaved: Boolean) = withContext(Dispatchers.IO) {
        require(productId.isNotBlank())
        val wasSaved = productId in savedProductIds
        if (isSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
        savedFlow.value = savedProductIds.toSet()

        if (wasSaved != isSaved) {
            statsRepo.adjustSaves(productId, if (isSaved) 1L else -1L)
        }

        val token = activeBackendToken() ?: return@withContext
        try {
            val response = ApiAutenticacionMovil.service.setSaved(
                authorization = "Bearer $token",
                productId = productId,
                request = mapOf("saved" to isSaved, "isSaved" to isSaved)
            )
            val remoteIds = extractSavedProductIdsJson(response)
            if (remoteIds.isNotEmpty() || response.containsKey("items") || response.containsKey("productIds")) {
                savedProductIds.clear()
                savedProductIds.addAll(remoteIds)
                savedFlow.value = savedProductIds.toSet()
            }
        } catch (e: Exception) {
            Log.w("EleganSaved", "No se pudo sincronizar producto guardado $productId con el backend.", e)
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

internal fun extractSavedProductIdsJson(response: Map<String, Any?>): Set<String> {
    val directIds = (response["productIds"] as? List<*>)
        ?: (response["product_ids"] as? List<*>)
        ?: (response["savedIds"] as? List<*>)
        ?: (response["saved_ids"] as? List<*>)
    if (directIds != null) {
        return directIds.mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotBlank() } }.toSet()
    }

    val rawList = (response["items"] as? List<*>)
        ?: (response["saved"] as? List<*>)
        ?: (response["products"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptySet()

    return rawList.mapNotNull { entry ->
        when (entry) {
            is String -> entry.trim().takeIf { it.isNotBlank() }
            is Map<*, *> -> listOf("productId", "product_id", "id", "_id")
                .firstNotNullOfOrNull { key -> (entry[key] as? String)?.trim()?.takeIf { it.isNotBlank() } }
            else -> null
        }
    }.toSet()
}
