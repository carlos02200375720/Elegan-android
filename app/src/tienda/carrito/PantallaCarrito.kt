package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import kotlinx.coroutines.launch

@Composable
fun CarritoScreen(
    cartItems: List<CartItem>,
    onUpdateQuantity: (String, Long) -> Unit,
    onRemoveItem: (String) -> Unit,
    onNavigateToVerificasion: (Double, Double, Double, Double) -> Unit,
    onExploreCatalog: () -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    var couponCode by remember { mutableStateOf("") }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val subtotal by remember(cartItems) {
        derivedStateOf { cartItems.sumOf { it.price * it.quantity } }
    }
    val discountAmount by remember(subtotal, discountPercent) {
        derivedStateOf { subtotal * discountPercent }
    }
    val shippingCost by remember(subtotal) {
        derivedStateOf { if (subtotal >= 100.0 || subtotal == 0.0) 0.0 else 12.0 }
    }
    val total by remember(subtotal, discountAmount, shippingCost) {
        derivedStateOf { (subtotal - discountAmount + shippingCost).coerceAtLeast(0.0) }
    }
    val totalItems by remember(cartItems) {
        derivedStateOf { cartItems.sumOf { it.quantity } }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CabeceraCarrito(
                totalItems = totalItems,
                onBack = onBack
            )
        },
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                BarraInferiorTotalCarrito(
                    total = total,
                    onContinue = {
                        onNavigateToVerificasion(subtotal, discountAmount, shippingCost, total)
                    }
                )
            }
        },
        containerColor = Color.White
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 16.dp
            }

            if (cartItems.isEmpty()) {
                EstadoVacioCarrito(
                    isCompact = isCompact,
                    onExploreCatalog = onExploreCatalog
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .testTag("cart_items_list"),
                    contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(2.dp)) }

                    items(cartItems, key = { it.id }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrease = { onUpdateQuantity(item.id, item.quantity + 1) },
                            onDecrease = {
                                if (item.quantity > 1) {
                                    onUpdateQuantity(item.id, item.quantity - 1)
                                } else {
                                    onRemoveItem(item.id)
                                }
                            },
                            onRemove = { onRemoveItem(item.id) }
                        )
                    }

                    item {
                        TarjetaCuponCarrito(
                            couponCode = couponCode,
                            onCouponCodeChange = { couponCode = it },
                            discountPercent = discountPercent,
                            isCompact = isCompact,
                            onApplyCoupon = {
                                if (couponCode == "ELEGAN10" || couponCode == "VIP") {
                                    discountPercent = 0.10
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡Cupón aplicado: 10% de descuento!")
                                    }
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Cupón inválido. Prueba con ELEGAN10")
                                    }
                                }
                            }
                        )
                    }

                    item {
                        TarjetaResumenPedidoCarrito(
                            subtotal = subtotal,
                            discountAmount = discountAmount,
                            shippingCost = shippingCost,
                            total = total,
                            isCompact = isCompact
                        )
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}
