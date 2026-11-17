package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Product
import com.example.data.remote.ApiAutenticacionMovil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

internal class RepositorioCatalogo {

    private val fetchMutex = Mutex()
    private val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    fun observeProducts(): Flow<List<Product>> = productsFlow.asStateFlow()

    fun observeLoading(): Flow<Boolean> = isLoadingFlow.asStateFlow()

    fun observeError(): Flow<String?> = errorFlow.asStateFlow()

    suspend fun refreshProducts(): List<Product> = fetchMutex.withLock {
        isLoadingFlow.value = true
        errorFlow.value = null
        try {
            val remoteProducts = fetchProductsFromBackend()
            productsFlow.value = remoteProducts
            remoteProducts
        } catch (e: Exception) {
            Log.e("EleganCatalog", "Error al obtener productos desde el backend.", e)
            errorFlow.value = e.localizedMessage ?: "No se pudo cargar el catálogo desde el servidor."
            emptyList()
        } finally {
            isLoadingFlow.value = false
        }
    }

    suspend fun fetchProductsFromBackend(): List<Product> = withContext(Dispatchers.IO) {
        val rawBody = ApiAutenticacionMovil.service.products().use { it.string() }
        val parsed = parseBackendProductsJson(rawBody, BuildConfig.ELEGAN_API_BASE_URL)
        productsFlow.value = parsed
        parsed
    }

    suspend fun getProductById(productId: String): Product? {
        if (productId.isBlank()) return null
        val remoteProducts = fetchProductsFromBackend()
        return remoteProducts.find { it.id == productId }
    }
}

internal fun parseBackendProductsJson(
    rawJson: String,
    baseUrl: String = BuildConfig.ELEGAN_API_BASE_URL
): List<Product> {
    val trimmed = rawJson.trim()
    if (trimmed.isEmpty()) return emptyList()

    val root = JSONTokener(trimmed).nextValue()
    val itemsArray: JSONArray = when (root) {
        is JSONArray -> root
        is JSONObject -> {
            root.optJSONArray("items")
                ?: root.optJSONArray("products")
                ?: root.optJSONArray("data")
                ?: JSONArray()
        }
        else -> JSONArray()
    }

    return buildList {
        for (i in 0 until itemsArray.length()) {
            val obj = itemsArray.optJSONObject(i) ?: continue
            val product = mapJsonObjectToProduct(obj, baseUrl)
            if (product != null) add(product)
        }
    }
}

private fun mapJsonObjectToProduct(obj: JSONObject, baseUrl: String): Product? {
    val id = obj.optString("id", "").ifBlank { obj.optString("_id", "") }.trim()
    val name = obj.optString("name", "").ifBlank { obj.optString("title", "") }.trim()
    if (id.isBlank() || name.isBlank()) return null

    val description = obj.optString("description", "").trim()
    val category = obj.optString("category", "todo").trim().ifBlank { "todo" }
    val price = parseDoubleField(obj, "price")
    val rating = parseDoubleField(obj, "rating", defaultValue = 5.0).let {
        if (it <= 0.0) 5.0 else it
    }
    val reviewCount = obj.optLong("reviewCount", obj.optLong("review_count", 0L)).coerceAtLeast(0L)

    val rawImageUrl = obj.optString("imageUrl", "")
        .ifBlank { obj.optString("image_url", "") }
        .ifBlank { obj.optString("image", "") }
        .ifBlank { obj.optString("coverImage", "") }
        .let { resolveBackendMediaUrl(it, baseUrl) }

    val rawImages = (parseStringList(obj.optJSONArray("images")) +
        parseStringList(obj.optJSONArray("gallery")))
        .map { resolveBackendMediaUrl(it, baseUrl) }
        .filter { it.isNotBlank() }
        .distinct()

    val effectiveImageUrl = rawImageUrl.ifBlank {
        rawImages.firstOrNull { !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
    }
    val effectiveImages = if (effectiveImageUrl.isNotBlank()) {
        (listOf(effectiveImageUrl) + rawImages).distinct()
    } else {
        rawImages
    }

    val rawVideoUrl = obj.optString("videoUrl", "")
        .ifBlank { obj.optString("video_url", "") }
        .ifBlank { obj.optString("video", "") }
        .let { resolveBackendMediaUrl(it, baseUrl) }

    val isVideo = parseBooleanField(obj, "isVideo", "is_video", defaultValue = false) ||
        rawVideoUrl.isNotBlank()

    val inStock = parseBooleanField(obj, "inStock", "in_stock", defaultValue = true)
    val isFeatured = parseBooleanField(obj, "isFeatured", "is_featured", "featured", defaultValue = false)
    val mediaFormat = obj.optString("mediaFormat", obj.optString("media_format", "1:1"))
        .trim()
        .ifBlank { "1:1" }

    val sizes = parseStringList(obj.optJSONArray("sizes"))
    val colors = parseStringList(obj.optJSONArray("colors"))
    val createdAt = obj.optString("createdAt", obj.optString("created_at", ""))
        .trim()
        .takeIf { it.isNotBlank() }

    return Product(
        id = id,
        name = name,
        description = description,
        category = category,
        price = price,
        imageUrl = effectiveImageUrl,
        images = effectiveImages,
        colors = colors,
        rating = rating,
        reviewCount = reviewCount,
        sizes = sizes,
        inStock = inStock,
        isFeatured = isFeatured,
        mediaFormat = mediaFormat,
        videoUrl = rawVideoUrl,
        isVideo = isVideo,
        createdAt = createdAt
    )
}

private fun parseDoubleField(obj: JSONObject, key: String, defaultValue: Double = 0.0): Double {
    if (!obj.has(key) || obj.isNull(key)) return defaultValue
    val raw = obj.opt(key)
    return when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDoubleOrNull() ?: defaultValue
        else -> defaultValue
    }
}

private fun parseBooleanField(obj: JSONObject, vararg keys: String, defaultValue: Boolean): Boolean {
    for (key in keys) {
        if (!obj.has(key) || obj.isNull(key)) continue
        return when (val raw = obj.opt(key)) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> defaultValue
        }
    }
    return defaultValue
}

private fun parseStringList(array: JSONArray?): List<String> {
    if (array == null) return emptyList()
    return buildList {
        for (i in 0 until array.length()) {
            when (val item = array.opt(i)) {
                is String -> item.trim().takeIf { it.isNotBlank() }?.let(::add)
                is JSONObject -> {
                    val url = item.optString("url", "")
                        .ifBlank { item.optString("imageUrl", "") }
                        .ifBlank { item.optString("name", "") }
                        .trim()
                    if (url.isNotBlank()) add(url)
                }
            }
        }
    }
}

private fun resolveBackendMediaUrl(rawUrl: String, baseUrl: String): String {
    val clean = rawUrl.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
        return clean
    }
    if (clean.startsWith("//")) return "https:$clean"
    val cleanBase = baseUrl.trimEnd('/')
    return if (clean.startsWith("/")) "$cleanBase$clean" else "$cleanBase/$clean"
}
