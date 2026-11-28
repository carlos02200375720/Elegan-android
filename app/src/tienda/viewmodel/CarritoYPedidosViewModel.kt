package com.example.ui.viewmodel

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.CashOnDeliveryConfig
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.observability.MonitorErrores
import com.example.data.repository.EleganRepository
import com.example.data.repository.ErrorCrearPedidoApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Resumen inmutable de los importes de checkout que sobrevive cambios de configuración
 * dentro de [CarritoYPedidosViewModel].
 */
@Immutable
data class ResumenCheckout(
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0
)

/**
 * ViewModel dedicado a los flujos de Carrito, Verificación, Pago (Transferencia y Contra entrega),
 * pantalla de Gracias e historial de Pedidos.
 */
class CarritoYPedidosViewModel(
    private val repository: EleganRepository,
    autoLoad: Boolean = true
) : ViewModel() {

    private var selectedCouponCode = ""
    private var selectedIdsForOrder: Set<String> = emptySet()

    private val _checkoutSummary = MutableStateFlow(ResumenCheckout())
    val checkoutSummary: StateFlow<ResumenCheckout> = _checkoutSummary.asStateFlow()

    private val _cashOnDeliveryConfig = MutableStateFlow(CashOnDeliveryConfig())
    val cashOnDeliveryConfig: StateFlow<CashOnDeliveryConfig> = _cashOnDeliveryConfig.asStateFlow()

    private val _selectedCartItemIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedCartItemIds: StateFlow<Set<String>> = _selectedCartItemIds.asStateFlow()

    private val _lastCreatedOrderId = MutableStateFlow<String?>(null)
    val lastCreatedOrderId: StateFlow<String?> = _lastCreatedOrderId.asStateFlow()

    private val _lastOrderPaymentMethod = MutableStateFlow("")
    val lastOrderPaymentMethod: StateFlow<String> = _lastOrderPaymentMethod.asStateFlow()

    private val _lastOrderDetails = MutableStateFlow<OrderConfirmationDetails?>(null)
    val lastOrderDetails: StateFlow<OrderConfirmationDetails?> = _lastOrderDetails.asStateFlow()

    private val _lastPaymentProofUri = MutableStateFlow<String?>(null)
    val lastPaymentProofUri: StateFlow<String?> = _lastPaymentProofUri.asStateFlow()

    val cartItems: StateFlow<List<CartItem>> = repository.observeCart()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val cartError: StateFlow<String?> = repository.observeCartError()
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val orders: StateFlow<List<Order>> = repository.observeOrders()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (autoLoad) {
            loadCashOnDeliveryConfig()
            refreshCart()
            refreshOrders()
        }
    }

    fun updateCheckoutTotals(
        subtotal: Double,
        discount: Double,
        shipping: Double,
        total: Double
    ) {
        _checkoutSummary.value = ResumenCheckout(
            subtotal = subtotal,
            discount = discount,
            shipping = shipping,
            total = total
        )
    }

    fun loadCashOnDeliveryConfig() {
        viewModelScope.launch {
            runCatching { repository.getCashOnDeliveryConfig() }
                .onSuccess { _cashOnDeliveryConfig.value = it }
        }
    }

    fun setSelectedCartItemIds(ids: List<String>) {
        val uniqueIds = ids.toSet()
        selectedIdsForOrder = uniqueIds
        _selectedCartItemIds.value = uniqueIds
    }

    fun clearCartError() = repository.clearCartError()

    fun setCouponCode(code: String) {
        selectedCouponCode = code.trim().uppercase()
    }

    fun currentCouponCode(): String = selectedCouponCode

    fun validateCoupon(
        code: String,
        selectedIds: List<String>,
        onResult: (Result<Map<String, Any?>>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                onResult(Result.success(repository.validateCoupon(code, selectedIds)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun refreshCart(onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.refreshCart()
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCarrito", "Error al sincronizar el carrito.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun refreshOrders() {
        viewModelScope.launch {
            try {
                repository.refreshOrders()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganPedidos", "Error al sincronizar las órdenes.", e)
            }
        }
    }

    fun addToCart(
        product: Product,
        size: String,
        quantity: Long = 1L,
        color: String = "",
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.addToCart(product, size, quantity, color)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCarrito", "Error al agregar al carrito.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun updateCartQuantity(
        cartItemId: String,
        quantity: Long,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateCartQuantity(cartItemId, quantity)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCarrito", "Error al actualizar cantidad.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun removeFromCart(
        cartItemId: String,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.removeFromCart(cartItemId)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCarrito", "Error al eliminar del carrito.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun clearCart(onResult: (Result<Unit>) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.clearCart()
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCarrito", "Error al vaciar el carrito.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun loadBankAccounts(onResult: (Result<List<BankAccountInfo>>) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(Result.success(repository.getBankAccounts()))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganCheckout", "Error al cargar cuentas bancarias.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun createManualPaymentOrder(
        paymentMethod: String,
        bankAccountId: String,
        cashOnDeliveryAccepted: Boolean,
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        subtotal: Double = 0.0,
        discount: Double = 0.0,
        shipping: Double = 0.0,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val selectedIds = selectedIdsForOrder
                if (selectedIds.isEmpty()) {
                    throw IllegalStateException("Selecciona al menos un producto antes de confirmar el pedido.")
                }
                val snapshot = cartItems.value.filter { it.id in selectedIds }
                if (snapshot.size != selectedIds.size) {
                    throw IllegalStateException("El carrito cambió. Regresa al carrito y vuelve a seleccionar los productos.")
                }
                val id = repository.createPaymentOrder(
                    paymentMethod, bankAccountId, cashOnDeliveryAccepted,
                    customerName, email, phone, address, city, selectedCouponCode, selectedIds.toList()
                )
                selectedCouponCode = ""
                val label = if (paymentMethod == "bank_transfer") "Transferencia bancaria" else "Pago contra entrega"
                val effectiveSubtotal = if (subtotal > 0.0) subtotal else snapshot.sumOf { it.price * it.quantity }
                val effectiveShipping = if (shipping > 0.0 || snapshot.isEmpty()) shipping else snapshot.sumOf { it.shippingPrice }
                _lastPaymentProofUri.value = null
                repository.clearConfirmedPaymentProofPreview()
                _lastOrderDetails.value = OrderConfirmationDetails(
                    orderId = id,
                    items = snapshot,
                    total = total,
                    paymentMethod = label,
                    customerName = customerName,
                    phone = phone,
                    address = address,
                    city = city,
                    subtotal = effectiveSubtotal,
                    discount = discount.coerceAtLeast(0.0),
                    shippingCost = effectiveShipping.coerceAtLeast(0.0)
                )
                _lastOrderPaymentMethod.value = label
                _lastCreatedOrderId.value = id
                onResult(Result.success(id))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val errorExtras = mutableMapOf("payment_method" to paymentMethod)
                if (e is ErrorCrearPedidoApi) {
                    errorExtras["http_status"] = e.statusCode.toString()
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
        viewModelScope.launch {
            try {
                val uploadedUrl = repository.uploadPaymentProof(orderId, uri)
                val localPreviewUri = repository.saveConfirmedPaymentProofPreview(uri)
                val resolvedProofImage = localPreviewUri
                    ?: uploadedUrl.trim()
                        .takeIf { it.isNotBlank() && !it.equals("uploaded", ignoreCase = true) }
                        ?.let(::resolveBackendMediaUrl)
                    ?: uri.toString()
                _lastPaymentProofUri.value = resolvedProofImage
                _lastOrderDetails.value = _lastOrderDetails.value?.copy(
                    paymentProofUri = resolvedProofImage
                )
                refreshOrders()
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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
        selectedIdsForOrder = emptySet()
        _selectedCartItemIds.value = emptySet()
        _checkoutSummary.value = ResumenCheckout()
        _lastCreatedOrderId.value = null
        _lastOrderPaymentMethod.value = ""
        _lastOrderDetails.value = null
        _lastPaymentProofUri.value = null
        repository.clearConfirmedPaymentProofPreview()
    }
}
