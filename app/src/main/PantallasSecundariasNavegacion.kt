package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import android.net.Uri
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BankAccountInfo
import com.example.data.model.productCategory
import com.example.data.model.productId
import com.example.ui.CarritoScreen
import com.example.ui.CategoriaScreen
import com.example.ui.ChatsScreen
import com.example.ui.EstadoBloqueoTransferencia
import com.example.ui.GraciaScreen
import com.example.ui.ProductoScreen
import com.example.ui.VerificasionScreen
import com.example.ui.viewmodel.EleganViewModel

@Composable
internal fun PantallasSecundariasNavegacion(
    screen: Screen,
    previousScreen: Screen,
    chatsReturnScreen: Screen,
    checkoutSubtotal: Double,
    checkoutDiscount: Double,
    checkoutShipping: Double,
    checkoutTotal: Double,
    activeTransferLock: EstadoBloqueoTransferencia? = null,
    viewModel: EleganViewModel,
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onNavigateTo: (Screen) -> Unit,
    onNavigateToPerfilOrders: () -> Unit = { onNavigateTo(Screen.PERFIL) },
    onTransferLockChanged: (EstadoBloqueoTransferencia) -> Unit = {},
    onTransferLockCleared: () -> Unit = {},
    onTransferPaymentConfirmed: () -> Unit = { onNavigateTo(Screen.GRACIA) },
    onUpdateCheckoutTotals: (Double, Double, Double, Double) -> Unit,
    onAccountDeleted: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val recommendedProducts by viewModel.recommendedProducts.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCartItemIds by viewModel.selectedCartItemIds.collectAsStateWithLifecycle()
    val cashOnDeliveryConfig by viewModel.cashOnDeliveryConfig.collectAsStateWithLifecycle()
    val checkoutCartItems = cartItems.filter { it.id in selectedCartItemIds }
    val cartError by viewModel.cartError.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val savedProductIds by viewModel.savedProductIds.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val lastOrderId by viewModel.lastCreatedOrderId.collectAsStateWithLifecycle()
    val lastOrderPaymentMethod by viewModel.lastOrderPaymentMethod.collectAsStateWithLifecycle()
    val lastOrderDetails by viewModel.lastOrderDetails.collectAsStateWithLifecycle()
    val lastPaymentProofUri by viewModel.lastPaymentProofUri.collectAsStateWithLifecycle()
    var bankAccounts by remember { mutableStateOf<List<BankAccountInfo>>(activeTransferLock?.bankAccounts.orEmpty()) }
    var isSubmittingPayment by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }
    var accountDeletionError by remember { mutableStateOf<String?>(null) }
    var pendingTransferOrderId by remember { mutableStateOf<String?>(activeTransferLock?.pendingTransferOrderId) }

    LaunchedEffect(screen) {
        when (screen) {
            Screen.CATEGORIA -> viewModel.loadCategoriesOnce()
            Screen.CARRITO -> viewModel.refreshCart()
            Screen.CHATS -> viewModel.refreshMessages()
            Screen.VERIFICASION -> { paymentError = null; viewModel.loadCashOnDeliveryConfig() }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
    ) {
        when (screen) {
            Screen.CATEGORIA -> {
                CategoriaScreen(
                    categories = categories,
                    products = products,
                    onCategorySelected = { cat ->
                        viewModel.setCategory(cat)
                        onNavigateTo(Screen.CATALOGO)
                    },
                    deleteAccountError = accountDeletionError,
                    onDismissDeleteAccountError = { accountDeletionError = null },
                    onDeleteAccount = {
                        if (isAuthenticated) {
                            accountDeletionError = null
                            viewModel.deleteAccount(
                                onComplete = { onAccountDeleted() },
                                onFailure = { accountDeletionError = it }
                            )
                        } else {
                            onRequireAuth("Regístrate o inicia sesión para gestionar tu cuenta.") {}
                        }
                    },
                    onBack = { onNavigateTo(previousScreen) }
                )
            }

            Screen.PRODUCTO -> {
                val prod = selectedProduct
                    ?: cartItems.lastOrNull()?.let { item -> products.find { it.productId() == item.productId } }
                    ?: products.firstOrNull()
                if (prod != null) {
                    val prodId = prod.productId()
                    LaunchedEffect(prodId) {
                        viewModel.loadRecommendedProducts(prodId)
                    }
                    ProductoScreen(
                        product = prod,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        isSaved = savedProductIds.contains(prodId),
                        onToggleSave = { onResult ->
                            onRequireAuth("Regístrate para guardar esta prenda en tu perfil.") {
                                viewModel.toggleSaveProduct(prodId, onResult)
                            }
                        },
                        onAddToCart = { p, s, color, q, onResult ->
                            onRequireAuth("Regístrate para añadir prendas a tu carrito.") {
                                viewModel.addToCart(p, s, q, color, onResult)
                            }
                        },
                        onBuyNow = { p, s, color, q, onResult ->
                            onRequireAuth("Regístrate para comprar tus prendas favoritas.") {
                                viewModel.addToCart(p, s, q, color) { result ->
                                    onResult(result)
                                    if (result.isSuccess) {
                                        onNavigateTo(Screen.CARRITO)
                                    }
                                }
                            }
                        },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        onBack = { onNavigateTo(Screen.CATALOGO) },
                        categories = categories,
                        recommendedProducts = recommendedProducts,
                        savedProductIds = savedProductIds,
                        onSelectRecommendedProduct = { recommended ->
                            viewModel.selectProduct(recommended)
                        },
                        onToggleSaveRecommended = { recommendedId, onResult ->
                            onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                                viewModel.toggleSaveProduct(recommendedId, onResult)
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) { onNavigateTo(Screen.CATALOGO) }
                }
            }

            Screen.CARRITO -> {
                CarritoScreen(
                    cartItems = cartItems,
                    cartError = cartError,
                    onRetrySync = { viewModel.refreshCart() },
                    onUpdateQuantity = { id, qty ->
                        onRequireAuth("Regístrate para modificar los artículos de tu carrito.") {
                            viewModel.updateCartQuantity(id, qty)
                        }
                    },
                    onRemoveItem = { id ->
                        onRequireAuth("Regístrate para modificar los artículos de tu carrito.") {
                            viewModel.removeFromCart(id)
                        }
                    },
                    onValidateCoupon = { code, selectedIds, onResult ->
                        onRequireAuth("Inicia sesión para validar un cupón.") {
                            viewModel.validateCoupon(code, selectedIds, onResult)
                        }
                    },
                    onCouponCodeApplied = { code -> viewModel.setCouponCode(code) },
                    onNavigateToVerificasion = { sub, disc, ship, tot, selectedIds ->
                        onRequireAuth("Regístrate para continuar con tu compra.") {
                            viewModel.setSelectedCartItemIds(selectedIds)
                            onUpdateCheckoutTotals(sub, disc, ship, tot)
                            onNavigateTo(Screen.VERIFICASION)
                        }
                    },
                    onExploreCatalog = {
                        viewModel.setCategory(null)
                        onNavigateTo(Screen.CATALOGO)
                    },
                    onBack = { onNavigateTo(Screen.PRODUCTO) }
                )
            }

            Screen.VERIFICASION -> {
                VerificasionScreen(
                    cartItems = checkoutCartItems,
                    cashOnDeliveryEnabled = cashOnDeliveryConfig["enabled"] as? Boolean == true && checkoutCartItems.isNotEmpty() && checkoutCartItems.all { it.allowCashOnDelivery },
                    cashOnDeliveryMessage = cashOnDeliveryConfig["message"]?.toString().orEmpty(),
                    subtotal = checkoutSubtotal,
                    discount = checkoutDiscount,
                    shipping = checkoutShipping,
                    total = checkoutTotal,
                    userProfile = userProfile,
                    bankAccounts = bankAccounts,
                    isSubmittingPayment = isSubmittingPayment,
                    paymentError = paymentError,
                    onDismissPaymentError = { paymentError = null },
                    couponCode = viewModel.currentCouponCode(),
                    pendingTransferOrderId = pendingTransferOrderId,
                    restoredTransferLock = activeTransferLock,
                    onTransferLockChanged = onTransferLockChanged,
                    onTransferLockExitByHeaderBack = {
                        pendingTransferOrderId = null
                        paymentError = null
                        onTransferLockCleared()
                    },
                    onLoadBankAccounts = {
                        viewModel.loadBankAccounts { result ->
                            result.onSuccess { bankAccounts = it; paymentError = null }
                                .onFailure { paymentError = it.localizedMessage ?: "No se pudieron cargar las cuentas bancarias." }
                        }
                    },
                    onSubmitBankTransfer = { accountId, proofUri, name, email, phone, address, city ->
                        isSubmittingPayment = true
                        paymentError = null
                        fun uploadForOrder(orderId: String) {
                            pendingTransferOrderId = orderId
                            viewModel.uploadPaymentProof(orderId, proofUri) { upload ->
                                isSubmittingPayment = false
                                upload.onSuccess {
                                    pendingTransferOrderId = null
                                    onTransferPaymentConfirmed()
                                }.onFailure {
                                    paymentError = it.localizedMessage ?: "No se pudo subir el comprobante. Puedes volver a intentarlo sin crear otro pedido."
                                }
                            }
                        }
                        val existingOrderId = pendingTransferOrderId
                        if (!existingOrderId.isNullOrBlank()) {
                            uploadForOrder(existingOrderId)
                        } else {
                            viewModel.createManualPaymentOrder(
                                paymentMethod = "bank_transfer", bankAccountId = accountId, cashOnDeliveryAccepted = false,
                                customerName = name, email = email, phone = phone, address = address, city = city,
                                total = checkoutTotal,
                                subtotal = checkoutSubtotal,
                                discount = checkoutDiscount,
                                shipping = checkoutShipping
                            ) { result ->
                                result.onSuccess { orderId -> uploadForOrder(orderId) }
                                    .onFailure {
                                        isSubmittingPayment = false
                                        paymentError = it.localizedMessage ?: "No se pudo registrar el pedido."
                                    }
                            }
                        }
                    },
                    onSubmitCashOnDelivery = { name, email, phone, address, city ->
                        isSubmittingPayment = true
                        paymentError = null
                        viewModel.createManualPaymentOrder(
                            paymentMethod = "cash_on_delivery", bankAccountId = "", cashOnDeliveryAccepted = true,
                            customerName = name, email = email, phone = phone, address = address, city = city,
                            total = checkoutTotal,
                            subtotal = checkoutSubtotal,
                            discount = checkoutDiscount,
                            shipping = checkoutShipping
                        ) { result ->
                            isSubmittingPayment = false
                            result.onSuccess { onNavigateTo(Screen.GRACIA) }
                                .onFailure { paymentError = it.localizedMessage ?: "No se pudo registrar el pedido." }
                        }
                    },
                    onBack = {
                        paymentError = null
                        viewModel.setCouponCode("")
                        onNavigateTo(Screen.CARRITO)
                    }
                )
            }

            Screen.GRACIA -> {
                GraciaScreen(
                    orderId = lastOrderId,
                    isBankTransfer = lastOrderPaymentMethod.contains("Transferencia", ignoreCase = true),
                    isCashOnDelivery = lastOrderPaymentMethod.contains("contra entrega", ignoreCase = true),
                    orderDetails = lastOrderDetails,
                    paymentProofUri = lastOrderDetails?.paymentProofUri ?: lastPaymentProofUri,
                    onNavigateToInicio = { onNavigateTo(Screen.INICIO) },
                    onNavigateToPerfil = onNavigateToPerfilOrders,
                    onNavigateToChats = { onNavigateTo(Screen.CHATS) }
                )
            }

            Screen.CHATS -> {
                ChatsScreen(
                    messages = messages,
                    cartItemCount = cartItems.sumOf { it.quantity },
                    isAuthenticated = isAuthenticated,
                    onRequireAuth = onRequireAuth,
                    onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                    onSendMessage = { text, mediaUri, mediaType ->
                        viewModel.sendSupportMessage(text, mediaUri, mediaType)
                    },
                    onBack = { onNavigateTo(chatsReturnScreen) }
                )
            }

            else -> {}
        }
    }
}
