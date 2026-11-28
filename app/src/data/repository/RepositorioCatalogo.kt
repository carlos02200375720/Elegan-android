package com.example.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.data.error.ApiParsingException
import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.local.EleganDatabase
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.Product
import com.example.data.model.toProduct
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioCatalogo(
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val databaseProvider: (() -> EleganDatabase)? = null
) {

    private val fetchMutex = Mutex()
    private val cacheLock = Any()
    private val boundedCache = LinkedHashMap<String, Product>(64)
    private val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val isLoadingFlow = MutableStateFlow(false)
    private val apiErrorFlow = MutableStateFlow<ErrorApi?>(null)

    fun observeProducts(): Flow<List<Product>> {
        val dbProvider = databaseProvider
        if (dbProvider != null) {
            return flow {
                emitAll(
                    dbProvider().productDao()
                        .observeAllProducts()
                        .map { entities -> entities.map { it.toDomain() } }
                )
            }.flowOn(Dispatchers.IO)
        }
        return productsFlow.asStateFlow()
    }

    fun observeLoading(): Flow<Boolean> = isLoadingFlow.asStateFlow()

    fun observeApiError(): Flow<ErrorApi?> = apiErrorFlow.asStateFlow()

    fun observeError(): Flow<String?> = apiErrorFlow.map { it?.userMessage }

    /**
     * Crea un flujo paginado de Paging 3 con ventana deslizante en memoria ([maxSize]).
     * Cuando existe base de datos local Room ([databaseProvider]), Room actúa como fuente única
     * de verdad y [ProductosRemoteMediator] sincroniza en segundo plano manteniendo soporte offline.
     */
    @OptIn(ExperimentalPagingApi::class)
    fun observePagedProducts(
        category: String? = null,
        query: String? = null,
        inStockOnly: Boolean = true,
        pageSize: Int = ProductosPagingSource.DEFAULT_PAGE_SIZE,
        prefetchDistance: Int = ProductosPagingSource.DEFAULT_PREFETCH_DISTANCE,
        maxSize: Int = ProductosPagingSource.DEFAULT_MAX_MEMORY_ITEMS
    ): Flow<PagingData<Product>> {
        val safePageSize = pageSize.coerceIn(5, ProductosPagingSource.MAX_PAGE_SIZE)
        val safePrefetch = prefetchDistance.coerceAtLeast(1)
        val minAllowedMaxSize = safePageSize + (2 * safePrefetch)
        val safeMaxSize = maxSize.coerceAtLeast(minAllowedMaxSize)
        val cleanCategory = category?.trim()?.takeIf { it.isNotBlank() }
        val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }

        val dbProvider = databaseProvider
        if (dbProvider != null) {
            val queryPattern = cleanQuery?.let { "%${it.lowercase()}%" }
            return Pager(
                config = PagingConfig(
                    pageSize = safePageSize,
                    prefetchDistance = safePrefetch,
                    initialLoadSize = safePageSize,
                    maxSize = safeMaxSize,
                    enablePlaceholders = false
                ),
                remoteMediator = ProductosRemoteMediator(
                    databaseProvider = dbProvider,
                    fetchPage = { page, limit, cat, q, stockOnly ->
                        fetchProductsPage(
                            page = page,
                            limit = limit,
                            category = cat,
                            query = q,
                            inStockOnly = stockOnly
                        )
                    },
                    category = cleanCategory,
                    query = cleanQuery,
                    inStockOnly = inStockOnly,
                    onPageSynced = ::registrarPaginaEnCacheAcotada
                ),
                pagingSourceFactory = {
                    dbProvider().productDao().pagingSource(
                        categoryId = cleanCategory,
                        queryPattern = queryPattern,
                        inStockOnly = inStockOnly
                    )
                }
            ).flow.map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
        }

        return Pager(
            config = PagingConfig(
                pageSize = safePageSize,
                prefetchDistance = safePrefetch,
                initialLoadSize = safePageSize,
                maxSize = safeMaxSize,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ProductosPagingSource(
                    fetchPage = { page, limit, cat, q, stockOnly ->
                        fetchProductsPage(
                            page = page,
                            limit = limit,
                            category = cat,
                            query = q,
                            inStockOnly = stockOnly
                        )
                    },
                    category = cleanCategory,
                    query = cleanQuery,
                    inStockOnly = inStockOnly,
                    onPageLoaded = ::registrarPaginaEnCacheAcotada
                )
            }
        ).flow
    }

    suspend fun fetchProductsPageResult(
        page: Int,
        limit: Int = ProductosPagingSource.DEFAULT_PAGE_SIZE,
        category: String? = null,
        query: String? = null,
        inStockOnly: Boolean = true
    ): ResultWrapper<PaginaProductos> {
        val safePage = page.coerceAtLeast(1)
        val safeLimit = limit.coerceIn(1, ProductosPagingSource.MAX_PAGE_SIZE)
        val cleanCategory = category?.trim()?.takeIf { it.isNotBlank() }
        val cleanQuery = query?.trim()?.takeIf { it.isNotBlank() }

        return safeApiCall(
            tag = "EleganCatalog",
            operation = "fetchProductsPage",
            extras = mapOf(
                "page" to safePage.toString(),
                "limit" to safeLimit.toString(),
                "category" to (cleanCategory ?: "all")
            )
        ) {
            val response = apiProvider().productsPage(
                page = safePage,
                limit = safeLimit,
                category = cleanCategory,
                query = cleanQuery,
                inStock = if (inStockOnly) true else null
            )
            extractPaginaProductos(
                response = response,
                requestedPage = safePage,
                requestedLimit = safeLimit
            )
        }
    }

    suspend fun fetchProductsPage(
        page: Int,
        limit: Int = ProductosPagingSource.DEFAULT_PAGE_SIZE,
        category: String? = null,
        query: String? = null,
        inStockOnly: Boolean = true
    ): PaginaProductos = fetchProductsPageResult(
        page = page,
        limit = limit,
        category = category,
        query = query,
        inStockOnly = inStockOnly
    ).getOrThrow()

    suspend fun refreshProductsResult(): ResultWrapper<List<Product>> = fetchMutex.withLock {
        isLoadingFlow.value = true
        apiErrorFlow.value = null
        try {
            val result = safeApiCall(
                tag = "EleganCatalog",
                operation = "refreshProducts"
            ) {
                fetchProductsFromBackend()
            }
            when (result) {
                is ResultWrapper.Success -> apiErrorFlow.value = null
                is ResultWrapper.Error -> apiErrorFlow.value = result.error
                ResultWrapper.Loading -> Unit
            }
            result
        } finally {
            isLoadingFlow.value = false
        }
    }

    suspend fun refreshProducts(): List<Product> =
        refreshProductsResult().getOrElse { obtenerProductosGuardadosLocalmente() }

    suspend fun fetchProductsFromBackend(): List<Product> = withContext(Dispatchers.IO) {
        val response = apiProvider().products()
        val hasExpectedListKey = response["items"] is List<*> ||
            response["products"] is List<*> ||
            response["data"] is List<*>
        if (!hasExpectedListKey) {
            throw ApiParsingException(
                message = "El catálogo recibido del servidor no tiene una lista válida de productos.",
                fieldOrContext = "products.items"
            )
        }
        val items = extractJsonItems(response)
        reemplazarCacheAcotada(items)
        persistirCatalogoEnRoom(items)
        items
    }

    suspend fun getRecommendedProductsResult(
        productId: String,
        limit: Int = 12
    ): ResultWrapper<List<Product>> {
        if (productId.isBlank()) return ResultWrapper.Success(emptyList())
        return safeApiCall(
            tag = "EleganCatalog",
            operation = "getRecommendedProducts",
            extras = mapOf("productId" to productId)
        ) {
            val response = apiProvider().recommendedProducts(productId, limit)
            extractJsonItems(response)
        }
    }

    suspend fun getRecommendedProducts(productId: String, limit: Int = 12): List<Product> =
        getRecommendedProductsResult(productId, limit).getOrThrow()

    suspend fun getProductByIdResult(productId: String): ResultWrapper<Product> {
        if (productId.isBlank()) {
            return ResultWrapper.Error(
                ErrorApi.ClientError(
                    httpCode = 400,
                    userMessage = "Identificador de producto inválido."
                )
            )
        }
        return safeApiCall(
            tag = "EleganCatalog",
            operation = "getProductById",
            extras = mapOf("productId" to productId)
        ) {
            val response = apiProvider().productById(productId)
            val candidate = (response["item"] as? Map<*, *>)
                ?: (response["product"] as? Map<*, *>)
                ?: (response["data"] as? Map<*, *>)
                ?: response
            val parsed = candidate.toProduct()
            if (parsed.id.isBlank() || parsed.name.isBlank()) {
                throw ApiParsingException(
                    message = "El detalle del producto recibido del servidor está incompleto.",
                    fieldOrContext = "productById:$productId"
                )
            }
            registrarPaginaEnCacheAcotada(listOf(parsed))
            persistirProductoIndividualEnRoom(parsed)
            parsed
        }
    }

    suspend fun getProductById(productId: String): Product? = withContext(Dispatchers.IO) {
        if (productId.isBlank()) return@withContext null
        when (val result = getProductByIdResult(productId)) {
            is ResultWrapper.Success -> result.data
            is ResultWrapper.Error, ResultWrapper.Loading -> {
                synchronized(cacheLock) { boundedCache[productId] }
                    ?: productsFlow.value.find { it.id == productId }
                    ?: buscarProductoEnRoom(productId)
            }
        }
    }

    private suspend fun persistirCatalogoEnRoom(items: List<Product>) {
        val db = databaseProvider?.invoke() ?: return
        val nowMs = System.currentTimeMillis()
        val entities = items.mapIndexedNotNull { index, product ->
            if (product.id.isBlank()) return@mapIndexedNotNull null
            product.toEntity(sortOrder = index, syncedAtMs = nowMs)
        }
        db.withTransaction {
            db.productDao().deleteAll()
            db.productRemoteKeyDao().clearAll()
            if (entities.isNotEmpty()) {
                db.productDao().upsertAll(entities)
            }
        }
    }

    private suspend fun persistirProductoIndividualEnRoom(product: Product) {
        val db = databaseProvider?.invoke() ?: return
        if (product.id.isBlank()) return
        val dao = db.productDao()
        val existingOrder = dao.getSortOrder(product.id) ?: DETAIL_ONLY_SORT_ORDER
        dao.upsert(product.toEntity(sortOrder = existingOrder, syncedAtMs = System.currentTimeMillis()))
    }

    private suspend fun buscarProductoEnRoom(productId: String): Product? {
        val db = databaseProvider?.invoke() ?: return null
        val dao = db.productDao()
        return (dao.getById(productId) ?: dao.getByIdIgnoreCase(productId))?.toDomain()
    }

    private suspend fun obtenerProductosGuardadosLocalmente(): List<Product> {
        val db = databaseProvider?.invoke() ?: return productsFlow.value
        return withContext(Dispatchers.IO) {
            db.productDao().getAllProducts().map { it.toDomain() }
        }
    }

    private fun registrarPaginaEnCacheAcotada(items: List<Product>) {
        if (items.isEmpty()) return
        val snapshot = synchronized(cacheLock) {
            for (product in items) {
                if (product.id.isBlank()) continue
                // Reinsertar al final para mantener comportamiento LRU acotado
                boundedCache.remove(product.id)
                boundedCache[product.id] = product
            }
            while (boundedCache.size > MAX_BOUNDED_PRODUCTS_CACHE) {
                val iterator = boundedCache.entries.iterator()
                if (iterator.hasNext()) {
                    iterator.next()
                    iterator.remove()
                } else {
                    break
                }
            }
            boundedCache.values.toList()
        }
        productsFlow.value = snapshot
    }

    private fun reemplazarCacheAcotada(items: List<Product>) {
        val snapshot = synchronized(cacheLock) {
            boundedCache.clear()
            for (product in items) {
                if (product.id.isBlank()) continue
                boundedCache[product.id] = product
                if (boundedCache.size >= MAX_BOUNDED_PRODUCTS_CACHE) break
            }
            boundedCache.values.toList()
        }
        productsFlow.value = snapshot
    }

    companion object {
        private const val MAX_BOUNDED_PRODUCTS_CACHE = 250
        private const val DETAIL_ONLY_SORT_ORDER = 500_000
    }
}

internal fun extractJsonItems(response: Map<String, Any?>): List<Product> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["products"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val parsed = map.toProduct()
        parsed.takeIf { it.id.isNotBlank() && it.name.isNotBlank() }
    }
}
