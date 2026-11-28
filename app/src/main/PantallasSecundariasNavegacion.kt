package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.data.local.EstadoBloqueoTransferencia
import com.example.data.model.BankAccountInfo
import com.example.data.model.productId
import com.example.data.remote.RealtimeEvents
import com.example.ui.CarritoScreen
import com.example.ui.CategoriaScreen
import com.example.ui.ChatsScreen
import com.example.ui.GraciaScreen
import com.example.ui.ProductoScreen
import com.example.ui.VerificasionScreen
import com.example.ui.viewmodel.CarritoYPedidosViewModel
import com.example.ui.viewmodel.CatalogoViewModel
import com.example.ui.viewmodel.InteraccionesViewModel
import com.example.ui.viewmodel.PerfilYSoporteViewModel

internal fun NavGraphBuilder.registrarRutasSecundarias(
    catalogoViewModel: CatalogoViewModel,
    interaccionesViewModel: InteraccionesViewModel,
    carritoYPedidosViewModel: CarritoYPedidosViewModel,
    perfilYSoporteViewModel: PerfilYSoporteViewModel,
    isAuthenticated: Boolean,
    activeTransferLock: () -> EstadoBloqueoTransferencia?,
    onRequireAuth: (String, () -> Unit) -> Unit,
    onNavigateTo: (RutaApp) -> Unit,
    onBack: (RutaApp) -> Unit,
    onNavigateToPerfilOrders: () -> Unit,
    onTransferLockChanged: (EstadoBloqueoTransferencia) -> Unit,
    onTransferLockCleared: () -> Unit,
    onTransferPaymentConfirmed: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    composable<RutaApp.Categoria> {
        val products by catalogoViewModel.products.collectAsStateWithLifecycle()
        val categories by catalogoViewModel.categories.collectAsStateWithLifecycle()
        var accountDeletionError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            catalogoViewModel.loadCategoriesOnce()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            CategoriaScreen(
                categories = categories,
                products = products,
                onCategorySelected = { cat ->
                    catalogoViewModel.setCategory(cat)
                    onNavigateTo(RutaApp.Catalogo)
                },
                deleteAccountError = accountDeletionError,
                onDismissDeleteAccountError = { accountDeletionError = null },
                onDeleteAccount = {
                    if (isAuthenticated) {
                        accountDeletionError = null
                        perfilYSoporteViewModel.deleteAccount(
                            onCleared = {
                                catalogoViewModel.clearSelection()
                                carritoYPedidosViewModel.clearOrderState()
                            },
                            onComplete = { onAccountDeleted() },
                            onFailure = { accountDeletionError = it }
                        )
                    } else {
                        onRequireAuth("Regístrate o inicia sesión para gestionar tu cuenta.") {}
                    }
                },
                onBack = { onBack(RutaApp.Inicio) }
            )
        }
    }

    composable<RutaApp.Producto> { backStackEntry ->
        val route = backStackEntry.toRoute<RutaApp.Producto>()
        val products by catalogoViewModel.products.collectAsStateWithLifecycle()
        val recommendedProducts by catalogoViewModel.recommendedProducts.collectAsStateWithLifecycle()
        val categories by catalogoViewModel.categories.collectAsStateWithLifecycle()
        val selectedProduct by catalogoViewModel.selectedProduct.collectAsStateWithLifecycle()
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val savedProductIds by interaccionesViewModel.savedProductIds.collectAsStateWithLifecycle()

        val prod = remember(route.productId, selectedProduct, products, cartItems) {
            val targetId = route.productId.trim()
            if (targetId.isNotBlank()) {
                selectedProduct?.takeIf { it.productId() == targetId }
                    ?: products.firstOrNull { it.productId() == targetId }
                    ?: selectedProduct
            } else {
                selectedProduct
                    ?: cartItems.lastOrNull()?.let { item -> products.find { it.productId() == item.productId } }
                    ?: products.firstOrNull()
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            if (prod != null) {
                val prodId = prod.productId()
                LaunchedEffect(prodId) {
                    catalogoViewModel.loadRecommendedProducts(prodId)
                }
                ProductoScreen(
                    product = prod,
                    cartItemCount = cartItems.sumOf { it.quantity },
                    isSaved = savedProductIds.contains(prodId),
                    onToggleSave = { onResult ->
                        onRequireAuth("Regístrate para guardar esta prenda en tu perfil.") {
                            interaccionesViewModel.toggleSaveProduct(prodId, onResult)
                        }
                    },
                    onAddToCart = { p, s, color, q, onResult ->
                        onRequireAuth("Regístrate para añadir prendas a tu carrito.") {
                            carritoYPedidosViewModel.addToCart(p, s, q, color, onResult)
                        }
                    },
                    onBuyNow = { p, s, color, q, onResult ->
                        onRequireAuth("Regístrate para comprar tus prendas favoritas.") {
                            carritoYPedidosViewModel.addToCart(p, s, q, color) { result ->
                                onResult(result)
                                if (result.isSuccess) {
                                    onNavigateTo(RutaApp.Carrito)
                                }
                            }
                        }
                    },
                    onNavigateToCart = { onNavigateTo(RutaApp.Carrito) },
                    onBack = { onBack(RutaApp.Catalogo) },
                    categories = categories,
                    recommendedProducts = recommendedProducts,
                    savedProductIds = savedProductIds,
                    onSelectRecommendedProduct = { recommended ->
                        catalogoViewModel.selectProduct(recommended)
                        onNavigateTo(RutaApp.Producto(recommended.productId()))
                    },
                    onToggleSaveRecommended = { recommendedId, onResult ->
                        onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                            interaccionesViewModel.toggleSaveProduct(recommendedId, onResult)
                        }
                    }
                )
            } else {
                LaunchedEffect(Unit) { onBack(RutaApp.Catalogo) }
            }
        }
    }

    composable<RutaApp.Carrito> {
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val cartError by carritoYPedidosViewModel.cartError.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            carritoYPedidosViewModel.refreshCart()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            CarritoScreen(
                cartItems = cartItems,
                cartError = cartError,
                onRetrySync = { carritoYPedidosViewModel.refreshCart() },
                onUpdateQuantity = { id, qty ->
                    onRequireAuth("Regístrate para modificar los artículos de tu carrito.") {
                        carritoYPedidosViewModel.updateCartQuantity(id, qty)
                    }
                },
                onRemoveItem = { id ->
                    onRequireAuth("Regístrate para modificar los artículos de tu carrito.") {
                        carritoYPedidosViewModel.removeFromCart(id)
                    }
                },
                onValidateCoupon = { code, selectedIds, onResult ->
                    onRequireAuth("Inicia sesión para validar un cupón.") {
                        carritoYPedidosViewModel.validateCoupon(code, selectedIds, onResult)
                    }
                },
                onCouponCodeApplied = { code -> carritoYPedidosViewModel.setCouponCode(code) },
                onNavigateToVerificasion = { sub, disc, ship, tot, selectedIds ->
                    onRequireAuth("Regístrate para continuar con tu compra.") {
                        carritoYPedidosViewModel.setSelectedCartItemIds(selectedIds)
                        carritoYPedidosViewModel.updateCheckoutTotals(sub, disc, ship, tot)
                        onNavigateTo(RutaApp.Verificacion)
                    }
                },
                onExploreCatalog = {
                    catalogoViewModel.setCategory(null)
                    onNavigateTo(RutaApp.Catalogo)
                },
                onBack = { onBack(RutaApp.Inicio) }
            )
        }
    }

    composable<RutaApp.Verificacion> {
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val selectedCartItemIds by carritoYPedidosViewModel.selectedCartItemIds.collectAsStateWithLifecycle()
        val cashOnDeliveryConfig by carritoYPedidosViewModel.cashOnDeliveryConfig.collectAsStateWithLifecycle()
        val checkoutSummary by carritoYPedidosViewModel.checkoutSummary.collectAsStateWithLifecycle()
        val userProfile by perfilYSoporteViewModel.userProfile.collectAsStateWithLifecycle()
        val sessionUser by perfilYSoporteViewModel.sessionUser.collectAsStateWithLifecycle()
        val checkoutCartItems = remember(cartItems, selectedCartItemIds) {
            cartItems.filter { it.id in selectedCartItemIds }
        }
        val currentLock = activeTransferLock()
        val effectiveSubtotal = if (checkoutSummary.total > 0.0 || currentLock == null) checkoutSummary.subtotal else currentLock.subtotal
        val effectiveDiscount = if (checkoutSummary.total > 0.0 || currentLock == null) checkoutSummary.discount else currentLock.discount
        val effectiveShipping = if (checkoutSummary.total > 0.0 || currentLock == null) checkoutSummary.shipping else currentLock.shipping
        val effectiveTotal = if (checkoutSummary.total > 0.0 || currentLock == null) checkoutSummary.total else currentLock.total

        var bankAccounts by remember {
            mutableStateOf<List<BankAccountInfo>>(currentLock?.bankAccounts.orEmpty())
        }
        var isSubmittingPayment by remember { mutableStateOf(false) }
        var paymentError by remember { mutableStateOf<String?>(null) }
        var pendingTransferOrderId by remember {
            mutableStateOf<String?>(currentLock?.pendingTransferOrderId)
        }

        LaunchedEffect(Unit) {
            paymentError = null
            carritoYPedidosViewModel.loadCashOnDeliveryConfig()
            carritoYPedidosViewModel.loadBankAccounts { result ->
                result.onSuccess { bankAccounts = it; paymentError = null }
                    .onFailure {
                        paymentError = it.localizedMessage ?: "No se pudieron cargar las cuentas bancarias."
                    }
            }
            RealtimeEvents.events.collect { event ->
                if (event.collection == "bank_accounts") {
                    carritoYPedidosViewModel.loadBankAccounts { result ->
                        result.onSuccess { bankAccounts = it; paymentError = null }
                            .onFailure {
                                paymentError = it.localizedMessage ?: "No se pudieron actualizar las cuentas bancarias."
                            }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            VerificasionScreen(
                cartItems = checkoutCartItems,
                cashOnDeliveryEnabled = cashOnDeliveryConfig.enabled &&
                    checkoutCartItems.isNotEmpty() &&
                    checkoutCartItems.all { it.allowCashOnDelivery },
                cashOnDeliveryMessage = cashOnDeliveryConfig.message,
                subtotal = effectiveSubtotal,
                discount = effectiveDiscount,
                shipping = effectiveShipping,
                total = effectiveTotal,
                userProfile = userProfile,
                bankAccounts = bankAccounts,
                currentUserId = sessionUser?.effectiveId.orEmpty(),
                sessionUserName = sessionUser?.name.orEmpty(),
                sessionUserEmail = sessionUser?.email.orEmpty(),
                isSubmittingPayment = isSubmittingPayment,
                paymentError = paymentError,
                onDismissPaymentError = { paymentError = null },
                couponCode = carritoYPedidosViewModel.currentCouponCode(),
                pendingTransferOrderId = pendingTransferOrderId,
                restoredTransferLock = currentLock,
                onTransferLockChanged = onTransferLockChanged,
                onTransferLockExitByHeaderBack = {
                    pendingTransferOrderId = null
                    paymentError = null
                    onTransferLockCleared()
                },
                onLoadBankAccounts = {
                    carritoYPedidosViewModel.loadBankAccounts { result ->
                        result.onSuccess { bankAccounts = it; paymentError = null }
                            .onFailure {
                                paymentError = it.localizedMessage ?: "No se pudieron cargar las cuentas bancarias."
                            }
                    }
                },
                onSubmitBankTransfer = { accountId, proofUri, name, email, phone, address, city ->
                    isSubmittingPayment = true
                    paymentError = null
                    fun uploadForOrder(orderId: String) {
                        pendingTransferOrderId = orderId
                        carritoYPedidosViewModel.uploadPaymentProof(orderId, proofUri) { upload ->
                            isSubmittingPayment = false
                            upload.onSuccess {
                                pendingTransferOrderId = null
                                onTransferPaymentConfirmed()
                            }.onFailure {
                                paymentError = it.localizedMessage
                                    ?: "No se pudo subir el comprobante. Puedes volver a intentarlo sin crear otro pedido."
                            }
                        }
                    }
                    val existingOrderId = pendingTransferOrderId
                    if (!existingOrderId.isNullOrBlank()) {
                        uploadForOrder(existingOrderId)
                    } else {
                        carritoYPedidosViewModel.createManualPaymentOrder(
                            paymentMethod = "bank_transfer",
                            bankAccountId = accountId,
                            cashOnDeliveryAccepted = false,
                            customerName = name,
                            email = email,
                            phone = phone,
                            address = address,
                            city = city,
                            total = effectiveTotal,
                            subtotal = effectiveSubtotal,
                            discount = effectiveDiscount,
                            shipping = effectiveShipping
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
                    carritoYPedidosViewModel.createManualPaymentOrder(
                        paymentMethod = "cash_on_delivery",
                        bankAccountId = "",
                        cashOnDeliveryAccepted = true,
                        customerName = name,
                        email = email,
                        phone = phone,
                        address = address,
                        city = city,
                        total = effectiveTotal,
                        subtotal = effectiveSubtotal,
                        discount = effectiveDiscount,
                        shipping = effectiveShipping
                    ) { result ->
                        isSubmittingPayment = false
                        result.onSuccess { onNavigateTo(RutaApp.Gracia) }
                            .onFailure { paymentError = it.localizedMessage ?: "No se pudo registrar el pedido." }
                    }
                },
                onBack = {
                    paymentError = null
                    carritoYPedidosViewModel.setCouponCode("")
                    onBack(RutaApp.Carrito)
                }
            )
        }
    }

    composable<RutaApp.Gracia> {
        val lastOrderId by carritoYPedidosViewModel.lastCreatedOrderId.collectAsStateWithLifecycle()
        val lastOrderPaymentMethod by carritoYPedidosViewModel.lastOrderPaymentMethod.collectAsStateWithLifecycle()
        val lastOrderDetails by carritoYPedidosViewModel.lastOrderDetails.collectAsStateWithLifecycle()
        val lastPaymentProofUri by carritoYPedidosViewModel.lastPaymentProofUri.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            GraciaScreen(
                orderId = lastOrderId,
                isBankTransfer = lastOrderPaymentMethod.contains("Transferencia", ignoreCase = true),
                isCashOnDelivery = lastOrderPaymentMethod.contains("contra entrega", ignoreCase = true),
                orderDetails = lastOrderDetails,
                paymentProofUri = lastOrderDetails?.paymentProofUri ?: lastPaymentProofUri,
                onNavigateToInicio = { onNavigateTo(RutaApp.Inicio) },
                onNavigateToPerfil = onNavigateToPerfilOrders,
                onNavigateToChats = { onNavigateTo(RutaApp.Chats) }
            )
        }
    }

    composable<RutaApp.Chats> {
        val messages by perfilYSoporteViewModel.messages.collectAsStateWithLifecycle()
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            perfilYSoporteViewModel.refreshMessages()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
        ) {
            ChatsScreen(
                messages = messages,
                cartItemCount = cartItems.sumOf { it.quantity },
                isAuthenticated = isAuthenticated,
                onRequireAuth = onRequireAuth,
                onNavigateToCart = { onNavigateTo(RutaApp.Carrito) },
                onSendMessage = { text, mediaUri, mediaType ->
                    perfilYSoporteViewModel.sendSupportMessage(text, mediaUri, mediaType)
                },
                onBack = { onBack(RutaApp.Inicio) }
            )
        }
    }
}
