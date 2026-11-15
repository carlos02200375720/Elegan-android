package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.Announcement
import com.example.data.model.Product
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorCatalogoYAnuncios(
    private val repository: EleganRepository,
    private val scope: CoroutineScope
) {
    private val _selectedCategory = MutableStateFlow("todo")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedProduct = MutableStateFlow<Product?>(null)
    val selectedProduct: StateFlow<Product?> = _selectedProduct.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _deletedProductIds = MutableStateFlow<Set<String>>(emptySet())
    private val _deletedAnnouncementIds = MutableStateFlow<Set<String>>(emptySet())

    val products: StateFlow<List<Product>> = combine(
        repository.observeProducts(),
        _deletedProductIds
    ) { list, deletedIds ->
        list.filter { it.id !in deletedIds }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = repository.getInitialSampleProducts()
    )

    val announcements: StateFlow<List<Announcement>> = combine(
        repository.observeAnnouncements(),
        _deletedAnnouncementIds
    ) { list, deletedIds ->
        list.filter { it.id !in deletedIds }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = repository.getInitialSampleAnnouncements()
    )

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProduct(product: Product) {
        _selectedProduct.value = product
    }

    fun clearSelection() {
        _selectedProduct.value = null
    }

    fun publishProduct(product: Product, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            try {
                repository.addProduct(product)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "No se pudo publicar el producto.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun publishAnnouncement(announcement: Announcement, onResult: (Boolean) -> Unit) {
        scope.launch {
            try {
                repository.addAnnouncement(announcement)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun deleteProduct(productId: String, onResult: (Boolean) -> Unit) {
        _deletedProductIds.value = _deletedProductIds.value + productId
        scope.launch {
            try {
                repository.deleteProduct(productId)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleProductStock(productId: String, inStock: Boolean) {
        scope.launch {
            try {
                repository.toggleProductStock(productId, inStock)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }

    fun deleteAnnouncement(announcementId: String, onResult: (Boolean) -> Unit) {
        _deletedAnnouncementIds.value = _deletedAnnouncementIds.value + announcementId
        scope.launch {
            try {
                repository.deleteAnnouncement(announcementId)
                onResult(true)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    fun toggleAnnouncementActive(announcementId: String, isActive: Boolean) {
        scope.launch {
            try {
                repository.toggleAnnouncementActive(announcementId, isActive)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }
    }
}
