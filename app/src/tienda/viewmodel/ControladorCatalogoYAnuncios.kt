package com.example.ui.viewmodel

import com.example.data.model.ProductJson
import com.example.data.repository.EleganRepository
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

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: ProductJson) {
        _selectedProduct.value = product
    }

    fun clearSelection() {
        _selectedProduct.value = null
    }
}
