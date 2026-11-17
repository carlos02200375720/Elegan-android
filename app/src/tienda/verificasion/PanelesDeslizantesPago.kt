package com.example.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.PayPalOrderSession

@Composable
internal fun PanelesDeslizantesPago(
    showPayPalPage: Boolean,
    showBankTransferPage: Boolean,
    showCardPaymentPage: Boolean,
    showCashOnDeliveryPage: Boolean,
    total: Double,
    customerName: String,
    email: String,
    phone: String,
    address: String,
    city: String,
    cartItems: List<CartItem>,
    bankAccounts: List<BankAccountInfo>,
    paymentProofUri: Uri?,
    isSubmitting: Boolean,
    payPalSession: PayPalOrderSession?,
    isProcessingPayPal: Boolean,
    payPalError: String?,
    onClosePayPal: () -> Unit,
    onCreatePayPalOrder: (onSessionReady: (PayPalOrderSession) -> Unit) -> Unit,
    onCapturePayPalOrder: (paypalOrderId: String) -> Unit,
    onCloseBankTransfer: () -> Unit,
    onCloseCardPayment: () -> Unit,
    onCloseCashOnDelivery: () -> Unit,
    onPickPaymentProof: () -> Unit,
    onRemovePaymentProof: () -> Unit,
    onConfirmTransferPayment: () -> Unit,
    onConfirmCardPayment: (paymentLabel: String) -> Unit,
    onConfirmCashOnDelivery: () -> Unit
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

    AnimatedVisibility(visible = showBankTransferPage, enter = slideEnter, exit = slideExit) {
        TranfereciaScreen(
            total = total,
            bankAccounts = bankAccounts,
            paymentProofUri = paymentProofUri,
            isSubmitting = isSubmitting,
            onBack = onCloseBankTransfer,
            onPickPaymentProof = onPickPaymentProof,
            onRemovePaymentProof = onRemovePaymentProof,
            onConfirmPayment = onConfirmTransferPayment
        )
    }

    AnimatedVisibility(visible = showCardPaymentPage, enter = slideEnter, exit = slideExit) {
        CardPaymentScreen(
            total = total,
            customerName = customerName,
            customerEmail = email,
            itemCount = cartItems.sumOf { it.quantity },
            itemsSummary = cartItems.joinToString(", ") { "${it.quantity}x ${it.name}" },
            isSubmittingOrder = isSubmitting,
            onBack = onCloseCardPayment,
            onPaymentSuccess = { paymentLabel, _ -> onConfirmCardPayment(paymentLabel) }
        )
    }

    AnimatedVisibility(visible = showCashOnDeliveryPage, enter = slideEnter, exit = slideExit) {
        ContraEntregaScreen(
            total = total,
            customerName = customerName,
            phone = phone,
            address = address,
            city = city,
            itemCount = cartItems.sumOf { it.quantity },
            isSubmitting = isSubmitting,
            onBack = onCloseCashOnDelivery,
            onConfirmOrder = onConfirmCashOnDelivery
        )
    }
}
