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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.BankAccountInfo
import com.example.data.model.productId
import com.example.ui.CarritoScreen
import com.example.ui.CategoriaScreen
import com.example.ui.ChatsScreen
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
    viewModel: EleganViewModel,
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onNavigateTo: (Screen) -> Unit,
    onUpdateCheckoutTotals: (Double, Double, Double, Double) -> Unit,
    onAccountDeleted: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val cartError by viewModel.cartError.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val savedProductIds by viewModel.savedProductIds.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val lastOrderId by viewModel.lastCreatedOrderId.collectAsState()
    val lastOrderPaymentMethod by viewModel.lastOrderPaymentMethod.collectAsState()
    val lastOrderDetails by viewModel.lastOrderDetails.collectAsState()
    var bankAccounts by remember { mutableStateOf<List<BankAccountInfo>>(emptyList()) }
    var isSubmittingPayment by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(screen) {
        when (screen) {
            Screen.CATEGORIA -> viewModel.loadCategoriesOnce()
            Screen.CARRITO -> viewModel.refreshCart()
            Screen.CHATS -> viewModel.refreshMessages()
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
                    onDeleteAccount = {
                        if (isAuthenticated) {
                            viewModel.deleteAccount { onAccountDeleted() }
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
                        categories = categories
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
                    onValidateCoupon = { code, onResult ->
                        onRequireAuth("Inicia sesión para validar un cupón.") {
                            viewModel.validateCoupon(code, onResult)
                        }
                    },
                    onCouponCodeApplied = { code -> viewModel.setCouponCode(code) },
                    onNavigateToVerificasion = { sub, disc, ship, tot ->
                        onRequireAuth("Regístrate para continuar con tu compra.") {
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
                val itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" }
                val totalItemCount = cartItems.sumOf { it.quantity }
                VerificasionScreen(
                    cartItems = cartItems,
                    subtotal = checkoutSubtotal,
                    discount = checkoutDiscount,
                    shipping = checkoutShipping,
                    total = checkoutTotal,
                    userProfile = userProfile,
                    bankAccounts = bankAccounts,
                    isSubmittingPayment = isSubmittingPayment,
                    paymentError = paymentError,
                    onLoadBankAccounts = {
                        viewModel.loadBankAccounts { result ->
                            result.onSuccess { bankAccounts = it; paymentError = null }
                                .onFailure { paymentError = it.localizedMessage ?: "No se pudieron cargar las cuentas bancarias." }
                        }
                    },
                    onSubmitBankTransfer = { accountId, proofUri, name, email, phone, address, city ->
                        isSubmittingPayment = true
                        paymentError = null
                        viewModel.createManualPaymentOrder(
                            paymentMethod = "bank_transfer", bankAccountId = accountId, cashOnDeliveryAccepted = false,
                            customerName = name, email = email, phone = phone, address = address, city = city,
                            total = checkoutTotal, couponCode = ""
                        ) { result ->
                            result.onSuccess { orderId ->
                                viewModel.uploadPaymentProof(orderId, proofUri) { upload ->
                                    isSubmittingPayment = false
                                    upload.onSuccess { onNavigateTo(Screen.GRACIA) }
                                        .onFailure { paymentError = it.localizedMessage ?: "El pedido se creó, pero no se pudo subir el comprobante. Vuelve a intentarlo desde soporte." }
                                }
                            }.onFailure {
                                isSubmittingPayment = false
                                paymentError = it.localizedMessage ?: "No se pudo registrar el pedido."
                            }
                        }
                    },
                    onSubmitCashOnDelivery = { name, email, phone, address, city ->
                        isSubmittingPayment = true
                        paymentError = null
                        viewModel.createManualPaymentOrder(
                            paymentMethod = "cash_on_delivery", bankAccountId = "", cashOnDeliveryAccepted = true,
                            customerName = name, email = email, phone = phone, address = address, city = city,
                            total = checkoutTotal, couponCode = ""
                        ) { result ->
                            isSubmittingPayment = false
                            result.onSuccess { onNavigateTo(Screen.GRACIA) }
                                .onFailure { paymentError = it.localizedMessage ?: "No se pudo registrar el pedido." }
                        }
                    },
                    onBack = { viewModel.setCouponCode(""); onNavigateTo(Screen.CARRITO) }
                )
            }

            Screen.GRACIA -> {
                GraciaScreen(
                    orderId = lastOrderId,
                    isBankTransfer = lastOrderPaymentMethod.contains("Transferencia", ignoreCase = true),
                    orderDetails = lastOrderDetails,
                    onNavigateToInicio = { onNavigateTo(Screen.INICIO) },
                    onNavigateToPerfil = { onNavigateTo(Screen.PERFIL) },
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
