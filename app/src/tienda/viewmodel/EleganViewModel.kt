package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.flow.StateFlow

class EleganViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = EleganRepository(application)
    private val catalogoCtrl = ControladorCatalogoYAnuncios(repository, viewModelScope)
    private val interaccionesCtrl = ControladorInteraccionesPublicacion(
        repository = repository,
        products = catalogoCtrl.products,
        scope = viewModelScope
    )
    private val pedidosCtrl = ControladorCarritoYPedidos(repository, viewModelScope)
    private val perfilCtrl = ControladorPerfilYSoporte(repository, viewModelScope)

    val selectedCategory: StateFlow<String> = catalogoCtrl.selectedCategory
    val selectedProduct: StateFlow<Product?> = catalogoCtrl.selectedProduct
    val searchQuery: StateFlow<String> = catalogoCtrl.searchQuery
    val products: StateFlow<List<Product>> = catalogoCtrl.products
    val isLoadingProducts: StateFlow<Boolean> = catalogoCtrl.isLoading
    val productsError: StateFlow<String?> = catalogoCtrl.error

    val savedProductIds: StateFlow<Set<String>> = interaccionesCtrl.savedProductIds
    val savedProducts: StateFlow<List<Product>> = interaccionesCtrl.savedProducts
    val likedPublicationIds: StateFlow<Set<String>> = interaccionesCtrl.likedPublicationIds
    val publicationStats: StateFlow<Map<String, PublicationStats>> = interaccionesCtrl.publicationStats
    val publicationComments: StateFlow<List<PublicationComment>> = interaccionesCtrl.publicationComments

    val cartItems: StateFlow<List<CartItem>> = pedidosCtrl.cartItems
    val orders: StateFlow<List<Order>> = pedidosCtrl.orders
    val lastCreatedOrderId: StateFlow<String?> = pedidosCtrl.lastCreatedOrderId
    val lastOrderPaymentMethod: StateFlow<String> = pedidosCtrl.lastOrderPaymentMethod
    val lastOrderDetails: StateFlow<OrderConfirmationDetails?> = pedidosCtrl.lastOrderDetails

    val messages: StateFlow<List<SupportMessage>> = perfilCtrl.messages
    val userProfile: StateFlow<UserProfile?> = perfilCtrl.userProfile

    init {
        catalogoCtrl.refreshProducts()
    }

    fun refreshProducts() = catalogoCtrl.refreshProducts()

    fun setCategory(category: String) = catalogoCtrl.setCategory(category)

    fun setSearchQuery(query: String) = catalogoCtrl.setSearchQuery(query)

    fun selectProduct(product: Product) = catalogoCtrl.selectProduct(product)

    fun toggleSaveProduct(productId: String, onResult: (Result<Boolean>) -> Unit = {}) =
        interaccionesCtrl.toggleSaveProduct(productId, onResult)

    fun togglePublicationLike(productId: String, onResult: (Result<Boolean>) -> Unit = {}) =
        interaccionesCtrl.togglePublicationLike(productId, onResult)

    fun openPublicationComments(productId: String?) =
        interaccionesCtrl.openPublicationComments(productId)

    fun addPublicationComment(
        productId: String,
        displayName: String,
        text: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) = interaccionesCtrl.addPublicationComment(productId, displayName, text, onResult)

    fun recordPublicationShare(productId: String, onResult: (Result<Unit>) -> Unit = {}) =
        interaccionesCtrl.recordPublicationShare(productId, onResult)

    fun addToCart(product: Product, size: String, quantity: Long = 1L, color: String = "") =
        pedidosCtrl.addToCart(product, size, quantity, color)

    fun updateCartQuantity(cartItemId: String, quantity: Long) =
        pedidosCtrl.updateCartQuantity(cartItemId, quantity)

    fun removeFromCart(cartItemId: String) = pedidosCtrl.removeFromCart(cartItemId)

    fun clearCart() = pedidosCtrl.clearCart()

    fun setProductSaved(productId: String, isSaved: Boolean, onResult: (Result<Unit>) -> Unit = {}) =
        interaccionesCtrl.setProductSaved(productId, isSaved, onResult)

    fun placeOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        paymentProofUri: String = "",
        onSuccess: (String) -> Unit
    ) = pedidosCtrl.placeOrder(
        customerName = customerName,
        email = email,
        phone = phone,
        address = address,
        city = city,
        paymentMethod = paymentMethod,
        total = total,
        itemCount = itemCount,
        itemsSummary = itemsSummary,
        paymentProofUri = paymentProofUri,
        onSuccess = onSuccess
    )

    fun sendSupportMessage(text: String, mediaUri: String = "", mediaType: String = "") =
        perfilCtrl.sendSupportMessage(text, mediaUri, mediaType)

    fun saveProfile(
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        onResult: (Result<Unit>) -> Unit
    ) = perfilCtrl.saveProfile(name, email, phone, address, city, onResult)

    fun deleteAccount(onComplete: () -> Unit) = perfilCtrl.deleteAccount(
        onCleared = {
            catalogoCtrl.clearSelection()
            pedidosCtrl.clearOrderState()
        },
        onComplete = onComplete
    )
}
