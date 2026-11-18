package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioGuardadosYLikes(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones,
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val tokenProvider: (() -> String?)? = null
) {
    private val savedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val savedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likedPublicationIds = ConcurrentHashMap.newKeySet<String>()
    private val likedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likeMutexes = ConcurrentHashMap<String, Mutex>()
    private val saveMutexes = ConcurrentHashMap<String, Mutex>()
    private val shareMutexes = ConcurrentHashMap<String, Mutex>()

    @Volatile
    private var likesLoadedToken: String? = null

    private fun activeBackendToken(): String? {
        if (tokenProvider != null) {
            return tokenProvider.invoke()?.takeIf {
                it.isNotBlank() && !it.startsWith("local_session_")
            }
        }
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    private fun isRobolectricLocalMode(): Boolean =
        tokenProvider == null && "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    fun observeSavedProductIds(): Flow<Set<String>> = savedFlow.asStateFlow()

    fun observeLikedPublicationIds(): Flow<Set<String>> = likedFlow.asStateFlow()

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = statsRepo.statsFlow

    fun syncCountsFromProducts(products: List<ProductJson>) {
        statsRepo.syncCountsFromProducts(products)
    }

    suspend fun refreshSavedProducts(): Set<String> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext savedProductIds.toSet()
        try {
            val response = apiProvider().getSaved("Bearer $token")
            val remoteIds = extractSavedProductIdsJson(response)
            savedProductIds.clear()
            savedProductIds.addAll(remoteIds)
            savedFlow.value = savedProductIds.toSet()
            applyScalarCountsMap(response["counts"] as? Map<*, *>) { id, count ->
                statsRepo.updateSavesCount(id, count)
            }
            applyScalarCountsMap(response["savesCounts"] as? Map<*, *>) { id, count ->
                statsRepo.updateSavesCount(id, count)
            }
            savedFlow.value
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganSaved", "No se pudieron obtener los productos guardados desde el backend.", e)
            savedProductIds.toSet()
        }
    }

    suspend fun setProductSaved(productId: String, isSaved: Boolean): Boolean = withContext(Dispatchers.IO) {
        require(productId.isNotBlank())
        val token = activeBackendToken()
        if (token == null) {
            if (isRobolectricLocalMode()) {
                val wasSaved = productId in savedProductIds
                if (isSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
                savedFlow.value = savedProductIds.toSet()
                if (wasSaved != isSaved) {
                    statsRepo.adjustSaves(productId, if (isSaved) 1L else -1L)
                }
                return@withContext isSaved
            }
            throw IllegalStateException("Debes iniciar sesión para guardar publicaciones.")
        }

        saveMutexes.getOrPut(productId) { Mutex() }.withLock {
            val wasSaved = productId in savedProductIds
            val previousSavesCount = statsRepo.statsFlow.value[productId]?.savesCount

            if (isSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
            savedFlow.value = savedProductIds.toSet()
            if (wasSaved != isSaved) {
                statsRepo.adjustSaves(productId, if (isSaved) 1L else -1L)
            }

            try {
                val response = apiProvider().setSaved(
                    authorization = "Bearer $token",
                    productId = productId,
                    request = mapOf("saved" to isSaved, "isSaved" to isSaved)
                )
                val remoteIds = extractSavedProductIdsJson(response)
                val confirmedSaved = if (
                    remoteIds.isNotEmpty() ||
                    response.containsKey("items") ||
                    response.containsKey("productIds") ||
                    response.containsKey("product_ids") ||
                    response.containsKey("ids")
                ) {
                    savedProductIds.clear()
                    savedProductIds.addAll(remoteIds)
                    savedFlow.value = savedProductIds.toSet()
                    productId in savedProductIds
                } else {
                    val serverSaved = (response["saved"] as? Boolean)
                        ?: (response["isSaved"] as? Boolean)
                        ?: (response["is_saved"] as? Boolean)
                        ?: isSaved
                    if (serverSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
                    savedFlow.value = savedProductIds.toSet()
                    if (serverSaved != isSaved && wasSaved == serverSaved) {
                        if (previousSavesCount != null) {
                            statsRepo.updateSavesCount(productId, previousSavesCount)
                        }
                    }
                    serverSaved
                }
                val savesCount = extractNumericCount(response, "savesCount", "saves_count", "count")
                if (savesCount != null) {
                    statsRepo.updateSavesCount(productId, savesCount)
                }
                confirmedSaved
            } catch (e: Exception) {
                if (wasSaved) savedProductIds.add(productId) else savedProductIds.remove(productId)
                savedFlow.value = savedProductIds.toSet()
                if (previousSavesCount != null) {
                    statsRepo.updateSavesCount(productId, previousSavesCount)
                } else if (wasSaved != isSaved) {
                    statsRepo.adjustSaves(productId, if (wasSaved) 1L else -1L)
                }
                Log.w("EleganSaved", "No se pudo sincronizar producto guardado $productId con el backend.", e)
                throw e
            }
        }
    }

    suspend fun refreshPublicationLikes(): Set<String> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext emptySet()
        try {
            val response = apiProvider().getLikes("Bearer $token")
            val remoteIds = extractLikeIdsJson(response)
            likedPublicationIds.clear()
            likedPublicationIds.addAll(remoteIds)
            likedFlow.value = likedPublicationIds.toSet()
            likesLoadedToken = token
            likedFlow.value
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganLikes", "No se pudieron cargar los likes desde el servidor.", e)
            likedPublicationIds.toSet()
        }
    }

    private suspend fun ensureLikesLoaded(token: String) {
        if (likesLoadedToken == token) return
        refreshPublicationLikes()
        if (likesLoadedToken != token) {
            throw IllegalStateException("No se pudieron sincronizar los likes con el servidor.")
        }
    }

    suspend fun refreshPublicationLikeCounts(productIds: Collection<String>) = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            // El backend limita la consulta a 100 IDs por petición.
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getLikeCounts("Bearer $token", batch)
                val counts = response["counts"] as? Map<*, *>
                if (counts != null) {
                    counts.forEach countLoop@{ (rawId, rawValue) ->
                        val id = rawId as? String ?: return@countLoop
                        when (rawValue) {
                            is Number, is String -> {
                                parseNumericCount(rawValue)?.let { statsRepo.updateLikesCount(id, it) }
                            }
                            is Map<*, *> -> {
                                extractNumericCount(rawValue, "likesCount", "likes_count", "likes")
                                    ?.let { statsRepo.updateLikesCount(id, it) }
                                extractNumericCount(rawValue, "savesCount", "saves_count", "saves")
                                    ?.let { statsRepo.updateSavesCount(id, it) }
                                extractNumericCount(rawValue, "sharesCount", "shares_count", "shares")
                                    ?.let { statsRepo.updateSharesCount(id, it) }
                                extractNumericCount(rawValue, "commentsCount", "comments_count", "comments")
                                    ?.let { statsRepo.updateCommentsCount(id, it) }
                            }
                        }
                    }
                }
                applyScalarCountsMap(response["savesCounts"] as? Map<*, *>) { id, count ->
                    statsRepo.updateSavesCount(id, count)
                }
                applyScalarCountsMap(response["sharesCounts"] as? Map<*, *>) { id, count ->
                    statsRepo.updateSharesCount(id, count)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganLikes", "No se pudieron actualizar los conteos de likes.", e)
        }
    }

    suspend fun refreshPublicationSaveCounts(productIds: Collection<String>) = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getSavedCounts("Bearer $token", batch)
                val counts = (response["counts"] as? Map<*, *>)
                    ?: (response["savesCounts"] as? Map<*, *>)
                    ?: return@batchLoop
                applyScalarCountsMap(counts) { id, count ->
                    statsRepo.updateSavesCount(id, count)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganSaved", "No se pudieron actualizar los conteos globales de guardados.", e)
        }
    }

    suspend fun refreshPublicationShareCounts(productIds: Collection<String>) = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getShareCounts("Bearer $token", batch)
                val counts = (response["counts"] as? Map<*, *>)
                    ?: (response["sharesCounts"] as? Map<*, *>)
                    ?: return@batchLoop
                applyScalarCountsMap(counts) { id, count ->
                    statsRepo.updateSharesCount(id, count)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganShares", "No se pudieron actualizar los conteos globales de compartidos.", e)
        }
    }

    suspend fun refreshPublicationCounts(productIds: Collection<String>) {
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return
        refreshPublicationLikeCounts(ids)
        refreshPublicationSaveCounts(ids)
        refreshPublicationShareCounts(ids)
    }

    suspend fun togglePublicationLike(productId: String): Boolean = withContext(Dispatchers.IO) {
        require(productId.isNotBlank())
        val token = activeBackendToken()
        if (token == null) {
            // Modo de pruebas Robolectric: permite probar el estado visual sin red ni sesión.
            if (isRobolectricLocalMode()) {
                val targetLiked = if (likedPublicationIds.add(productId)) true else {
                    likedPublicationIds.remove(productId)
                    false
                }
                likedFlow.value = likedPublicationIds.toSet()
                statsRepo.adjustLikes(productId, if (targetLiked) 1L else -1L)
                return@withContext targetLiked
            }
            throw IllegalStateException("Debes iniciar sesión para dar like.")
        }
        ensureLikesLoaded(token)

        likeMutexes.getOrPut(productId) { Mutex() }.withLock {
            val wasLiked = productId in likedPublicationIds
            val targetLiked = !wasLiked

            // Actualización optimista: el corazón cambia inmediatamente en la interfaz.
            if (targetLiked) likedPublicationIds.add(productId) else likedPublicationIds.remove(productId)
            likedFlow.value = likedPublicationIds.toSet()
            statsRepo.adjustLikes(productId, if (targetLiked) 1L else -1L)

            try {
                val response = apiProvider().setLike(
                    authorization = "Bearer $token",
                    productId = productId,
                    request = mapOf("liked" to targetLiked)
                )
                val confirmedLiked = response["liked"] as? Boolean ?: targetLiked
                val likesCount = extractNumericCount(response, "likesCount", "likes_count", "count")
                if (confirmedLiked != targetLiked) {
                    if (confirmedLiked) likedPublicationIds.add(productId)
                    else likedPublicationIds.remove(productId)
                    likedFlow.value = likedPublicationIds.toSet()
                    statsRepo.adjustLikes(productId, if (confirmedLiked) 1L else -1L)
                }
                if (likesCount != null) statsRepo.updateLikesCount(productId, likesCount)
                confirmedLiked
            } catch (e: Exception) {
                // Revierte el corazón y el contador si el servidor no confirma el cambio.
                if (wasLiked) likedPublicationIds.add(productId) else likedPublicationIds.remove(productId)
                likedFlow.value = likedPublicationIds.toSet()
                statsRepo.adjustLikes(productId, if (wasLiked) 1L else -1L)
                throw e
            }
        }
    }

    suspend fun recordPublicationShare(productId: String): Long = withContext(Dispatchers.IO) {
        require(productId.isNotBlank())
        val token = activeBackendToken()
        if (token == null) {
            if (isRobolectricLocalMode()) {
                statsRepo.recordShare(productId)
                return@withContext statsRepo.statsFlow.value[productId]?.sharesCount ?: 1L
            }
            // Compartir contenido no debe depender de que exista una sesión iniciada.
            return@withContext statsRepo.statsFlow.value[productId]?.sharesCount ?: 0L
        }

        shareMutexes.getOrPut(productId) { Mutex() }.withLock {
            // Nunca incrementamos localmente antes de recibir confirmación del backend.
            // Si la ruta opcional todavía no está desplegada o falla la red, el contador
            // permanece intacto y la acción de compartir puede continuar.
            val previousSharesCount = statsRepo.statsFlow.value[productId]?.sharesCount ?: 0L
            try {
                val response = apiProvider().recordShare(
                    authorization = "Bearer $token",
                    productId = productId,
                    request = mapOf("productId" to productId)
                )
                val remoteCount = extractNumericCount(response, "sharesCount", "shares_count", "count")
                if (remoteCount != null) {
                    statsRepo.updateSharesCount(productId, remoteCount)
                    remoteCount
                } else {
                    previousSharesCount
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("EleganShares", "No se pudo registrar el compartido de $productId; se conserva el conteo confirmado.", e)
                previousSharesCount
            }
        }
    }

    fun clearUserData() {
        savedProductIds.clear()
        savedFlow.value = emptySet()
        likedPublicationIds.clear()
        likedFlow.value = emptySet()
        likesLoadedToken = null
        likeMutexes.clear()
        saveMutexes.clear()
        shareMutexes.clear()
        statsRepo.clearUserData()
    }

    private fun normalizeProductIds(productIds: Collection<String>): List<String> =
        productIds.asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .toList()

    private inline fun applyScalarCountsMap(
        counts: Map<*, *>?,
        onEntry: (id: String, count: Long) -> Unit
    ) {
        counts?.forEach { (rawId, rawCount) ->
            val id = rawId as? String ?: return@forEach
            val count = parseNumericCount(rawCount) ?: return@forEach
            onEntry(id, count)
        }
    }
}

private fun parseNumericCount(raw: Any?): Long? = when (raw) {
    is Number -> raw.toLong()
    is String -> raw.trim().toLongOrNull()
    else -> null
}

private fun extractNumericCount(source: Map<*, *>, vararg keys: String): Long? {
    for (key in keys) {
        val parsed = parseNumericCount(source[key])
        if (parsed != null) return parsed
    }
    return null
}

internal fun extractLikeIdsJson(response: Map<String, Any?>): Set<String> {
    val ids = response["ids"] as? List<*> ?: return emptySet()
    return ids.mapNotNull { (it as? String)?.trim()?.takeIf(String::isNotBlank) }.toSet()
}

internal fun extractSavedProductIdsJson(response: Map<String, Any?>): Set<String> {
    val directIds = (response["ids"] as? List<*>)
        ?: (response["productIds"] as? List<*>)
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
