package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import com.example.data.model.CartItem
import com.example.data.model.PayPalOrderSession

@Composable
internal fun PanelesDeslizantesPago(
    showPayPalPage: Boolean,
    total: Double,
    customerName: String,
    email: String,
    cartItems: List<CartItem>,
    payPalSession: PayPalOrderSession?,
    isProcessingPayPal: Boolean,
    payPalError: String?,
    onClosePayPal: () -> Unit,
    onCreatePayPalOrder: (onSessionReady: (PayPalOrderSession) -> Unit) -> Unit,
    onCapturePayPalOrder: (paypalOrderId: String) -> Unit
) {
    val slideEnter = slideInHorizontally(
        initialOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 320)
    )
    val slideExit = slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(durationMillis = 280)
    )

    AnimatedVisibility(visible = showPayPalPage, enter = slideEnter, exit = slideExit) {
        PantallaPagoPayPal(
            total = total,
            customerName = customerName,
            customerEmail = email,
            itemCount = cartItems.sumOf { it.quantity },
            itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" },
            payPalSession = payPalSession,
            isProcessing = isProcessingPayPal,
            errorMessage = payPalError,
            onBack = onClosePayPal,
            onCreatePayPalOrder = onCreatePayPalOrder,
            onCapturePayPalOrder = onCapturePayPalOrder
        )
    }
}
