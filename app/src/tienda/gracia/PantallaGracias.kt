package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.OrderConfirmationDetails

@Composable
fun GraciaScreen(
    orderId: String?,
    isBankTransfer: Boolean = false,
    isCashOnDelivery: Boolean = false,
    orderDetails: OrderConfirmationDetails? = null,
    onNavigateToInicio: () -> Unit,
    onNavigateToPerfil: () -> Unit,
    onNavigateToChats: () -> Unit
) {
    BackHandler { onNavigateToInicio() }

    val displayOrderId = orderDetails?.orderId?.takeIf { it.isNotBlank() }
        ?: orderId?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.gracia_order_id_pending)
    val purchasedItems = orderDetails?.items.orEmpty()
    val fallbackPaymentMethod = stringResource(
        when {
            isBankTransfer -> R.string.gracia_payment_bank_transfer
            isCashOnDelivery -> R.string.gracia_payment_cash_on_delivery
            else -> R.string.gracia_payment_confirmed
        }
    )
    val displayPaymentMethod = orderDetails?.paymentMethod.orEmpty()
        .ifBlank { fallbackPaymentMethod }
    val displayAddress = listOfNotNull(
        orderDetails?.address?.takeIf { it.isNotBlank() },
        orderDetails?.city?.takeIf { it.isNotBlank() }
    ).joinToString(", ")
    val recipientLine = listOfNotNull(
        orderDetails?.customerName?.takeIf { it.isNotBlank() },
        orderDetails?.phone?.takeIf { it.isNotBlank() }
    ).joinToString(" • ")

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .testTag("gracia_screen"),
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
                .widthIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = horizontalPad, vertical = if (isCompact) 14.dp else 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CabeceraConfirmacionCompra(
                isCompact = isCompact,
                isBankTransfer = isBankTransfer,
                isCashOnDelivery = isCashOnDelivery
            )

            Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 20.dp))

            TarjetaReciboPedidoConfirmado(
                displayOrderId = displayOrderId,
                isBankTransfer = isBankTransfer,
                purchasedItems = purchasedItems,
                totalPaid = orderDetails?.total ?: 0.0,
                displayPaymentMethod = displayPaymentMethod,
                recipientLine = recipientLine,
                displayAddress = displayAddress,
                isCompact = isCompact
            )

            Spacer(modifier = Modifier.height(if (isCompact) 18.dp else 22.dp))

            AccionesPostCompra(
                isCompact = isCompact,
                onNavigateToInicio = onNavigateToInicio,
                onNavigateToPerfil = onNavigateToPerfil,
                onNavigateToChats = onNavigateToChats
            )
        }
    }
}
