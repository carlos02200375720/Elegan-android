package com.example.ui.viewmodel

import android.net.Uri
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.ProductJson
import com.example.data.observability.MonitorErrores
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
    private var selectedCouponCode = ""
    private val _lastCreatedOrderId = MutableStateFlow<String?>(null)
    val lastCreatedOrderId: StateFlow<String?> = _lastCreatedOrderId.asStateFlow()
    private val _lastOrderPaymentMethod = MutableStateFlow("")
    val lastOrderPaymentMethod: StateFlow<String> = _lastOrderPaymentMethod.asStateFlow()
    private val _lastOrderDetails = MutableStateFlow<OrderConfirmationDetails?>(null)
    val lastOrderDetails: StateFlow<OrderConfirmationDetails?> = _lastOrderDetails.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }.stateIn(scope, SharingStarted.Eagerly, emptyList())
    val cartError: StateFlow<String?> = repository.observeCartError()
        .catch { emit(null) }.stateIn(scope, SharingStarted.Eagerly, null)
    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    fun clearCartError() = repository.clearCartError()
    fun setCouponCode(code: String) { selectedCouponCode = code.trim().uppercase() }
    fun currentCouponCode(): String = selectedCouponCode

    fun validateCoupon(code: String, onResult: (Result<Map<String, Any?>>) -> Unit) {
        scope.launch {
            try { onResult(Result.success(repository.validateCoupon(code))) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { onResult(Result.failure(e)) }
        }
    }

    fun refreshCart(onResult: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            try { repository.refreshCart(); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al sincronizar el carrito.", e); onResult(Result.failure(e)) }
        }
    }

    fun refreshOrders() {
        scope.launch {
            try { repository.refreshOrders() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al sincronizar las órdenes.", e) }
        }
    }

    fun addToCart(product: ProductJson, size: String, quantity: Long, color: String, onResult: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            try { repository.addToCart(product, size, quantity, color); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al agregar al carrito.", e); onResult(Result.failure(e)) }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long, onResult: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            try { repository.updateCartQuantity(cartItemId, quantity); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al actualizar cantidad.", e); onResult(Result.failure(e)) }
        }
    }

    fun removeFromCart(cartItemId: String, onResult: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            try { repository.removeFromCart(cartItemId); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al eliminar del carrito.", e); onResult(Result.failure(e)) }
        }
    }

    fun clearCart(onResult: (Result<Unit>) -> Unit = {}) {
        scope.launch {
            try { repository.clearCart(); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { MonitorErrores.registrarError("EleganViewModel", "Error al vaciar el carrito.", e); onResult(Result.failure(e)) }
        }
    }

    fun loadBankAccounts(onResult: (Result<List<BankAccountInfo>>) -> Unit) {
        scope.launch {
            try { onResult(Result.success(repository.getBankAccounts())) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                MonitorErrores.registrarError("EleganCheckout", "Error al cargar cuentas bancarias.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun createManualPaymentOrder(
        paymentMethod: String, bankAccountId: String, cashOnDeliveryAccepted: Boolean,
        customerName: String, email: String, phone: String, address: String, city: String,
        total: Double, onResult: (Result<String>) -> Unit
    ) {
        scope.launch {
            try {
                val snapshot = cartItems.value.toList()
                val id = repository.createPaymentOrder(
                    paymentMethod, bankAccountId, cashOnDeliveryAccepted,
                    customerName, email, phone, address, city, selectedCouponCode
                )
                selectedCouponCode = ""
                val label = if (paymentMethod == "bank_transfer") "Transferencia bancaria" else "Pago contra entrega"
                _lastOrderDetails.value = OrderConfirmationDetails(
                    orderId = id, items = snapshot, total = total, paymentMethod = label,
                    customerName = customerName, phone = phone, address = address, city = city
                )
                _lastOrderPaymentMethod.value = label
                _lastCreatedOrderId.value = id
                onResult(Result.success(id))
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val errorExtras = mutableMapOf<String, String>("payment_method" to paymentMethod)
                if (e is com.example.data.repository.ErrorCrearPedidoApi) {
                    errorExtras["http_status"] = e.statusCode
                    errorExtras["backend_detail"] = e.backendDetail ?: "NO_DETAIL"
                }
                MonitorErrores.registrarError(
                    tag = "EleganCheckout",
                    mensaje = "Error al crear pedido manual.",
                    error = e,
                    extras = errorExtras
                )
                onResult(Result.failure(e))
            }
        }
    }

    fun uploadPaymentProof(orderId: String, uri: Uri, onResult: (Result<Unit>) -> Unit) {
        scope.launch {
            try { repository.uploadPaymentProof(orderId, uri); refreshOrders(); onResult(Result.success(Unit)) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                MonitorErrores.registrarError(
                    tag = "EleganCheckout",
                    mensaje = "Error al subir comprobante de pago.",
                    error = e,
                    extras = mapOf("order_id" to orderId)
                )
                onResult(Result.failure(e))
            }
        }
    }

    fun clearOrderState() {
        selectedCouponCode = ""
        _lastCreatedOrderId.value = null
        _lastOrderPaymentMethod.value = ""
        _lastOrderDetails.value = null
    }

    fun clearUserData() {
        clearOrderState()
    }
}
