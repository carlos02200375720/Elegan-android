package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import kotlinx.coroutines.launch

@Composable
fun CarritoScreen(
    cartItems: List<CartItem>,
    cartError: String? = null,
    onRetrySync: () -> Unit = {},
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

    LaunchedEffect(cartError) {
        if (!cartError.isNullOrBlank()) {
            snackbarHostState.showSnackbar(cartError)
        }
    }

    val subtotal by remember(cartItems) {
        derivedStateOf { cartItems.sumOf { it.price * it.quantity } }
    }
    val discountAmount by remember(subtotal, discountPercent) {
        derivedStateOf { subtotal * discountPercent }
    }
    val shippingCost by remember(subtotal) {
        derivedStateOf { cartItems.sumOf { it.shippingPrice * it.quantity } }
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

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
            ) {
                if (!cartError.isNullOrBlank()) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                        border = BorderStroke(1.dp, Color(0xFFFFCCCC)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = horizontalPad, vertical = 6.dp)
                            .testTag("cart_sync_error_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cartError,
                                color = Color(0xFFD32F2F),
                                fontSize = 12.5.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onRetrySync) {
                                Text(
                                    text = "Reintentar",
                                    color = Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                )
                            }
                        }
                    }
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
}
