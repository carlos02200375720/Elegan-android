package com.example.data.repository

import android.util.Log
import com.example.data.model.CategoryItem
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Fuente única de verdad para categorías en Android, alimentada exclusivamente por
 * `GET /api/movil/categories`. Conserva la última lista válida en memoria entre
 * navegaciones y nunca vacía las categorías mientras consulta o ante errores.
 */
internal class RepositorioCategorias(
    private val fetchRemotePayload: suspend () -> Any? = { fetchCategoriesPayloadFromBackend() }
) {

    private val fetchMutex = Mutex()
    private val categoriesFlow = MutableStateFlow<List<CategoryItem>>(emptyList())

    @Volatile
    private var hasLoadedSuccessfully = false

    fun observeCategories(): Flow<List<CategoryItem>> = categoriesFlow.asStateFlow()

    suspend fun loadCategoriesOnce(): List<CategoryItem> = fetchInternal(force = false)

    suspend fun refreshCategories(): List<CategoryItem> = fetchInternal(force = true)

    private suspend fun fetchInternal(force: Boolean): List<CategoryItem> = fetchMutex.withLock {
        if (!force && hasLoadedSuccessfully) {
            return@withLock categoriesFlow.value
        }
        try {
            val payload = fetchRemotePayload()
            val parsed = extractCategoriesJson(payload)
            if (parsed != null) {
                categoriesFlow.value = parsed
                hasLoadedSuccessfully = true
            }
            categoriesFlow.value
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("EleganCategories", "No se pudieron obtener las categorías del backend.", e)
            categoriesFlow.value
        }
    }
}

private suspend fun fetchCategoriesPayloadFromBackend(): Any = withContext(Dispatchers.IO) {
    ApiAutenticacionMovil.service.categories()
}

internal fun extractCategoriesJson(rawPayload: Any?): List<CategoryItem>? {
    if (rawPayload == null) return null

    val rawList: List<*> = when (rawPayload) {
        is List<*> -> rawPayload
        is Map<*, *> -> {
            val nestedData = rawPayload["data"] as? Map<*, *>
            (rawPayload["items"] as? List<*>)
                ?: (rawPayload["categories"] as? List<*>)
                ?: (rawPayload["data"] as? List<*>)
                ?: (rawPayload["results"] as? List<*>)
                ?: (nestedData?.get("items") as? List<*>)
                ?: (nestedData?.get("categories") as? List<*>)
                ?: return null
        }
        else -> return null
    }

    val parsed = rawList.mapIndexedNotNull { index, entry ->
        when (entry) {
            is String -> {
                val clean = entry.trim()
                if (clean.isBlank()) {
                    null
                } else {
                    CategoryItem(
                        id = clean,
                        title = clean.replaceFirstChar { it.uppercase() },
                        sortOrder = index
                    )
                }
            }
            is Map<*, *> -> parseCategoryMap(entry, index)
            else -> null
        }
    }

    return parsed
        .sortedBy { it.sortOrder }
        .distinctBy { it.id }
}

private fun parseCategoryMap(map: Map<*, *>, fallbackIndex: Int): CategoryItem? {
    val isActive = readCategoryBoolean(
        map = map,
        keys = arrayOf("active", "isActive", "is_active", "enabled", "published"),
        defaultValue = true
    )
    if (!isActive) return null

    val rawId = firstNonBlankCategoryString(
        map,
        "id", "slug", "key", "code", "_id", "categoryId", "category_id"
    )
    if (rawId.isBlank()) return null

    val rawTitle = firstNonBlankCategoryString(
        map,
        "title", "name", "label", "nombre"
    )

    val resolvedTitle = rawTitle.ifBlank {
        rawId.replaceFirstChar { it.uppercase() }
    }
    val subtitle = firstNonBlankCategoryString(
        map,
        "subtitle", "description", "summary", "descripcion"
    )
    val imageUrl = firstNonBlankCategoryString(
        map,
        "imageUrl", "image_url", "image", "coverImage", "cover_image", "thumbnail"
    ).let(::resolveBackendMediaUrl)
    val iconName = firstNonBlankCategoryString(
        map,
        "iconName", "icon_name", "icon"
    )
    val sortOrder = readCategoryOrder(
        map = map,
        keys = arrayOf("sortOrder", "sort_order", "order", "position"),
        fallback = fallbackIndex
    )

    return CategoryItem(
        id = rawId,
        title = resolvedTitle,
        subtitle = subtitle,
        imageUrl = imageUrl,
        iconName = iconName,
        sortOrder = sortOrder
    )
}

private fun firstNonBlankCategoryString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val value = (map[key] as? String)?.trim()
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

private fun readCategoryBoolean(
    map: Map<*, *>,
    keys: Array<String>,
    defaultValue: Boolean
): Boolean {
    for (key in keys) {
        if (!map.containsKey(key) || map[key] == null) continue
        return when (val raw = map[key]) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> defaultValue
        }
    }
    return defaultValue
}

private fun readCategoryOrder(
    map: Map<*, *>,
    keys: Array<String>,
    fallback: Int
): Int {
    for (key in keys) {
        val raw = map[key] ?: continue
        when (raw) {
            is Number -> return raw.toInt()
            is String -> raw.trim().toIntOrNull()?.let { return it }
        }
    }
    return fallback
}
