package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CardPaymentScreen(
    total: Double,
    customerName: String,
    customerEmail: String,
    itemCount: Long,
    itemsSummary: String,
    isSubmittingOrder: Boolean,
    onBack: () -> Unit,
    onPaymentSuccess: (paymentMethodLabel: String, transactionId: String) -> Unit
) {
    BackHandler(enabled = !isSubmittingOrder) { onBack() }

    var cardHolder by remember(customerName) { mutableStateOf(customerName) }
    var cardNumberDigits by remember { mutableStateOf("") }
    var cardExpiryDigits by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val amountFormatted = remember(total) {
        String.format(Locale.US, "%.2f", total.coerceAtLeast(0.01))
    }
    val cardBrand = remember(cardNumberDigits) { detectarMarcaTarjeta(cardNumberDigits) }
    val formattedCardNumber = remember(cardNumberDigits) { formatearNumeroTarjeta(cardNumberDigits) }
    val formattedExpiry = remember(cardExpiryDigits) { formatearVencimientoTarjeta(cardExpiryDigits) }

    fun submitLocalCardPayment() {
        val error = validarDatosTarjeta(cardHolder, cardNumberDigits, cardExpiryDigits, cardCvv)
        if (error != null) {
            scope.launch { snackbarHostState.showSnackbar(error) }
            return
        }
        val last4 = cardNumberDigits.takeLast(4)
        val localTxId = "ELEGAN-${System.currentTimeMillis().toString().takeLast(6)}"
        onPaymentSuccess("$cardBrand •••• $last4", localTxId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("card_payment_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            CabeceraPagoTarjeta(
                amountFormatted = amountFormatted,
                isSubmittingOrder = isSubmittingOrder,
                onBack = onBack
            )
        },
        bottomBar = {
            BarraInferiorPagoTarjeta(
                amountFormatted = amountFormatted,
                isSubmittingOrder = isSubmittingOrder,
                onBack = onBack,
                onConfirm = ::submitLocalCardPayment
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White),
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TarjetaResumenCheckoutSeguro(
                    customerName = customerName,
                    customerEmail = customerEmail,
                    itemCount = itemCount,
                    itemsSummary = itemsSummary
                )

                TarjetaVistaPreviaVisual(
                    cardBrand = cardBrand,
                    formattedCardNumber = formattedCardNumber,
                    cardHolder = cardHolder,
                    formattedExpiry = formattedExpiry
                )

                FormularioCamposTarjeta(
                    cardHolder = cardHolder,
                    onCardHolderChange = { cardHolder = it },
                    formattedCardNumber = formattedCardNumber,
                    onCardNumberDigitsChange = { cardNumberDigits = it },
                    formattedExpiry = formattedExpiry,
                    onCardExpiryDigitsChange = { cardExpiryDigits = it },
                    cardCvv = cardCvv,
                    onCardCvvChange = { cardCvv = it }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isSubmittingOrder) {
                SuperposicionProcesandoPagoTarjeta()
            }
        }
    }
}
