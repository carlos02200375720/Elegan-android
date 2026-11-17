package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.PayPalOrderSession
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

    private val _payPalOrderSession = MutableStateFlow<PayPalOrderSession?>(null)
    val payPalOrderSession: StateFlow<PayPalOrderSession?> = _payPalOrderSession.asStateFlow()

    private val _isProcessingPayPal = MutableStateFlow(false)
    val isProcessingPayPal: StateFlow<Boolean> = _isProcessingPayPal.asStateFlow()

    private val _payPalError = MutableStateFlow<String?>(null)
    val payPalError: StateFlow<String?> = _payPalError.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = emptyList())

    fun clearPayPalState() {
        _payPalOrderSession.value = null
        _isProcessingPayPal.value = false
        _payPalError.value = null
    }

    fun clearOrderState() {
        _lastCreatedOrderId.value = null
        _lastOrderPaymentMethod.value = ""
        _lastOrderDetails.value = null
        clearPayPalState()
    }

    fun refreshCart() {
        scope.launch {
            try {
                repository.refreshCart()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al sincronizar el carrito: ${e.message}")
            }
        }
    }

    fun refreshOrders() {
        scope.launch {
            try {
                repository.refreshOrders()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al sincronizar las órdenes: ${e.message}")
            }
        }
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

    fun startPayPalOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        onSessionReady: (PayPalOrderSession) -> Unit = {}
    ) {
        scope.launch {
            _isProcessingPayPal.value = true
            _payPalError.value = null
            try {
                val session = repository.createPayPalOrder(
                    customerName = customerName,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    total = total,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary,
                    cartItems = cartItems.value.toList()
                )
                _payPalOrderSession.value = session
                onSessionReady(session)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al iniciar orden PayPal.", e)
                _payPalError.value = e.localizedMessage ?: "No se pudo iniciar el pago con PayPal."
            } finally {
                _isProcessingPayPal.value = false
            }
        }
    }

    fun capturePayPalOrder(
        paypalOrderId: String,
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        onSuccess: (String) -> Unit
    ) {
        scope.launch {
            _isProcessingPayPal.value = true
            _payPalError.value = null
            try {
                val purchasedItemsSnapshot = cartItems.value.toList()
                val orderId = repository.capturePayPalOrder(
                    paypalOrderId = paypalOrderId,
                    customerName = customerName,
                    email = email,
                    phone = phone,
                    address = address,
                    city = city,
                    total = total,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary
                )
                _lastOrderDetails.value = OrderConfirmationDetails(
                    orderId = orderId,
                    items = purchasedItemsSnapshot,
                    total = total,
                    paymentMethod = "PayPal",
                    customerName = customerName,
                    phone = phone,
                    address = address,
                    city = city
                )
                _lastOrderPaymentMethod.value = "PayPal"
                _lastCreatedOrderId.value = orderId
                _payPalOrderSession.value = null
                onSuccess(orderId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al capturar pago PayPal.", e)
                _payPalError.value = e.localizedMessage ?: "No se pudo confirmar el pago en PayPal."
            } finally {
                _isProcessingPayPal.value = false
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
