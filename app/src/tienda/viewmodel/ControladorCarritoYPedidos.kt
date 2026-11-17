package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.ProductJson
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorCarritoYPedidos(
    private val repository: EleganRepository,
    private val scope: CoroutineScope
) {
    private val _lastCreatedOrderId = MutableStateFlow<String?>(null)
    val lastCreatedOrderId: StateFlow<String?> = _lastCreatedOrderId.asStateFlow()

    private val _lastOrderPaymentMethod = MutableStateFlow("")
    val lastOrderPaymentMethod: StateFlow<String> = _lastOrderPaymentMethod.asStateFlow()

    private val _lastOrderDetails = MutableStateFlow<OrderConfirmationDetails?>(null)
    val lastOrderDetails: StateFlow<OrderConfirmationDetails?> = _lastOrderDetails.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    fun clearOrderState() {
        _lastCreatedOrderId.value = null
        _lastOrderPaymentMethod.value = ""
        _lastOrderDetails.value = null
    }

    fun addToCart(product: ProductJson, size: String, quantity: Long, color: String) {
        scope.launch {
            try {
                repository.addToCart(product, size, quantity, color)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al agregar al carrito: ${e.message}")
            }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        scope.launch {
            try {
                repository.updateCartQuantity(cartItemId, quantity)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al actualizar cantidad: ${e.message}")
            }
        }
    }

    fun removeFromCart(cartItemId: String) {
        scope.launch {
            try {
                repository.removeFromCart(cartItemId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al eliminar del carrito: ${e.message}")
            }
        }
    }

    fun clearCart() {
        scope.launch {
            try {
                repository.clearCart()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al vaciar el carrito: ${e.message}")
            }
        }
    }

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
        paymentProofUri: String,
        onSuccess: (String) -> Unit
    ) {
        scope.launch {
            try {
                val purchasedItemsSnapshot = cartItems.value.toList()
                val orderId = repository.createOrder(
                    customerName = customerName,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    paymentMethod = paymentMethod,
                    total = total,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary,
                    paymentProofUri = paymentProofUri
                )
                _lastOrderDetails.value = OrderConfirmationDetails(
                    orderId = orderId,
                    items = purchasedItemsSnapshot,
                    total = total,
                    paymentMethod = paymentMethod,
                    customerName = customerName,
                    phone = phone,
                    address = address,
                    city = city
                )
                _lastOrderPaymentMethod.value = paymentMethod
                _lastCreatedOrderId.value = orderId
                onSuccess(orderId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al procesar la orden: ${e.message}")
            }
        }
    }
}
