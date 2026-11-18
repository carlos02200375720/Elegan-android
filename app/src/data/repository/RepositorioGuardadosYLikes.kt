package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.data.local.EleganDatabase
import com.example.data.local.PendingVisitorShareDao
import com.example.data.local.PendingVisitorShareEntity
import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioGuardadosYLikes(
    private val appContext: Context,
    private val statsRepo: RepositorioEstadisticasPublicaciones,
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val tokenProvider: (() -> String?)? = null,
    private val pendingShareDaoProvider: () -> PendingVisitorShareDao = {
        EleganDatabase.getInstance(appContext).pendingVisitorShareDao()
    }
) {
    private val savedProductIds = ConcurrentHashMap.newKeySet<String>()
    private val savedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likedPublicationIds = ConcurrentHashMap.newKeySet<String>()
    private val likedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val likeMutexes = ConcurrentHashMap<String, Mutex>()
    private val saveMutexes = ConcurrentHashMap<String, Mutex>()
    private val shareMutexes = ConcurrentHashMap<String, Mutex>()
    private val pendingShareDao: PendingVisitorShareDao by lazy { pendingShareDaoProvider() }
    private val pendingSharesFlushMutex = Mutex()
    private val visitorPrefs by lazy {
        appContext.applicationContext.getSharedPreferences("elegan_visitor_shares", Context.MODE_PRIVATE)
    }

    @Volatile
    private var likesLoadedToken: String? = null

    private fun getOrCreateVisitorId(): String {
        val existing = visitorPrefs.getString("visitor_id", null)?.takeIf { it.isNotBlank() }
        if (existing != null) return existing
        val created = UUID.randomUUID().toString()
        visitorPrefs.edit().putString("visitor_id", created).apply()
        return created
    }

    /**
     * Envía al backend los eventos pendientes almacenados en Room sin exigir token de sesión
     * y elimina únicamente cada evento confirmado por el servidor.
     */
    suspend fun flushPendingVisitorShares(): Int = withContext(Dispatchers.IO) {
        if (isRobolectricLocalMode()) return@withContext 0
        pendingSharesFlushMutex.withLock {
            val pending = pendingShareDao.getPendingShares()
            if (pending.isEmpty()) return@withContext 0
            val authHeader = activeBackendToken()?.let { "Bearer $it" }
            var flushedCount = 0
            for (entry in pending) {
                try {
                    val response = apiProvider().recordShare(
                        authorization = authHeader,
                        productId = entry.productId,
                        request = mapOf(
                            "eventId" to entry.eventId,
                            "productId" to entry.productId,
                            "visitorId" to entry.visitorId,
                            "isGuest" to true
                        )
                    )
                    if (response["success"] == false) {
                        Log.w(
                            "EleganShares",
                            "El backend no confirmó el compartido pendiente (${entry.eventId}, ${entry.productId}); se conserva en Room."
                        )
                        continue
                    }
                    val remoteCount = extractNumericCount(response, "sharesCount", "shares_count", "count")
                    if (remoteCount != null) {
                        statsRepo.updateSharesCount(entry.productId, remoteCount)
                    }
                    pendingShareDao.deleteByEventId(entry.eventId)
                    flushedCount++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(
                        "EleganShares",
                        "No se pudo enviar compartido pendiente (${entry.eventId}, ${entry.productId}); permanece guardado en Room.",
                        e
                    )
                    break
                }
            }
            flushedCount
        }
    }

    fun startPendingShareSync(scope: CoroutineScope) {
        if (isRobolectricLocalMode()) return
        scope.launch(Dispatchers.IO) {
            runCatching { flushPendingVisitorShares() }
        }
        val connectivityManager = runCatching {
            appContext.getSystemService(ConnectivityManager::class.java)
        }.getOrNull() ?: return

        val lastKnownOnline = AtomicBoolean(isCurrentlyConnected(connectivityManager))

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                lastKnownOnline.set(true)
                scope.launch(Dispatchers.IO) {
                    runCatching { flushPendingVisitorShares() }
                }
            }

            override fun onLost(network: Network) {
                lastKnownOnline.set(isCurrentlyConnected(connectivityManager))
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                if (hasInternet && lastKnownOnline.compareAndSet(false, true)) {
                    scope.launch(Dispatchers.IO) {
                        runCatching { flushPendingVisitorShares() }
                    }
                } else if (!hasInternet) {
                    lastKnownOnline.set(false)
                }
            }
        }

        runCatching {
            connectivityManager.registerDefaultNetworkCallback(callback)
            scope.coroutineContext[Job]?.invokeOnCompletion {
                runCatching { connectivityManager.unregisterNetworkCallback(callback) }
            }
        }
    }

    private fun isCurrentlyConnected(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

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
        flushPendingVisitorShares()
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
        if (isRobolectricLocalMode()) return@withContext
        val authHeader = activeBackendToken()?.let { "Bearer $it" }
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            // El backend limita la consulta a 100 IDs por petición.
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getLikeCounts(authHeader, batch)
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
        if (isRobolectricLocalMode()) return@withContext
        val authHeader = activeBackendToken()?.let { "Bearer $it" }
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getSavedCounts(authHeader, batch)
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
        if (isRobolectricLocalMode()) return@withContext
        flushPendingVisitorShares()
        val authHeader = activeBackendToken()?.let { "Bearer $it" }
        val ids = normalizeProductIds(productIds)
        if (ids.isEmpty()) return@withContext

        try {
            ids.chunked(100).forEach batchLoop@{ batch ->
                val response = apiProvider().getShareCounts(authHeader, batch)
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
        if (token == null && isRobolectricLocalMode()) {
            statsRepo.recordShare(productId)
            return@withContext statsRepo.statsFlow.value[productId]?.sharesCount ?: 1L
        }

        if (token == null) {
            val previousSharesCount = statsRepo.statsFlow.value[productId]?.sharesCount ?: 0L
            val eventId = UUID.randomUUID().toString()
            val visitorId = getOrCreateVisitorId()
            pendingShareDao.insertPendingShare(
                PendingVisitorShareEntity(
                    eventId = eventId,
                    productId = productId,
                    visitorId = visitorId
                )
            )
            flushPendingVisitorShares()
            return@withContext statsRepo.statsFlow.value[productId]?.sharesCount ?: previousSharesCount
        }

        shareMutexes.getOrPut(productId) { Mutex() }.withLock {
            // Nunca incrementamos localmente antes de recibir confirmación del backend.
            val previousSharesCount = statsRepo.statsFlow.value[productId]?.sharesCount ?: 0L
            val eventId = UUID.randomUUID().toString()
            try {
                val response = apiProvider().recordShare(
                    authorization = "Bearer $token",
                    productId = productId,
                    request = mapOf(
                        "eventId" to eventId,
                        "productId" to productId
                    )
                )
                if (response["success"] == false) {
                    return@withLock previousSharesCount
                }
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
