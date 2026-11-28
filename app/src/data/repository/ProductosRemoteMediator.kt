package com.example.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.data.local.EleganDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ProductRemoteKeyEntity
import com.example.data.local.toEntity
import com.example.data.model.Product
import com.example.data.model.productCategory
import com.example.data.model.productDescription
import com.example.data.model.productId
import com.example.data.model.productInStock
import com.example.data.model.productName
import com.example.data.observability.MonitorErrores
import kotlinx.coroutines.CancellationException

/**
 * Mediador de Paging 3 respaldado por Room (estrategia offline-first / Single Source of Truth).
 *
 * - Room sirve inmediatamente los productos guardados localmente, incluso sin conexión.
 * - En paralelo sincroniza páginas desde el backend y actualiza Room de forma atómica con [withTransaction].
 * - Si la red falla, nunca borra la caché local: el usuario sigue navegando el catálogo persistido.
 */
@OptIn(ExperimentalPagingApi::class)
internal class ProductosRemoteMediator(
    private val databaseProvider: () -> EleganDatabase,
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
    private val onPageSynced: (List<Product>) -> Unit = {},
    private val clockMs: () -> Long = { System.currentTimeMillis() }
) : RemoteMediator<Int, ProductEntity>() {

    private val cleanCategory: String? = category?.trim()?.takeIf { it.isNotBlank() }
    private val cleanQuery: String? = query?.trim()?.takeIf { it.isNotBlank() }
    private val queryPattern: String? = cleanQuery?.let { "%${it.lowercase()}%" }
    private val isUnfilteredCatalog: Boolean = cleanCategory == null && cleanQuery == null
    private val queryKey: String = buildQueryKey(cleanCategory, cleanQuery, inStockOnly)

    override suspend fun initialize(): InitializeAction {
        // LAUNCH_INITIAL_REFRESH muestra de inmediato el contenido persistido en Room
        // mientras dispara la sincronización de la página 1 en segundo plano.
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, ProductEntity>
    ): MediatorResult {
        val db = databaseProvider()
        val productDao = db.productDao()
        val remoteKeyDao = db.productRemoteKeyDao()

        val page = when (loadType) {
            LoadType.REFRESH -> 1
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)
            LoadType.APPEND -> {
                val remoteKey = remoteKeyDao.getByQueryKey(queryKey)
                val nextPage = remoteKey?.nextPage
                    ?: return MediatorResult.Success(endOfPaginationReached = true)
                nextPage
            }
        }

        val requestedLimit = when (loadType) {
            LoadType.REFRESH -> state.config.initialLoadSize
            else -> state.config.pageSize
        }.coerceIn(1, ProductosPagingSource.MAX_PAGE_SIZE)

        return try {
            val pagina = fetchPage(page, requestedLimit, cleanCategory, cleanQuery, inStockOnly)
            val filtered = filtrarPaginaRemota(
                items = pagina.items,
                category = cleanCategory,
                query = cleanQuery,
                inStockOnly = inStockOnly,
                enforceLocalCategoryAndQuery = !pagina.serverPaginated
            )

            val nowMs = clockMs()
            val baseOrder = if (isUnfilteredCatalog) {
                ((page - 1) * state.config.pageSize).coerceAtLeast(0)
            } else {
                FILTERED_SORT_ORDER_OFFSET + ((page - 1) * state.config.pageSize).coerceAtLeast(0)
            }

            db.withTransaction {
                if (loadType == LoadType.REFRESH && isUnfilteredCatalog) {
                    productDao.deleteAll()
                    remoteKeyDao.clearAll()
                }

                val entities = ArrayList<ProductEntity>(filtered.size)
                filtered.forEachIndexed { index, product ->
                    if (product.productId().isBlank()) return@forEachIndexed
                    val resolvedSortOrder = if (isUnfilteredCatalog) {
                        baseOrder + index
                    } else {
                        productDao.getSortOrder(product.productId()) ?: (baseOrder + index)
                    }
                    entities.add(product.toEntity(sortOrder = resolvedSortOrder, syncedAtMs = nowMs))
                }

                if (entities.isNotEmpty()) {
                    productDao.upsertAll(entities)
                }

                remoteKeyDao.upsert(
                    ProductRemoteKeyEntity(
                        queryKey = queryKey,
                        nextPage = pagina.nextPage,
                        lastUpdatedMs = nowMs,
                        serverPaginated = pagina.serverPaginated
                    )
                )
            }

            if (filtered.isNotEmpty()) {
                onPageSynced(filtered)
            }

            MediatorResult.Success(
                endOfPaginationReached = pagina.nextPage == null || pagina.items.isEmpty()
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val cachedCount = runCatching {
                productDao.countMatching(
                    categoryId = cleanCategory,
                    queryPattern = queryPattern,
                    inStockOnly = inStockOnly
                )
            }.getOrDefault(0)

            if (cachedCount > 0) {
                MonitorErrores.registrarAdvertencia(
                    tag = "EleganCatalogOffline",
                    mensaje = "Sin conexión al sincronizar página $page; mostrando $cachedCount prendas guardadas en Room.",
                    error = e
                )
            } else {
                MonitorErrores.registrarError(
                    tag = "EleganCatalogPaging",
                    mensaje = "Error al sincronizar la página $page del catálogo sin caché local disponible.",
                    error = e,
                    extras = mapOf(
                        "page" to page.toString(),
                        "limit" to requestedLimit.toString(),
                        "category" to (cleanCategory ?: "all")
                    )
                )
            }
            MediatorResult.Error(e)
        }
    }

    private fun filtrarPaginaRemota(
        items: List<Product>,
        category: String?,
        query: String?,
        inStockOnly: Boolean,
        enforceLocalCategoryAndQuery: Boolean
    ): List<Product> {
        if (items.isEmpty()) return emptyList()
        return items.filter { prod ->
            if (prod.productId().isBlank()) return@filter false
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
        private const val FILTERED_SORT_ORDER_OFFSET = 100_000

        fun buildQueryKey(
            category: String?,
            query: String?,
            inStockOnly: Boolean
        ): String {
            val normalizedCat = category?.trim().orEmpty()
            val normalizedQuery = query?.trim()?.lowercase().orEmpty()
            val stockFlag = if (inStockOnly) "1" else "0"
            return "stock=$stockFlag|cat=$normalizedCat|q=$normalizedQuery"
        }
    }
}
