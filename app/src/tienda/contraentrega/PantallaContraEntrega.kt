package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ContraEntregaScreen(
    total: Double,
    customerName: String,
    phone: String,
    address: String,
    city: String,
    itemCount: Long,
    message: String = "",
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onConfirmOrder: () -> Unit
) {
    BackHandler(enabled = !isSubmitting) { onBack() }

    var hasAcceptedResponsibility by remember { mutableStateOf(false) }
    val amountFormatted = remember(total) {
        formatearMontoDecimal(total.coerceAtLeast(0.0))
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("contraentrega_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            CabeceraContraEntrega(
                amountFormatted = amountFormatted,
                isSubmitting = isSubmitting,
                onBack = onBack
            )
        },
        bottomBar = {
            BarraInferiorContraEntrega(
                hasAcceptedResponsibility = hasAcceptedResponsibility,
                isSubmitting = isSubmitting,
                onConfirmOrder = onConfirmOrder
            )
        }
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
                else -> 20.dp
            }
            val cardPad = if (isCompact) 12.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 14.dp)
            ) {
                TarjetaExplicacionContraEntrega(cardPad = cardPad)
                TarjetaPasosContraEntrega(amountFormatted = amountFormatted)
                TarjetaAvisoCompromisoContraEntrega(message = message)
                TarjetaDatosEntregaContraEntrega(
                    customerName = customerName,
                    phone = phone,
                    address = address,
                    city = city,
                    itemCount = itemCount,
                    amountFormatted = amountFormatted
                )
                AceptacionCompromisoContraEntrega(
                    hasAcceptedResponsibility = hasAcceptedResponsibility,
                    isSubmitting = isSubmitting,
                    onCheckedChange = { hasAcceptedResponsibility = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
