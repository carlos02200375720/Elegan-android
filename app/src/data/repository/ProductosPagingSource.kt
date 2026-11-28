package com.example.data.repository

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.data.model.Product
import com.example.data.model.productCategory
import com.example.data.model.productDescription
import com.example.data.model.productId
import com.example.data.model.productInStock
import com.example.data.model.productName
import com.example.data.observability.MonitorErrores
import kotlinx.coroutines.CancellationException

internal data class PaginaProductos(
    val items: List<Product>,
    val page: Int,
    val nextPage: Int?,
    val totalItems: Int? = null,
    val serverPaginated: Boolean = true
)

/**
 * Parsea la respuesta paginada de `GET api/movil/products` soportando metadatos en raíz o bajo
 * `pagination` / `meta` (`hasMore`, `hasNextPage`, `nextPage`, `totalPages`, `total`) y detectando
 * de forma segura respuestas legadas sin paginación para evitar bucles o claves duplicadas.
 */
internal fun extractPaginaProductos(
    response: Map<String, Any?>,
    requestedPage: Int,
    requestedLimit: Int
): PaginaProductos {
    val items = extractJsonItems(response)
    val meta = (response["pagination"] as? Map<*, *>)
        ?: (response["meta"] as? Map<*, *>)
        ?: response

    val currentPage = extractIntField(meta, "page", "currentPage", "current_page")
        ?: extractIntField(response, "page", "currentPage", "current_page")
        ?: requestedPage
    val totalPages = extractIntField(meta, "totalPages", "total_pages", "pages")
        ?: extractIntField(response, "totalPages", "total_pages", "pages")
    val totalItems = extractIntField(meta, "total", "totalItems", "total_items", "totalCount", "count")
        ?: extractIntField(response, "total", "totalItems", "total_items", "totalCount", "count")
    val explicitNextPage = extractIntField(meta, "nextPage", "next_page")
        ?: extractIntField(response, "nextPage", "next_page")
    val hasMore = extractBooleanField(meta, "hasMore", "has_more", "hasNextPage", "has_next_page")
        ?: extractBooleanField(response, "hasMore", "has_more", "hasNextPage", "has_next_page")
    val limitFromMeta = extractIntField(meta, "limit", "pageSize", "page_size", "perPage", "per_page")
        ?: extractIntField(response, "limit", "pageSize", "page_size", "perPage", "per_page")

    val hasExplicitPagination =
        hasMore != null ||
            explicitNextPage != null ||
            totalPages != null ||
            limitFromMeta != null ||
            response["pagination"] is Map<*, *> ||
            response["meta"] is Map<*, *>

    val safeLimit = (limitFromMeta ?: requestedLimit).coerceAtLeast(1)

    // Si el servidor aún no implementa `limit` y devuelve todo el catálogo en la página 1,
    // cerramos la paginación en esa página para no volver a pedir todo y duplicar claves.
    val looksLikeLegacyFullDump = !hasExplicitPagination && requestedPage == 1 && items.size > safeLimit

    val nextPage: Int? = when {
        items.isEmpty() -> null
        looksLikeLegacyFullDump -> null
        explicitNextPage != null -> explicitNextPage.takeIf { it > currentPage }
        hasMore != null -> if (hasMore) currentPage + 1 else null
        totalPages != null -> if (currentPage < totalPages) currentPage + 1 else null
        totalItems != null -> if (currentPage * safeLimit < totalItems) currentPage + 1 else null
        items.size >= safeLimit -> currentPage + 1
        else -> null
    }

    return PaginaProductos(
        items = items,
        page = currentPage,
        nextPage = nextPage,
        totalItems = totalItems,
        serverPaginated = hasExplicitPagination || !looksLikeLegacyFullDump
    )
}

/**
 * Fuente de paginación de Paging 3 para el catálogo móvil de Elegan.
 *
 * - Carga páginas acotadas bajo demanda (`page` / `limit`) sin retener miles de productos en RAM.
 * - Deduplica IDs entre páginas para resistir inserciones concurrentes sin romper claves de Compose.
 * - Si un backend legado devuelve todo el catálogo sin filtrar, aplica el filtro en la página
 *   sin solicitar páginas redundantes.
 */
