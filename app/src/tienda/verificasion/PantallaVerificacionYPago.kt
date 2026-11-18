package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CartItem
import com.example.data.model.PayPalOrderSession
import com.example.data.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun VerificasionScreen(
    cartItems: List<CartItem>,
    subtotal: Double,
    discount: Double,
    shipping: Double,
    total: Double,
    userProfile: UserProfile?,
    payPalSession: PayPalOrderSession? = null,
    isProcessingPayPal: Boolean = false,
    payPalError: String? = null,
    onCreatePayPalOrder: (
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        onSessionReady: (PayPalOrderSession) -> Unit
    ) -> Unit = { _, _, _, _, _, _ -> },
    onCapturePayPalOrder: (
        paypalOrderId: String,
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String
    ) -> Unit = { _, _, _, _, _, _ -> },
    onClearPayPalState: () -> Unit = {},
    onBack: () -> Unit
) {
    val estado = rememberEstadoFormularioVerificacion(userProfile = userProfile)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun openPayPalCheckout(missingMsg: String) {
        if (!estado.isDeliveryFormValid()) {
            scope.launch { snackbarHostState.showSnackbar(missingMsg) }
            return
        }
        estado.showPayPalPage = true
    }

    BackHandler(enabled = !estado.showPayPalPage) { onBack() }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = { CabeceraVerificacion(onBack = onBack) },
            bottomBar = {
                BarraInferiorConfirmarPago(
                    total = total,
                    isSubmitting = isProcessingPayPal,
                    onConfirmClick = {
                        openPayPalCheckout("Por favor completa los datos de entrega requeridos.")
                    }
                )
            },
            containerColor = Color.White
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                val isCompact = maxWidth < 350.dp
                val horizontalPad = when {
                    isCompact -> 12.dp
                    maxWidth >= 600.dp -> 24.dp
                    else -> 16.dp
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize().widthIn(max = 680.dp).testTag("verificasion_screen"),
                    contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(if (isCompact) 14.dp else 18.dp)
                ) {
                    item { BannerSeguridadSsl() }
                    item {
                        SeccionDireccionEntrega(
                            estado.customerName, { estado.customerName = it },
                            estado.email, { estado.email = it },
                            estado.phone, { estado.phone = it },
                            estado.address, { estado.address = it },
                            estado.city, { estado.city = it }
                        )
                    }
                    item { HorizontalDivider(color = Color(0xFFF0EAE4)) }
                    item { SeccionResumenArticulos(cartItems, subtotal, discount, shipping, total) }
                    item { HorizontalDivider(color = Color(0xFFF0EAE4)) }
                    item {
                        SeccionMetodoPago(
                            paymentOptions = estado.paymentOptions,
                            selectedPayment = estado.selectedPayment,
                            onSelectOption = { opt ->
                                estado.selectedPayment = opt
                                openPayPalCheckout(estado.mensajeCampoFaltante())
                            },
                            onOpenSelectedAction = {
                                openPayPalCheckout("Por favor completa los datos de entrega requeridos.")
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        PanelesDeslizantesPago(
            showPayPalPage = estado.showPayPalPage,
            total = total,
            customerName = estado.customerName,
            email = estado.email,
            cartItems = cartItems,
            payPalSession = payPalSession,
            isProcessingPayPal = isProcessingPayPal,
            payPalError = payPalError,
            onClosePayPal = {
                estado.showPayPalPage = false
                onClearPayPalState()
            },
            onCreatePayPalOrder = { onSessionReady ->
                onCreatePayPalOrder(
                    estado.customerName,
                    estado.email,
                    estado.phone,
                    estado.address,
                    estado.city,
                    onSessionReady
                )
            },
            onCapturePayPalOrder = { paypalOrderId ->
                onCapturePayPalOrder(
                    paypalOrderId,
                    estado.customerName,
                    estado.email,
                    estado.phone,
                    estado.address,
                    estado.city
                )
            }
        )
    }
}
