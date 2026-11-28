package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.data.error.ErrorApi
import com.example.data.model.Announcement
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.productId
import com.example.data.model.productInStock
import com.example.data.observability.MonitorErrores
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel dedicado a los flujos de Inicio, Catálogo, Categorías y Detalle de Producto.
 * Usa Paging 3 ([pagedFeedProducts] y [pagedCatalogProducts]) para cargar miles de productos
 * de forma paginada sin mantener todo el catálogo en RAM.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogoViewModel(
    private val repository: EleganRepository,
    autoLoad: Boolean = true,
    private val enablePaging: Boolean = autoLoad,
    private val computationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val searchDebounceMs: Long = 220L
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _recommendedProducts = MutableStateFlow<List<Product>>(emptyList())
    val recommendedProducts: StateFlow<List<Product>> = _recommendedProducts.asStateFlow()
    private var requestedRecommendationsFor: String = ""

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val pagingRefreshTrigger = MutableStateFlow(0L)

    val products: StateFlow<List<Product>> = repository.observeProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val inStockProducts: StateFlow<List<Product>> = products
        .map { list -> list.filter(Product::productInStock) }
        .flowOn(computationDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    /** Flujo paginado de Paging 3 para el feed principal de Inicio (solo productos en stock). */
    val pagedFeedProducts: Flow<PagingData<Product>> = if (enablePaging) {
        pagingRefreshTrigger
            .flatMapLatest {
                repository.observePagedProducts(
                    category = null,
                    query = null,
                    inStockOnly = true
                )
            }
            .cachedIn(viewModelScope)
    } else {
        flowOf(PagingData.empty())
    }

    private val debouncedSearchQuery: Flow<String> = _searchQuery
        .map { it.trim() }
        .debounce { query -> if (query.isEmpty()) 0L else searchDebounceMs }
        .distinctUntilChanged()

    /** Flujo paginado de Paging 3 para el Catálogo filtrado por categoría y búsqueda con debounce. */
    val pagedCatalogProducts: Flow<PagingData<Product>> = if (enablePaging) {
        combine(
            _selectedCategory,
            debouncedSearchQuery,
            pagingRefreshTrigger
        ) { category, query, _ ->
            category to query.ifBlank { null }
        }
            .distinctUntilChanged()
            .flatMapLatest { (category, query) ->
                repository.observePagedProducts(
                    category = category,
                    query = query,
                    inStockOnly = true
                )
            }
            .cachedIn(viewModelScope)
    } else {
        flowOf(PagingData.empty())
    }

    val announcements: StateFlow<List<Announcement>> = repository.observeAnnouncements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val categories: StateFlow<List<CategoryItem>> = repository.observeCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val categoryTitleById: StateFlow<Map<String, String>> = categories
        .map { list -> list.associate { it.id to it.title.trim() } }
        .flowOn(computationDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    val isLoadingProducts: StateFlow<Boolean> = repository.observeProductsLoading()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val productsApiError: StateFlow<ErrorApi?> = repository.observeProductsApiError()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val productsError: StateFlow<String?> = repository.observeProductsError()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    init {
        if (autoLoad) {
            refreshAnnouncements()
            loadCategoriesOnce()
        }
    }

    fun refreshProducts() {
        if (enablePaging) {
            pagingRefreshTrigger.value += 1L
            repository.scheduleBackgroundCatalogSync(force = false)
        } else {
            viewModelScope.launch {
                repository.refreshProducts()
            }
        }
    }

    fun refreshAnnouncements() {
        viewModelScope.launch {
            repository.refreshAnnouncements()
        }
    }

    fun loadCategoriesOnce() {
        viewModelScope.launch {
            repository.loadCategoriesOnce()
        }
    }

    fun refreshCategories() {
        viewModelScope.launch {
            repository.refreshCategories()
        }
    }

    fun refreshSelectedProduct() {
        val productId = _selectedProduct.value?.productId()?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch {
            try {
                val fresh = repository.getProductById(productId)
                if (fresh != null && _selectedProduct.value?.productId() == productId) {
                    _selectedProduct.value = fresh
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    "EleganCatalog",
                    "No se pudo actualizar el producto abierto en tiempo real.",
                    e
                )
            }
        }
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category?.takeIf { it.isNotBlank() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadRecommendedProducts(productId: String) {
        requestedRecommendationsFor = productId
        _recommendedProducts.value = emptyList()
        if (productId.isBlank()) return
        viewModelScope.launch {
            try {
                val result = repository.getRecommendedProducts(productId)
                    .filter { it.productId() != productId }
                if (requestedRecommendationsFor == productId) {
                    _recommendedProducts.value = result
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (requestedRecommendationsFor == productId) {
                    _recommendedProducts.value = emptyList()
                }
                MonitorErrores.registrarAdvertencia(
                    "EleganCatalog",
                    "No se pudieron cargar los productos recomendados desde el backend.",
                    e
                )
            }
        }
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
        val productId = product.productId()
        if (productId.isNotBlank()) {
            viewModelScope.launch {
                try {
                    val fresh = repository.getProductById(productId)
                    if (fresh != null && _selectedProduct.value?.productId() == productId) {
                        _selectedProduct.value = fresh
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    MonitorErrores.registrarAdvertencia(
                        "EleganCatalog",
                        "No se pudo refrescar el detalle del producto $productId.",
                        e
                    )
                }
            }
        }
    }

    fun selectProductById(
        productId: String,
        onFound: () -> Unit,
        onNotFound: () -> Unit = {}
    ) {
        val cleanId = productId.trim()
        if (cleanId.isBlank()) {
            onNotFound()
            return
        }
        val localProduct = products.value.firstOrNull { it.productId() == cleanId }
            ?: products.value.firstOrNull { it.productId().equals(cleanId, ignoreCase = true) }
        if (localProduct != null) {
            selectProduct(localProduct)
            onFound()
            return
        }
        viewModelScope.launch {
            try {
                val remoteProduct = repository.getProductById(cleanId)
                if (remoteProduct != null) {
                    _selectedProduct.value = remoteProduct
                    onFound()
                } else {
                    onNotFound()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    "EleganCatalog",
                    "No se pudo cargar el producto $cleanId por identificador.",
                    e
                )
                onNotFound()
            }
        }
    }

    fun clearSelection() {
        _selectedProduct.value = null
        _recommendedProducts.value = emptyList()
    }
}
