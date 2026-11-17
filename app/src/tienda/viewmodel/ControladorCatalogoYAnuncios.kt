package com.example.ui.viewmodel

import com.example.data.model.AnnouncementJson
import com.example.data.model.ProductJson
import com.example.data.model.productId
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorCatalogoYAnuncios(
    private val repository: EleganRepository,
    private val scope: CoroutineScope
) {
    private val _selectedCategory = MutableStateFlow("todo")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductJson?>(null)
    val selectedProduct: StateFlow<ProductJson?> = _selectedProduct.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val products: StateFlow<List<ProductJson>> = repository.observeProducts()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val announcements: StateFlow<List<AnnouncementJson>> = repository.observeAnnouncements()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val isLoading: StateFlow<Boolean> = repository.observeProductsLoading()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val error: StateFlow<String?> = repository.observeProductsError()
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun refreshProducts() {
        scope.launch {
            repository.refreshProducts()
        }
    }

    fun refreshAnnouncements() {
        scope.launch {
            repository.refreshAnnouncements()
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: ProductJson) {
        _selectedProduct.value = product
        val productId = product.productId()
        if (productId.isNotBlank()) {
            scope.launch {
                try {
                    val fresh = repository.getProductById(productId)
                    if (fresh != null && _selectedProduct.value?.productId() == productId) {
                        _selectedProduct.value = fresh
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                }
            }
        }
    }

    fun clearSelection() {
        _selectedProduct.value = null
    }
}