internal class ProductosPagingSource(
    private val fetchPage: suspend (
        page: Int,
        limit: Int,
        category: String?,
        query: String?,
        inStockOnly: Boolean
    ) -> PaginaProductos,
    private val category: String? = null,
    private val query: String? = null,
    private val inStockOnly: Boolean = true,
    private val onPageLoaded: (List<Product>) -> Unit = {}
) : PagingSource<Int, Product>() {

    private val seenIds = HashSet<String>()

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Product> {
        val page = (params.key ?: 1).coerceAtLeast(1)
        val limit = params.loadSize.coerceIn(1, MAX_PAGE_SIZE)
        val cleanCategory = category?.trim()?.takeIf { it.isNotBlank() }
        val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }

        return try {
            val pagina = fetchPage(page, limit, cleanCategory, cleanQuery, inStockOnly)
            if (page == 1) {
                seenIds.clear()
            }

            val filtered = filtrarPagina(
                items = pagina.items,
                category = cleanCategory,
                query = cleanQuery,
                inStockOnly = inStockOnly,
                enforceLocalCategoryAndQuery = !pagina.serverPaginated
            )

            val uniqueItems = ArrayList<Product>(filtered.size)
            for (product in filtered) {
                val id = product.productId()
                if (id.isNotBlank() && seenIds.add(id)) {
                    uniqueItems.add(product)
                }
            }

            if (uniqueItems.isNotEmpty()) {
                onPageLoaded(uniqueItems)
            }

            // Si un servidor legado devolvió los mismos elementos repetidos en página > 1, detenemos la paginación.
            val effectiveNextKey = if (pagina.items.isNotEmpty() && uniqueItems.isEmpty() && !pagina.serverPaginated) {
                null
            } else {
                pagina.nextPage
            }

            LoadResult.Page(
                data = uniqueItems,
                prevKey = if (page <= 1) null else page - 1,
                nextKey = effectiveNextKey
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarError(
                tag = "EleganCatalogPaging",
                mensaje = "Error al cargar la página $page del catálogo.",
                error = e,
                extras = mapOf(
                    "page" to page.toString(),
                    "limit" to limit.toString(),
                    "category" to (cleanCategory ?: "all")
                )
            )
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Product>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null
        return anchorPage.prevKey?.plus(1) ?: anchorPage.nextKey?.minus(1)
    }

    private fun filtrarPagina(
        items: List<Product>,
        category: String?,
        query: String?,
        inStockOnly: Boolean,
        enforceLocalCategoryAndQuery: Boolean
    ): List<Product> {
        if (items.isEmpty()) return emptyList()
        return items.filter { prod ->
            if (inStockOnly && !prod.productInStock()) return@filter false
            if (enforceLocalCategoryAndQuery) {
                if (category != null && prod.productCategory() != category) return@filter false
                if (query != null) {
                    val matches = prod.productName().contains(query, ignoreCase = true) ||
                        prod.productDescription().contains(query, ignoreCase = true) ||
                        prod.categoryName.contains(query, ignoreCase = true)
                    if (!matches) return@filter false
                }
            }
            true
        }
    }

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val DEFAULT_PREFETCH_DISTANCE = 6
        const val DEFAULT_MAX_MEMORY_ITEMS = 120
        const val MAX_PAGE_SIZE = 50
    }
}

private fun extractIntField(source: Map<*, *>, vararg keys: String): Int? {
    for (key in keys) {
        val raw = source[key] ?: continue
        when (raw) {
            is Number -> return raw.toInt()
            is String -> raw.trim().toIntOrNull()?.let { return it }
        }
    }
    return null
}

private fun extractBooleanField(source: Map<*, *>, vararg keys: String): Boolean? {
    for (key in keys) {
        val raw = source[key] ?: continue
        when (raw) {
            is Boolean -> return raw
            is Number -> return raw.toInt() != 0
            is String -> when (raw.trim().lowercase()) {
                "true", "1", "yes" -> return true
                "false", "0", "no" -> return false
            }
        }
    }
    return null
}
