package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onNavigateTo: (Screen) -> Unit,
    onUpdateCheckoutTotals: (Double, Double, Double, Double) -> Unit,
    onAccountDeleted: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val savedProductIds by viewModel.savedProductIds.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val lastOrderId by viewModel.lastCreatedOrderId.collectAsState()
    val lastOrderPaymentMethod by viewModel.lastOrderPaymentMethod.collectAsState()
    val lastOrderDetails by viewModel.lastOrderDetails.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .windowInsetsPadding(ScaffoldDefaults.contentWindowInsets)
    ) {
        when (screen) {
            Screen.CATEGORIA -> {
                CategoriaScreen(
                    products = products,
                    onCategorySelected = { cat ->
                        viewModel.setCategory(cat)
                        onNavigateTo(Screen.CATALOGO)
                    },
                    onDeleteAccount = { viewModel.deleteAccount { onAccountDeleted() } },
                    onBack = { onNavigateTo(previousScreen) }
                )
            }

            Screen.PRODUCTO -> {
                val prod = selectedProduct
                    ?: cartItems.lastOrNull()?.let { item -> products.find { it.id == item.productId } }
                    ?: products.firstOrNull()
                if (prod != null) {
                    ProductoScreen(
                        product = prod,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        isSaved = savedProductIds.contains(prod.id),
                        onToggleSave = { onResult -> viewModel.toggleSaveProduct(prod.id, onResult) },
                        onAddToCart = { p, s, color, q -> viewModel.addToCart(p, s, q, color) },
                        onBuyNow = { p, s, color, q ->
                            viewModel.addToCart(p, s, q, color)
                            onNavigateTo(Screen.CARRITO)
                        },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        onBack = { onNavigateTo(Screen.CATALOGO) }
                    )
                } else {
                    LaunchedEffect(Unit) { onNavigateTo(Screen.CATALOGO) }
                }
            }

            Screen.CARRITO -> {
                CarritoScreen(
                    cartItems = cartItems,
                    onUpdateQuantity = { id, qty -> viewModel.updateCartQuantity(id, qty) },
                    onRemoveItem = { id -> viewModel.removeFromCart(id) },
                    onNavigateToVerificasion = { sub, disc, ship, tot ->
                        onUpdateCheckoutTotals(sub, disc, ship, tot)
                        onNavigateTo(Screen.VERIFICASION)
                    },
                    onExploreCatalog = {
                        viewModel.setCategory("todo")
                        onNavigateTo(Screen.CATALOGO)
                    },
                    onBack = { onNavigateTo(Screen.PRODUCTO) }
                )
            }

            Screen.VERIFICASION -> {
                VerificasionScreen(
                    cartItems = cartItems,
                    subtotal = checkoutSubtotal,
                    discount = checkoutDiscount,
                    shipping = checkoutShipping,
                    total = checkoutTotal,
                    userProfile = userProfile,
                    onConfirmOrder = { name, email, phone, addr, city, payMethod, paymentProofUri ->
                        val itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" }
                        viewModel.placeOrder(
                            customerName = name,
                            email = email,
                            phone = phone,
                            address = addr,
                            city = city,
                            paymentMethod = payMethod,
                            total = checkoutTotal,
                            itemCount = cartItems.sumOf { it.quantity },
                            itemsSummary = itemsSummary,
                            paymentProofUri = paymentProofUri,
                            onSuccess = { onNavigateTo(Screen.GRACIA) }
                        )
                    },
                    onBack = { onNavigateTo(Screen.CARRITO) }
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
