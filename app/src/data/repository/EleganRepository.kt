package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.Announcement
import com.example.data.model.CartItem
import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.remote.MobileSession
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class EleganRepository(context: Context) {
    private val appContext = context.applicationContext

    private fun optionalUserId(): String? = MobileSession.userId(appContext)

    private val cartPrefs by lazy {
        appContext.getSharedPreferences("elegan_cart_cache", Context.MODE_PRIVATE)
    }
    private val savedPrefs by lazy {
        appContext.getSharedPreferences("elegan_saved_cache", Context.MODE_PRIVATE)
    }
    private val statsPrefs by lazy {
        appContext.getSharedPreferences("elegan_stats_cache", Context.MODE_PRIVATE)
    }
    private val likesPrefs by lazy {
        appContext.getSharedPreferences("elegan_likes_cache", Context.MODE_PRIVATE)
    }
    private val commentsPrefs by lazy {
        appContext.getSharedPreferences("elegan_comments_cache", Context.MODE_PRIVATE)
    }
    private val ordersPrefs by lazy {
        appContext.getSharedPreferences("elegan_orders_cache", Context.MODE_PRIVATE)
    }
    private val profilePrefs by lazy {
        appContext.getSharedPreferences("elegan_profile_cache", Context.MODE_PRIVATE)
    }
    private val publicationStatePrefs by lazy {
        appContext.getSharedPreferences("elegan_publication_states", Context.MODE_PRIVATE)
    }
    private val catalogStorePrefs by lazy {
        appContext.getSharedPreferences("elegan_catalog_store", Context.MODE_PRIVATE)
    }
    private val messagesPrefs by lazy {
        appContext.getSharedPreferences("elegan_support_messages", Context.MODE_PRIVATE)
    }

    private val productStockOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap("product_stock_overrides"))
    }
    private val announcementActiveOverridesFlow by lazy {
        MutableStateFlow(loadBooleanOverrideMap("announcement_active_overrides"))
    }
    private val customProductsFlow by lazy {
        MutableStateFlow(loadPersistedCustomProducts())
    }
    private val deletedProductIdsFlow by lazy {
        MutableStateFlow(loadPersistedIdSet("deleted_product_ids"))
    }
    private val customAnnouncementsFlow by lazy {
        MutableStateFlow(loadPersistedCustomAnnouncements())
    }
    private val deletedAnnouncementIdsFlow by lazy {
        MutableStateFlow(loadPersistedIdSet("deleted_announcement_ids"))
    }

    private val cartLoadMutex = Mutex()
    private val savedLoadMutex = Mutex()
    private val statsLoadMutex = Mutex()
    private val likesLoadMutex = Mutex()
    private val ordersLoadMutex = Mutex()
    private val messagesLoadMutex = Mutex()
    private val profileLoadMutex = Mutex()

    @Volatile
    private var loadedCartKey: String? = null
    @Volatile
    private var loadedSavedKey: String? = null
    @Volatile
    private var loadedStatsKey: String? = null
    @Volatile
    private var loadedLikesKey: String? = null
    @Volatile
    private var loadedOrdersKey: String? = null
    @Volatile
    private var loadedMessagesKey: String? = null
    @Volatile
    private var loadedProfileKey: String? = null

    private val localCommentsCache = java.util.concurrent.ConcurrentHashMap<String, MutableList<PublicationComment>>()
    private val localCommentsUpdated = MutableSharedFlow<String>(extraBufferCapacity = 32)
    private val localStatsMap = java.util.concurrent.ConcurrentHashMap<String, PublicationStats>()
    private val localStatsFlow = MutableStateFlow<Map<String, PublicationStats>>(emptyMap())
    private val localCartItems = java.util.concurrent.CopyOnWriteArrayList<CartItem>()
    private val localCartFlow = MutableStateFlow<List<CartItem>>(emptyList())
    private val localSavedProductIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val localSavedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val localLikedPublicationIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private val localLikedFlow = MutableStateFlow<Set<String>>(emptySet())
    private val localOrdersList = java.util.concurrent.CopyOnWriteArrayList<Order>()
    private val localOrdersFlow = MutableStateFlow<List<Order>>(emptyList())
    private val localMessagesList = java.util.concurrent.CopyOnWriteArrayList<SupportMessage>()
    private val localMessagesFlow = MutableStateFlow<List<SupportMessage>>(emptyList())
    private val localProfileFlow = MutableStateFlow<UserProfile?>(null)

    private fun userCacheKey(): String {
        val user = MobileSession.user(appContext)
        val email = user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        val id = user?.effectiveId?.trim()?.takeIf { it.isNotBlank() }
        return email ?: id ?: "guest"
    }

    private fun cartCacheKey(): String = "cart_items_${userCacheKey()}"
    private fun savedCacheKey(userKey: String = userCacheKey()): String = "saved_ids_$userKey"
    private fun likesCacheKey(userKey: String = userCacheKey()): String = "liked_ids_$userKey"
    private fun ordersCacheKey(userKey: String = userCacheKey()): String = "orders_list_$userKey"
    private fun statsCacheKey(userKey: String = userCacheKey()): String = "publication_stats_$userKey"
    private fun commentsCacheKey(productId: String): String = "comments_$productId"
    private fun profileCacheKey(userKey: String = userCacheKey()): String = "user_profile_$userKey"
    private fun messagesCacheKey(userKey: String = userCacheKey()): String = "support_messages_$userKey"

    // -------------------------------------------------------------
    // LOCAL MEDIA STORAGE HELPER
    // -------------------------------------------------------------

    private suspend fun persistLocalMediaFile(source: String, entityId: String, label: String): String {
        if (source.isBlank()) return ""
        if (source.startsWith("android.resource://")) return source
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(source)
                val localMediaRoot = File(appContext.filesDir, "elegan_local_media").apply {
                    if (!exists()) mkdirs()
                }
                if (uri.scheme == "file") {
                    val existing = File(uri.path ?: "")
                    if (existing.exists() && existing.absolutePath.startsWith(localMediaRoot.absolutePath)) {
                        return@withContext source
                    }
                }
                val isVideo = label.contains("video", ignoreCase = true) || source.endsWith(".mp4", true)
                val ext = if (isVideo) "mp4" else "jpg"
                val destFile = File(localMediaRoot, "${entityId}_${label}_${System.currentTimeMillis()}.$ext")
                val input = when (uri.scheme) {
                    "content" -> appContext.contentResolver.openInputStream(uri)
                    "file" -> File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
                    else -> File(source).takeIf { it.exists() }?.inputStream()
                }
                if (input != null) {
                    input.use { src ->
                        destFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                    Uri.fromFile(destFile).toString()
                } else {
                    source
                }
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar archivo multimedia local: ${e.message}")
                source
            }
        }
    }

    // -------------------------------------------------------------
    // CART PERSISTENCE
    // -------------------------------------------------------------

    private suspend fun ensureCartLoaded() {
        val currentKey = userCacheKey()
        if (loadedCartKey == currentKey) return
        cartLoadMutex.withLock {
            if (loadedCartKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedCart() }
            localCartItems.clear()
            if (loaded.isNotEmpty()) {
                localCartItems.addAll(loaded)
            }
            localCartFlow.value = localCartItems.toList()
            loadedCartKey = currentKey
        }
    }

    private fun loadPersistedCart(): List<CartItem> {
        val raw = cartPrefs.getString(cartCacheKey(), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        CartItem(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            productId = obj.optString("productId", ""),
                            name = obj.optString("name", ""),
                            price = obj.optDouble("price", 0.0),
                            imageUrl = obj.optString("imageUrl", ""),
                            selectedSize = obj.optString("selectedSize", ""),
                            selectedColor = obj.optString("selectedColor", ""),
                            quantity = obj.optLong("quantity", 1L).coerceAtLeast(1L)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer el carrito local persistido: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedCart(items: List<CartItem>) {
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (item in items) {
                    array.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("userId", item.userId)
                            put("productId", item.productId)
                            put("name", item.name)
                            put("price", item.price)
                            put("imageUrl", item.imageUrl)
                            put("selectedSize", item.selectedSize)
                            put("selectedColor", item.selectedColor)
                            put("quantity", item.quantity)
                        }
                    )
                }
                cartPrefs.edit().putString(cartCacheKey(), array.toString()).apply()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar el carrito localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishCartState() {
        val snapshot = localCartItems.toList()
        localCartFlow.value = snapshot
        savePersistedCart(snapshot)
    }

    // -------------------------------------------------------------
    // SAVED, LIKES & STATS PERSISTENCE
    // -------------------------------------------------------------

    private suspend fun ensureSavedLoaded() {
        val currentKey = userCacheKey()
        if (loadedSavedKey == currentKey) return
        savedLoadMutex.withLock {
            if (loadedSavedKey == currentKey) return
            val loadedIds = withContext(Dispatchers.IO) { loadPersistedSavedState(currentKey) }
            localSavedProductIds.clear()
            localSavedProductIds.addAll(loadedIds)
            localSavedFlow.value = localSavedProductIds.toSet()
            loadedSavedKey = currentKey
        }
    }

    private fun loadPersistedSavedState(userKey: String): Set<String> {
        val ids = mutableSetOf<String>()
        try {
            val rawIds = savedPrefs.getString(savedCacheKey(userKey), null)
            if (!rawIds.isNullOrBlank()) {
                val array = JSONArray(rawIds)
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) ids.add(id)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los productos guardados localmente: ${e.message}")
        }
        return ids
    }

    private suspend fun savePersistedSavedState(userKey: String = userCacheKey()) {
        val idsSnapshot = localSavedProductIds.toList()
        withContext(Dispatchers.IO) {
            try {
                val idsArray = JSONArray()
                idsSnapshot.forEach { idsArray.put(it) }
                savedPrefs.edit()
                    .putString(savedCacheKey(userKey), idsArray.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los productos localmente: ${e.message}")
            }
        }
    }

    private suspend fun ensureLikesLoaded() {
        val currentKey = userCacheKey()
        if (loadedLikesKey == currentKey) return
        likesLoadMutex.withLock {
            if (loadedLikesKey == currentKey) return
            val loadedIds = withContext(Dispatchers.IO) { loadPersistedLikesState(currentKey) }
            localLikedPublicationIds.clear()
            localLikedPublicationIds.addAll(loadedIds)
            localLikedFlow.value = localLikedPublicationIds.toSet()
            loadedLikesKey = currentKey
        }
    }

    private fun loadPersistedLikesState(userKey: String): Set<String> {
        val ids = mutableSetOf<String>()
        try {
            val rawIds = likesPrefs.getString(likesCacheKey(userKey), null)
            if (!rawIds.isNullOrBlank()) {
                val array = JSONArray(rawIds)
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) ids.add(id)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los likes locales: ${e.message}")
        }
        return ids
    }

    private suspend fun savePersistedLikesState(userKey: String = userCacheKey()) {
        val idsSnapshot = localLikedPublicationIds.toList()
        withContext(Dispatchers.IO) {
            try {
                val idsArray = JSONArray()
                idsSnapshot.forEach { idsArray.put(it) }
                likesPrefs.edit()
                    .putString(likesCacheKey(userKey), idsArray.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los likes localmente: ${e.message}")
            }
        }
    }

    private suspend fun ensureStatsLoaded() {
        val currentKey = userCacheKey()
        if (loadedStatsKey == currentKey) return
        statsLoadMutex.withLock {
            if (loadedStatsKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedStats(currentKey) }
            localStatsMap.clear()
            localStatsMap.putAll(loaded)
            reconcileSavedCountsLocked()
            reconcileLikedCountsLocked()
            localStatsFlow.value = localStatsMap.toMap()
            loadedStatsKey = currentKey
        }
    }

    private fun reconcileSavedCountsLocked() {
        localSavedProductIds.forEach { productId ->
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            if (current.savesCount < 1L) {
                localStatsMap[productId] = current.copy(savesCount = 1L)
            }
        }
    }

    private fun reconcileLikedCountsLocked() {
        localLikedPublicationIds.forEach { productId ->
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            if (current.likesCount < 1L) {
                localStatsMap[productId] = current.copy(likesCount = 1L)
            }
        }
    }

    private fun loadPersistedStats(userKey: String): Map<String, PublicationStats> {
        val raw = statsPrefs.getString(statsCacheKey(userKey), null) ?: return emptyMap()
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
        userKey: String = userCacheKey()
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
                statsPrefs.edit().putString(statsCacheKey(userKey), root.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar las estadísticas localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishStatsState() {
        val snapshot = localStatsMap.toMap()
        localStatsFlow.value = snapshot
        savePersistedStats(snapshot)
    }

    // -------------------------------------------------------------
    // PRODUCTS & ANNOUNCEMENTS (100% LOCAL)
    // -------------------------------------------------------------

    private fun loadBooleanOverrideMap(key: String): Map<String, Boolean> {
        val result = mutableMapOf<String, Boolean>()
        try {
            val raw = publicationStatePrefs.getString(key, null)
            if (!raw.isNullOrBlank()) {
                val obj = JSONObject(raw)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val id = keys.next()
                    if (id.isNotBlank()) {
                        result[id] = obj.optBoolean(id, true)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los estados locales ($key): ${e.message}")
        }
        return result
    }

    private fun saveBooleanOverrideMap(key: String, map: Map<String, Boolean>) {
        try {
            val obj = JSONObject()
            map.forEach { (id, state) -> obj.put(id, state) }
            publicationStatePrefs.edit().putString(key, obj.toString()).commit()
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron guardar los estados locales ($key): ${e.message}")
        }
    }

    private fun loadPersistedIdSet(key: String): Set<String> {
        val raw = catalogStorePrefs.getString(key, null) ?: return emptySet()
        return try {
            val array = JSONArray(raw)
            buildSet {
                for (i in 0 until array.length()) {
                    val id = array.optString(i, "").trim()
                    if (id.isNotBlank()) add(id)
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer conjunto local ($key): ${e.message}")
            emptySet()
        }
    }

    private fun savePersistedIdSet(key: String, ids: Set<String>) {
        try {
            val array = JSONArray()
            ids.forEach { array.put(it) }
            catalogStorePrefs.edit().putString(key, array.toString()).commit()
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo guardar conjunto local ($key): ${e.message}")
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val value = optString(i, "")
                if (value.isNotBlank()) add(value)
            }
        }
    }

    private fun loadPersistedCustomProducts(): List<Product> {
        val raw = catalogStorePrefs.getString("custom_products", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        Product(
                            id = obj.optString("id", ""),
                            name = obj.optString("name", ""),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "todo"),
                            price = obj.optDouble("price", 0.0),
                            imageUrl = obj.optString("imageUrl", ""),
                            images = obj.optJSONArray("images").toStringList(),
                            colors = obj.optJSONArray("colors").toStringList(),
                            rating = obj.optDouble("rating", 5.0),
                            reviewCount = obj.optLong("reviewCount", 0L),
                            sizes = obj.optJSONArray("sizes").toStringList(),
                            inStock = obj.optBoolean("inStock", true),
                            isFeatured = obj.optBoolean("isFeatured", false),
                            mediaFormat = obj.optString("mediaFormat", "1:1"),
                            videoUrl = obj.optString("videoUrl", ""),
                            isVideo = obj.optBoolean("isVideo", false),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los productos publicados localmente: ${e.message}")
            emptyList()
        }
    }

    private fun savePersistedCustomProducts(items: List<Product>) {
        try {
            val array = JSONArray()
            items.forEach { p ->
                array.put(
                    JSONObject().apply {
                        put("id", p.id)
                        put("name", p.name)
                        put("description", p.description)
                        put("category", p.category)
                        put("price", p.price)
                        put("imageUrl", p.imageUrl)
                        put("images", JSONArray(p.images))
                        put("colors", JSONArray(p.colors))
                        put("rating", p.rating)
                        put("reviewCount", p.reviewCount)
                        put("sizes", JSONArray(p.sizes))
                        put("inStock", p.inStock)
                        put("isFeatured", p.isFeatured)
                        put("mediaFormat", p.mediaFormat)
                        put("videoUrl", p.videoUrl)
                        put("isVideo", p.isVideo)
                        put("createdAt", p.createdAt.orEmpty())
                    }
                )
            }
            catalogStorePrefs.edit().putString("custom_products", array.toString()).commit()
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron guardar los productos publicados localmente: ${e.message}")
        }
    }

    private fun loadPersistedCustomAnnouncements(): List<Announcement> {
        val raw = catalogStorePrefs.getString("custom_announcements", null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        Announcement(
                            id = obj.optString("id", ""),
                            title = obj.optString("title", ""),
                            message = obj.optString("message", ""),
                            type = obj.optString("type", "banner"),
                            imageUrl = obj.optString("imageUrl", ""),
                            discountCode = obj.optString("discountCode", ""),
                            discountPercent = obj.optDouble("discountPercent", 0.0),
                            ctaText = obj.optString("ctaText", "Ver Más"),
                            targetCategory = obj.optString("targetCategory", "todo"),
                            isActive = obj.optBoolean("isActive", true),
                            videoUrl = obj.optString("videoUrl", ""),
                            isVideo = obj.optBoolean("isVideo", false),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los anuncios publicados localmente: ${e.message}")
            emptyList()
        }
    }

    private fun savePersistedCustomAnnouncements(items: List<Announcement>) {
        try {
            val array = JSONArray()
            items.forEach { ann ->
                array.put(
                    JSONObject().apply {
                        put("id", ann.id)
                        put("title", ann.title)
                        put("message", ann.message)
                        put("type", ann.type)
                        put("imageUrl", ann.imageUrl)
                        put("discountCode", ann.discountCode)
                        put("discountPercent", ann.discountPercent)
                        put("ctaText", ann.ctaText)
                        put("targetCategory", ann.targetCategory)
                        put("isActive", ann.isActive)
                        put("videoUrl", ann.videoUrl)
                        put("isVideo", ann.isVideo)
                        put("createdAt", ann.createdAt.orEmpty())
                    }
                )
            }
            catalogStorePrefs.edit().putString("custom_announcements", array.toString()).commit()
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron guardar los anuncios publicados localmente: ${e.message}")
        }
    }

    private fun mergedProducts(
        custom: List<Product>,
        deletedIds: Set<String>,
        overrides: Map<String, Boolean>
    ): List<Product> {
        return (custom + getInitialSampleProducts())
            .distinctBy { it.id }
            .filter { it.id !in deletedIds }
            .map { p -> overrides[p.id]?.let { inStock -> p.copy(inStock = inStock) } ?: p }
    }

    fun observeProducts(): Flow<List<Product>> =
        combine(
            customProductsFlow,
            deletedProductIdsFlow,
            productStockOverridesFlow
        ) { custom, deletedIds, overrides ->
            mergedProducts(custom, deletedIds, overrides)
        }.flowOn(Dispatchers.IO)

    suspend fun seedProductsIfEmpty() {
        // El catálogo se alimenta directamente desde el almacenamiento local.
    }

    suspend fun seedAnnouncementsIfEmpty() {
        // Los anuncios se alimentan directamente desde el almacenamiento local.
    }

    suspend fun getProductById(productId: String): Product? =
        mergedProducts(
            customProductsFlow.value,
            deletedProductIdsFlow.value,
            productStockOverridesFlow.value
        ).find { it.id == productId }

    private fun getFallbackCoverForCategory(category: String): String {
        return LocalBoutiqueMedia.fallbackImageForCategory(category)
    }

    suspend fun addProduct(product: Product): String {
        val productId = product.id.ifBlank { "prod_${java.util.UUID.randomUUID()}" }
        val category = product.category.ifBlank { "camisa" }
        val sourceVideo = product.videoUrl.takeIf { product.isVideo }.orEmpty()
        val coverSource = if (product.isVideo) {
            if (product.imageUrl.isNotBlank() && product.imageUrl != sourceVideo) product.imageUrl
            else getFallbackCoverForCategory(category)
        } else {
            product.imageUrl.ifBlank { product.images.firstOrNull() ?: getFallbackCoverForCategory(category) }
        }

        val savedVideoUrl = if (product.isVideo && sourceVideo.isNotBlank()) {
            persistLocalMediaFile(sourceVideo, productId, "video")
        } else ""
        val savedCoverUrl = persistLocalMediaFile(coverSource, productId, "cover")
            .ifBlank { getFallbackCoverForCategory(category) }
        val gallery = (product.images + listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() }))
            .distinct()
            .filterNot { it == coverSource || it == sourceVideo }
            .mapIndexed { idx, uri -> persistLocalMediaFile(uri, productId, "gallery_$idx") }
            .filter { it.isNotBlank() }
        val images = (listOf(savedCoverUrl) + gallery).distinct()

        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val finalProduct = product.copy(
            id = productId,
            category = category,
            imageUrl = savedCoverUrl,
            images = images,
            videoUrl = savedVideoUrl,
            isVideo = product.isVideo || savedVideoUrl.isNotBlank(),
            createdAt = product.createdAt ?: nowIso
        )

        withContext(Dispatchers.IO) {
            val updated = listOf(finalProduct) + customProductsFlow.value.filterNot { it.id == productId }
            customProductsFlow.value = updated
            savePersistedCustomProducts(updated)
            if (productId in deletedProductIdsFlow.value) {
                val updatedDeleted = deletedProductIdsFlow.value - productId
                deletedProductIdsFlow.value = updatedDeleted
                savePersistedIdSet("deleted_product_ids", updatedDeleted)
            }
        }
        return productId
    }

    suspend fun addAnnouncement(announcement: Announcement): String {
        val id = announcement.id.ifBlank { "ann_${java.util.UUID.randomUUID()}" }
        val image = if (announcement.imageUrl.isNotBlank()) {
            persistLocalMediaFile(announcement.imageUrl, id, "announcement_img")
        } else {
            getFallbackCoverForCategory(announcement.targetCategory)
        }
        val video = if (announcement.videoUrl.isNotBlank()) {
            persistLocalMediaFile(announcement.videoUrl, id, "announcement_video")
        } else ""

        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val finalAnnouncement = announcement.copy(
            id = id,
            imageUrl = image,
            videoUrl = video,
            isVideo = announcement.hasVideo || video.isNotBlank(),
            createdAt = announcement.createdAt ?: nowIso
        )

        withContext(Dispatchers.IO) {
            val updated = listOf(finalAnnouncement) + customAnnouncementsFlow.value.filterNot { it.id == id }
            customAnnouncementsFlow.value = updated
            savePersistedCustomAnnouncements(updated)
            if (id in deletedAnnouncementIdsFlow.value) {
                val updatedDeleted = deletedAnnouncementIdsFlow.value - id
                deletedAnnouncementIdsFlow.value = updatedDeleted
                savePersistedIdSet("deleted_announcement_ids", updatedDeleted)
            }
        }
        return id
    }

    fun observeAnnouncements(): Flow<List<Announcement>> =
        combine(
            customAnnouncementsFlow,
            deletedAnnouncementIdsFlow,
            announcementActiveOverridesFlow
        ) { custom, deletedIds, overrides ->
            (custom + getInitialSampleAnnouncements())
                .distinctBy { it.id }
                .filter { it.id !in deletedIds }
                .map { ann -> overrides[ann.id]?.let { active -> ann.copy(isActive = active) } ?: ann }
        }.flowOn(Dispatchers.IO)

    suspend fun deleteProduct(productId: String) {
        withContext(Dispatchers.IO) {
            val updatedCustom = customProductsFlow.value.filterNot { it.id == productId }
            customProductsFlow.value = updatedCustom
            savePersistedCustomProducts(updatedCustom)

            val updatedDeleted = deletedProductIdsFlow.value + productId
            deletedProductIdsFlow.value = updatedDeleted
            savePersistedIdSet("deleted_product_ids", updatedDeleted)
        }
    }

    suspend fun toggleProductStock(productId: String, inStock: Boolean) {
        val updatedOverrides = productStockOverridesFlow.value + (productId to inStock)
        productStockOverridesFlow.value = updatedOverrides
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap("product_stock_overrides", updatedOverrides)
        }
    }

    suspend fun deleteAnnouncement(announcementId: String) {
        withContext(Dispatchers.IO) {
            val updatedCustom = customAnnouncementsFlow.value.filterNot { it.id == announcementId }
            customAnnouncementsFlow.value = updatedCustom
            savePersistedCustomAnnouncements(updatedCustom)

            val updatedDeleted = deletedAnnouncementIdsFlow.value + announcementId
            deletedAnnouncementIdsFlow.value = updatedDeleted
            savePersistedIdSet("deleted_announcement_ids", updatedDeleted)
        }
    }

    suspend fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        val updatedOverrides = announcementActiveOverridesFlow.value + (announcementId to isActive)
        announcementActiveOverridesFlow.value = updatedOverrides
        withContext(Dispatchers.IO) {
            saveBooleanOverrideMap("announcement_active_overrides", updatedOverrides)
        }
    }

    fun getInitialSampleAnnouncements(): List<Announcement> = listOf(
        Announcement(
            id = "ann_1",
            title = "Gran Venta Sartorial 2026",
            message = "Disfruta de hasta un 25% de descuento en trajes y camisas Oxford seleccionadas por tiempo limitado.",
            type = "descuento",
            imageUrl = LocalBoutiqueMedia.camisaCover,
            discountCode = "SARTORIAL25",
            discountPercent = 25.0,
            ctaText = "Ver Colección",
            targetCategory = "camisa",
            isActive = true
        ),
        Announcement(
            id = "ann_2",
            title = "Nueva Colección: Fragancias Noir",
            message = "Descubre nuestras esencias exclusivas destiladas para el caballero contemporáneo.",
            type = "banner",
            imageUrl = LocalBoutiqueMedia.perfumeCover,
            discountCode = "",
            discountPercent = 0.0,
            ctaText = "Explorar Perfumes",
            targetCategory = "perfume",
            isActive = true
        ),
        Announcement(
            id = "ann_3",
            title = "Artesanía & Calzado Goodyear",
            message = "Zapatos hechos a mano con pieles italianas de la más alta distinción.",
            type = "coleccion",
            imageUrl = LocalBoutiqueMedia.zapatoCover,
            discountCode = "",
            discountPercent = 0.0,
            ctaText = "Ver Zapatos",
            targetCategory = "zapato",
            isActive = true
        )
    )

    // -------------------------------------------------------------
    // CART
    // -------------------------------------------------------------

    fun observeCart(): Flow<List<CartItem>> = flow {
        ensureCartLoaded()
        localCartFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun observeSavedProductIds(): Flow<Set<String>> = flow {
        ensureSavedLoaded()
        ensureStatsLoaded()
        localSavedFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun setProductSaved(productId: String, isSaved: Boolean) {
        require(productId.isNotBlank())
        ensureSavedLoaded()
        ensureStatsLoaded()

        val wasSaved = productId in localSavedProductIds
        if (isSaved) {
            localSavedProductIds.add(productId)
        } else {
            localSavedProductIds.remove(productId)
        }
        localSavedFlow.value = localSavedProductIds.toSet()
        savePersistedSavedState()

        if (wasSaved != isSaved) {
            val current = localStatsMap[productId] ?: PublicationStats(id = productId)
            val updatedSaves = if (isSaved) {
                current.savesCount + 1L
            } else {
                (current.savesCount - 1L).coerceAtLeast(0L)
            }
            localStatsMap[productId] = current.copy(savesCount = updatedSaves)
            publishStatsState()
        }
    }

    fun observePublicationStats(): Flow<Map<String, PublicationStats>> = flow {
        ensureSavedLoaded()
        ensureLikesLoaded()
        ensureStatsLoaded()
        localStatsFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    fun observeLikedPublicationIds(): Flow<Set<String>> = flow {
        ensureLikesLoaded()
        ensureStatsLoaded()
        localLikedFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun togglePublicationLike(productId: String): Boolean {
        require(productId.isNotBlank())
        ensureLikesLoaded()
        ensureStatsLoaded()

        val wasLiked = productId in localLikedPublicationIds
        val targetLiked = !wasLiked
        if (targetLiked) {
            localLikedPublicationIds.add(productId)
        } else {
            localLikedPublicationIds.remove(productId)
        }
        localLikedFlow.value = localLikedPublicationIds.toSet()
        savePersistedLikesState()

        val current = localStatsMap[productId] ?: PublicationStats(id = productId)
        val updatedLikes = if (targetLiked) {
            current.likesCount + 1L
        } else {
            (current.likesCount - 1L).coerceAtLeast(0L)
        }
        localStatsMap[productId] = current.copy(likesCount = updatedLikes)
        publishStatsState()
        return targetLiked
    }

    // -------------------------------------------------------------
    // COMMENTS (100% LOCAL)
    // -------------------------------------------------------------

    private fun loadPersistedComments(productId: String): MutableList<PublicationComment> {
        val raw = commentsPrefs.getString(commentsCacheKey(productId), null) ?: return mutableListOf()
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
                commentsPrefs.edit().putString(commentsCacheKey(productId), array.toString()).commit()
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

        val userId = optionalUserId() ?: "cliente_${java.util.UUID.randomUUID().toString().take(8)}"
        val finalName = displayName.trim().ifBlank { "Cliente" }.take(80)
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val newComment = PublicationComment(
            id = "local_${java.util.UUID.randomUUID()}",
            productId = productId,
            userId = userId,
            displayName = finalName,
            text = clean,
            createdAt = nowIso
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
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(commentsCount = currentStat.commentsCount + 1)
        publishStatsState()
    }

    suspend fun recordPublicationShare(productId: String) {
        require(productId.isNotBlank())
        ensureStatsLoaded()
        val currentStat = localStatsMap[productId] ?: PublicationStats(id = productId)
        localStatsMap[productId] = currentStat.copy(sharesCount = currentStat.sharesCount + 1)
        publishStatsState()
    }

    suspend fun addToCart(product: Product, selectedSize: String, quantity: Long = 1L, selectedColor: String = "") {
        require(product.id.isNotBlank() && quantity > 0)
        ensureCartLoaded()
        val existingIndex = localCartItems.indexOfFirst {
            it.productId == product.id && it.selectedSize == selectedSize && it.selectedColor == selectedColor
        }
        if (existingIndex >= 0) {
            val existing = localCartItems[existingIndex]
            localCartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            localCartItems.add(
                CartItem(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = optionalUserId().orEmpty(),
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    selectedSize = selectedSize,
                    selectedColor = selectedColor,
                    quantity = quantity
                )
            )
        }
        publishCartState()
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        ensureCartLoaded()
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }
        val index = localCartItems.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            localCartItems[index] = localCartItems[index].copy(quantity = newQuantity)
            publishCartState()
        }
    }

    suspend fun removeFromCart(cartItemId: String) {
        ensureCartLoaded()
        localCartItems.removeAll { it.id == cartItemId }
        publishCartState()
    }

    suspend fun clearCart() {
        ensureCartLoaded()
        localCartItems.clear()
        publishCartState()
    }

    // -------------------------------------------------------------
    // ORDERS (100% LOCAL)
    // -------------------------------------------------------------

    private suspend fun ensureOrdersLoaded() {
        val currentKey = userCacheKey()
        if (loadedOrdersKey == currentKey) return
        ordersLoadMutex.withLock {
            if (loadedOrdersKey == currentKey) return
            val loadedOrders = withContext(Dispatchers.IO) { loadPersistedOrdersState(currentKey) }
            localOrdersList.clear()
            if (loadedOrders.isNotEmpty()) {
                localOrdersList.addAll(loadedOrders)
            }
            localOrdersFlow.value = localOrdersList.toList()
            loadedOrdersKey = currentKey
        }
    }

    private fun loadPersistedOrdersState(userKey: String): List<Order> {
        val orders = mutableListOf<Order>()
        try {
            val rawOrders = ordersPrefs.getString(ordersCacheKey(userKey), null)
            if (!rawOrders.isNullOrBlank()) {
                val array = JSONArray(rawOrders)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    orders.add(
                        Order(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            customerName = obj.optString("customerName", ""),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            city = obj.optString("city", ""),
                            paymentMethod = obj.optString("paymentMethod", "Tarjeta"),
                            paymentProofUrl = obj.optString("paymentProofUrl", ""),
                            total = obj.optDouble("total", 0.0),
                            itemCount = obj.optLong("itemCount", 1L).coerceAtLeast(1L),
                            status = obj.optString("status", "confirmado"),
                            itemsSummary = obj.optString("itemsSummary", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los pedidos locales: ${e.message}")
        }
        return orders
    }

    private suspend fun savePersistedOrdersState(
        ordersSnapshot: List<Order> = localOrdersList.toList(),
        userKey: String = userCacheKey()
    ) {
        withContext(Dispatchers.IO) {
            try {
                val ordersArray = JSONArray()
                ordersSnapshot.forEach { order ->
                    ordersArray.put(
                        JSONObject().apply {
                            put("id", order.id)
                            put("userId", order.userId)
                            put("customerName", order.customerName)
                            put("email", order.email)
                            put("phone", order.phone)
                            put("address", order.address)
                            put("city", order.city)
                            put("paymentMethod", order.paymentMethod)
                            put("paymentProofUrl", order.paymentProofUrl)
                            put("total", order.total)
                            put("itemCount", order.itemCount)
                            put("status", order.status)
                            put("itemsSummary", order.itemsSummary)
                            put("createdAt", order.createdAt.orEmpty())
                        }
                    )
                }
                ordersPrefs.edit()
                    .putString(ordersCacheKey(userKey), ordersArray.toString())
                    .commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los pedidos localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishOrdersState() {
        val snapshot = localOrdersList.toList()
        localOrdersFlow.value = snapshot
        savePersistedOrdersState(ordersSnapshot = snapshot)
    }

    fun observeOrders(): Flow<List<Order>> = flow {
        ensureOrdersLoaded()
        localOrdersFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun createOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        paymentProofUri: String = ""
    ): String {
        ensureCartLoaded()
        ensureOrdersLoaded()

        val localId = "order_" + java.util.UUID.randomUUID().toString().take(8)
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }.format(java.util.Date())

        val persistedProofUrl = if (paymentProofUri.isNotBlank()) {
            persistLocalMediaFile(paymentProofUri, localId, "payment_proof")
        } else ""

        val initialOrder = Order(
            id = localId,
            userId = optionalUserId().orEmpty(),
            customerName = customerName,
            email = email,
            phone = phone,
            address = address,
            city = city,
            paymentMethod = paymentMethod,
            paymentProofUrl = persistedProofUrl,
            total = total,
            itemCount = itemCount.coerceAtLeast(1L),
            status = "confirmado",
            itemsSummary = itemsSummary,
            createdAt = nowIso
        )

        localOrdersList.add(0, initialOrder)
        publishOrdersState()

        localCartItems.clear()
        publishCartState()

        return localId
    }

    // -------------------------------------------------------------
    // SUPPORT MESSAGES (100% LOCAL)
    // -------------------------------------------------------------

    private suspend fun ensureMessagesLoaded() {
        val currentKey = userCacheKey()
        if (loadedMessagesKey == currentKey) return
        messagesLoadMutex.withLock {
            if (loadedMessagesKey == currentKey) return
            val currentUserId = optionalUserId().orEmpty()
            val welcome = SupportMessage(
                id = "welcome",
                userId = currentUserId,
                sender = "support",
                senderName = "Concierge Elegan",
                text = "¡Bienvenido a Elegan! Soy tu asesor personal de estilo y compras. ¿En qué podemos ayudarte hoy?"
            )
            val loaded = withContext(Dispatchers.IO) { loadPersistedMessages(currentKey) }
            localMessagesList.clear()
            if (loaded.isNotEmpty()) {
                localMessagesList.addAll(loaded)
            } else {
                localMessagesList.add(welcome)
            }
            localMessagesFlow.value = localMessagesList.toList()
            loadedMessagesKey = currentKey
        }
    }

    private fun loadPersistedMessages(userKey: String): List<SupportMessage> {
        val raw = messagesPrefs.getString(messagesCacheKey(userKey), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        SupportMessage(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            sender = obj.optString("sender", "user"),
                            senderName = obj.optString("senderName", ""),
                            text = obj.optString("text", ""),
                            mediaUrl = obj.optString("mediaUrl", ""),
                            mediaType = obj.optString("mediaType", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los mensajes locales: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedMessages(userKey: String = userCacheKey()) {
        val snapshot = localMessagesList.toList()
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (msg in snapshot) {
                    array.put(
                        JSONObject().apply {
                            put("id", msg.id)
                            put("userId", msg.userId)
                            put("sender", msg.sender)
                            put("senderName", msg.senderName)
                            put("text", msg.text)
                            put("mediaUrl", msg.mediaUrl)
                            put("mediaType", msg.mediaType)
                            put("createdAt", msg.createdAt.orEmpty())
                        }
                    )
                }
                messagesPrefs.edit().putString(messagesCacheKey(userKey), array.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los mensajes localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishMessagesState() {
        localMessagesFlow.value = localMessagesList.toList()
        savePersistedMessages()
    }

    private suspend fun persistSupportMediaLocally(source: String, msgId: String, isVideo: Boolean): String {
        if (source.isBlank() || source.startsWith("android.resource://")) return source
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(source)
                if (uri.scheme == "file" && uri.path?.let { File(it).exists() } == true) {
                    return@withContext source
                }
                val ext = if (isVideo) "mp4" else "jpg"
                val dir = File(appContext.filesDir, "support_media").apply { if (!exists()) mkdirs() }
                val destFile = File(dir, "${msgId}.$ext")
                val input = when (uri.scheme) {
                    "content" -> appContext.contentResolver.openInputStream(uri)
                    "file" -> File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
                    else -> File(source).takeIf { it.exists() }?.inputStream()
                }
                if (input != null) {
                    input.use { src ->
                        destFile.outputStream().use { dst -> src.copyTo(dst) }
                    }
                    Uri.fromFile(destFile).toString()
                } else {
                    source
                }
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo copiar medio local del chat: ${e.message}")
                source
            }
        }
    }

    fun observeMessages(): Flow<List<SupportMessage>> = flow {
        ensureMessagesLoaded()
        localMessagesFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun sendSupportMessage(
        text: String,
        mediaUri: String = "",
        mediaType: String = "",
        @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
    ) {
        ensureMessagesLoaded()
        val clean = text.trim()
        val cleanMedia = mediaUri.trim()
        require((clean.isNotEmpty() && clean.length <= 4000) || cleanMedia.isNotEmpty())
        val msgId = java.util.UUID.randomUUID().toString()
        val resolvedType = if (cleanMedia.isNotEmpty()) {
            if (mediaType.equals("video", ignoreCase = true)) "video" else "image"
        } else ""
        val isVideo = resolvedType == "video"
        val localMediaUrl = if (cleanMedia.isNotEmpty()) {
            persistSupportMediaLocally(cleanMedia, msgId, isVideo)
        } else ""

        val nowIso = java.time.Instant.now().toString()
        val userMsg = SupportMessage(
            id = msgId,
            userId = optionalUserId().orEmpty(),
            sender = "user",
            senderName = MobileSession.user(appContext)?.name ?: "Cliente",
            text = clean,
            mediaUrl = localMediaUrl,
            mediaType = resolvedType,
            createdAt = nowIso
        )
        localMessagesList.add(userMsg)
        publishMessagesState()

        val normalized = clean.lowercase()
        val replyText = when {
            clean.isEmpty() && localMediaUrl.isNotEmpty() ->
                "Hemos recibido tu archivo multimedia. Un asesor de Elegan lo revisará enseguida para ayudarte."
            normalized.contains("envio") || normalized.contains("entrega") ->
                "Gracias por tu consulta. Nuestro equipo de Elegan te ayudará con la información de entrega de tu pedido."
            normalized.contains("talla") || normalized.contains("medida") ->
                "Gracias por escribirnos. Nuestro equipo puede orientarte con las tallas y medidas disponibles."
            normalized.contains("devolucion") || normalized.contains("cambio") ->
                "Recibimos tu consulta sobre cambios o devoluciones. Un asesor de Elegan te dará seguimiento."
            normalized.contains("descuento") || normalized.contains("promocion") ->
                "Gracias por consultar nuestras promociones. Revisa los anuncios y ofertas vigentes en la tienda."
            else ->
                "Gracias por comunicarte con Elegan. Hemos recibido tu consulta y nuestro equipo te dará seguimiento."
        }
        localMessagesList.add(
            SupportMessage(
                id = java.util.UUID.randomUUID().toString(),
                userId = optionalUserId().orEmpty(),
                sender = "support",
                senderName = "Elegan Concierge",
                text = replyText,
                createdAt = java.time.Instant.now().plusMillis(1).toString()
            )
        )
        publishMessagesState()
    }

    // -------------------------------------------------------------
    // USER PROFILE & DELIVERY DATA (100% LOCAL)
    // -------------------------------------------------------------

    private suspend fun ensureProfileLoaded() {
        val currentKey = userCacheKey()
        if (loadedProfileKey == currentKey) return
        profileLoadMutex.withLock {
            if (loadedProfileKey == currentKey) return
            val cachedUser = MobileSession.user(appContext)
            val saved = withContext(Dispatchers.IO) { loadPersistedProfile(currentKey) }
            val resolved = when {
                saved != null -> saved.copy(
                    userId = saved.userId.ifBlank { cachedUser?.effectiveId.orEmpty() },
                    displayName = saved.displayName.ifBlank { cachedUser?.name.orEmpty() },
                    email = saved.email.ifBlank { cachedUser?.email.orEmpty() },
                    photoUrl = saved.photoUrl.ifBlank { cachedUser?.effectivePhotoUrl.orEmpty() }
                )
                cachedUser != null -> UserProfile(
                    id = cachedUser.effectiveId,
                    userId = cachedUser.effectiveId,
                    displayName = cachedUser.name,
                    email = cachedUser.email.orEmpty(),
                    photoUrl = cachedUser.effectivePhotoUrl
                )
                else -> null
            }
            localProfileFlow.value = resolved
            loadedProfileKey = currentKey
        }
    }

    private fun loadPersistedProfile(userKey: String): UserProfile? {
        val raw = profilePrefs.getString(profileCacheKey(userKey), null) ?: return null
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = obj.optString("id", ""),
                userId = obj.optString("userId", ""),
                displayName = obj.optString("displayName", ""),
                email = obj.optString("email", ""),
                photoUrl = obj.optString("photoUrl", ""),
                phone = obj.optString("phone", ""),
                address = obj.optString("address", ""),
                city = obj.optString("city", ""),
                updatedAt = obj.optString("updatedAt", "").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer el perfil local: ${e.message}")
            null
        }
    }

    private suspend fun savePersistedProfile(profile: UserProfile, userKey: String = userCacheKey()) {
        withContext(Dispatchers.IO) {
            try {
                val obj = JSONObject().apply {
                    put("id", profile.id)
                    put("userId", profile.userId)
                    put("displayName", profile.displayName)
                    put("email", profile.email)
                    put("photoUrl", profile.photoUrl)
                    put("phone", profile.phone)
                    put("address", profile.address)
                    put("city", profile.city)
                    put("updatedAt", profile.updatedAt.orEmpty())
                }
                profilePrefs.edit().putString(profileCacheKey(userKey), obj.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar el perfil local: ${e.message}")
            }
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        ensureProfileLoaded()
        localProfileFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun saveUserProfile(phone: String, address: String, city: String) {
        ensureProfileLoaded()
        val user = MobileSession.user(appContext)
        val nowIso = java.time.Instant.now().toString()
        val profile = UserProfile(
            id = user?.effectiveId.orEmpty(),
            userId = optionalUserId().orEmpty(),
            displayName = user?.name.orEmpty(),
            email = user?.email.orEmpty(),
            photoUrl = user?.effectivePhotoUrl.orEmpty(),
            phone = phone.trim(),
            address = address.trim(),
            city = city.trim(),
            updatedAt = nowIso
        )
        localProfileFlow.value = profile
        savePersistedProfile(profile)
    }

    fun getInitialSampleProducts(): List<Product> = listOf(
        Product(
            id = "prod_gorra_1",
            name = "Boina Clásica Gatsby Peaky Tweed",
            description = "Confeccionada con lana británica de espiga en tono marrón carbón. Interior forrado en satén sedoso para una sensación inigualable.",
            category = "gorra",
            price = 55.00,
            imageUrl = LocalBoutiqueMedia.gorraCover,
            rating = 4.9,
            reviewCount = 142,
            sizes = listOf("S (56cm)", "M (58cm)", "L (60cm)", "XL (62cm)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_gorra_2",
            name = "Gorra Newsboy Borgoña Signature",
            description = "Elegante gorra de 8 paneles en pana fina color vino intenso. Un homenaje al corte tradicional inglés con acabados a mano.",
            category = "gorra",
            price = 48.00,
            imageUrl = LocalBoutiqueMedia.gorraCover,
            rating = 4.8,
            reviewCount = 98,
            sizes = listOf("M (58cm)", "L (60cm)"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_gafa_1",
            name = "Gafas de Sol Aviador Gold Master",
            description = "Montura de titanio bañada en oro de 18k con lentes polarizadas verde bosque degradadas. Protección UV400 completa con estuche de cuero.",
            category = "gafa",
            price = 145.00,
            imageUrl = LocalBoutiqueMedia.gafaCover,
            rating = 5.0,
            reviewCount = 210,
            sizes = listOf("Única (54-18-145)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_gafa_2",
            name = "Gafas Clubmaster Carey Vintage",
            description = "Acetato italiano pulido a mano con detalles metálicos plateados. Diseño atemporal para caballeros distinguidos.",
            category = "gafa",
            price = 120.00,
            imageUrl = LocalBoutiqueMedia.gafaCover,
            rating = 4.7,
            reviewCount = 85,
            sizes = listOf("Única (51-20-145)"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_camisa_1",
            name = "Camisa Oxford Blanca Algodón Egipcio",
            description = "Tejido dos cabos de algodón Giza 100%. Cuello cutaway reforzado y botones de nácar natural. Planchado antiarrugas permanente.",
            category = "camisa",
            price = 89.00,
            imageUrl = LocalBoutiqueMedia.camisaCover,
            rating = 4.9,
            reviewCount = 340,
            sizes = listOf("38 (S)", "40 (M)", "42 (L)", "44 (XL)", "46 (XXL)"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_camisa_2",
            name = "Blazer Sartorial Borgoña Imperial",
            description = "Chaqueta estructurada de sarga de lana peinada con solapa en punta de lanza y botones esmaltados. La insignia indiscutible de Elegan.",
            category = "camisa",
            price = 280.00,
            imageUrl = LocalBoutiqueMedia.camisaCover,
            rating = 5.0,
            reviewCount = 115,
            sizes = listOf("48R", "50R", "52R", "54R"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_correa_1",
            name = "Correa de Cuero Vacuno Hebilla Dorada",
            description = "Piel de curtición vegetal teñida en anilina color coñac intenso. Hebilla de latón macizo cepillado de alta durabilidad.",
            category = "correa",
            price = 65.00,
            imageUrl = LocalBoutiqueMedia.zapatoCover,
            rating = 4.8,
            reviewCount = 160,
            sizes = listOf("85cm", "90cm", "95cm", "100cm", "105cm"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_correa_2",
            name = "Cinturón Reversible Negro & Borgoña",
            description = "Mecanismo giratorio suizo que permite alternar entre cuero negro caviar texturizado y borgoña liso satinado.",
            category = "correa",
            price = 75.00,
            imageUrl = LocalBoutiqueMedia.zapatoCover,
            rating = 4.9,
            reviewCount = 130,
            sizes = listOf("90cm", "95cm", "100cm", "105cm"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_pantalone_1",
            name = "Pantalón de Vestir Plisado Gris Carbón",
            description = "Corte sartorial con doble pliegue frontal, ajustadores laterales sin necesidad de cinturón y caída recta impecable.",
            category = "pantalone",
            price = 110.00,
            imageUrl = LocalBoutiqueMedia.camisaCover,
            rating = 4.9,
            reviewCount = 192,
            sizes = listOf("30", "32", "34", "36", "38"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_pantalone_2",
            name = "Pantalón Chino Premium Arena",
            description = "Algodón stretch de 280g con tacto melocotón. Ideal tanto para ocasiones de negocios informales como para fines de semana elegantes.",
            category = "pantalone",
            price = 85.00,
            imageUrl = LocalBoutiqueMedia.camisaCover,
            rating = 4.7,
            reviewCount = 94,
            sizes = listOf("30", "32", "34", "36"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_zapato_1",
            name = "Zapatos Oxford Cuero Glaseado Cap-Toe",
            description = "Construcción Goodyear welted con suela de cuero apomazado. Piel de becerro de grano fino con acabado espejo en la puntera.",
            category = "zapato",
            price = 240.00,
            imageUrl = LocalBoutiqueMedia.zapatoCover,
            rating = 5.0,
            reviewCount = 280,
            sizes = listOf("40", "41", "42", "43", "44", "45"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_zapato_2",
            name = "Zapatos Derby Brogue Borgoña Antik",
            description = "Perforado brogue wingtip artesanal en tono burdeos envejecido. Plantilla con amortiguación de corcho natural transpirable.",
            category = "zapato",
            price = 220.00,
            imageUrl = LocalBoutiqueMedia.zapatoCover,
            rating = 4.9,
            reviewCount = 175,
            sizes = listOf("41", "42", "43", "44"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_interiore_1",
            name = "Pack 3 Bóxers Modal Seda Elegan",
            description = "Micro-modal ultra suave con pretina elástica forrada en algodón que no marca ni oprime. Máxima frescura y elegancia interior.",
            category = "interiore",
            price = 45.00,
            imageUrl = LocalBoutiqueMedia.camisaCover,
            rating = 4.8,
            reviewCount = 150,
            sizes = listOf("S", "M", "L", "XL"),
            inStock = true,
            isFeatured = false
        ),
        Product(
            id = "prod_perfume_1",
            name = "Elegan Noir Eau de Parfum 100ml",
            description = "Notas de salida de bergamota y cardamomo, corazón de cedro del Atlas y fondo profundo de ámbar gris y cuero ahumado.",
            category = "perfume",
            price = 135.00,
            imageUrl = LocalBoutiqueMedia.perfumeCover,
            rating = 5.0,
            reviewCount = 410,
            sizes = listOf("100ml Spray"),
            inStock = true,
            isFeatured = true
        ),
        Product(
            id = "prod_perfume_2",
            name = "Gentleman Reserve Extrait de Parfum",
            description = "Alta concentración con notas de tabaco rubio, vainilla bourbon y maderas nobles. Duración superior a 14 horas en piel.",
            category = "perfume",
            price = 165.00,
            imageUrl = LocalBoutiqueMedia.perfumeCover,
            rating = 4.9,
            reviewCount = 225,
            sizes = listOf("50ml Extrait"),
            inStock = true,
            isFeatured = true
        )
    )
}
