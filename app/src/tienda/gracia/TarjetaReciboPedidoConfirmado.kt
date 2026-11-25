package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundyDark

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TarjetaReciboPedidoConfirmado(
    displayOrderId: String,
    isBankTransfer: Boolean,
    purchasedItems: List<CartItem>,
    totalPaid: Double,
    displayPaymentMethod: String,
    recipientLine: String,
    displayAddress: String,
    isCompact: Boolean,
    subtotal: Double = purchasedItems.sumOf { it.price * it.quantity },
    discount: Double = 0.0,
    shippingCost: Double = purchasedItems.sumOf { it.shippingPrice },
    hasOrderDetails: Boolean = purchasedItems.isNotEmpty(),
    paymentProofUri: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEDE6DF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(if (isCompact) 14.dp else 18.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.gracia_order_code_label),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(end = 8.dp)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF6EFEA),
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        text = displayOrderId,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundyDark
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("gracia_order_id")
                    )
                }
            }

            if (isBankTransfer) {
                Spacer(modifier = Modifier.height(14.dp))
                BannerEstadoTransferencia()
            }

            if (!paymentProofUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                TarjetaComprobantePagoConfirmado(paymentProofUri = paymentProofUri)
            }

            if (purchasedItems.isNotEmpty()) {
                ResumenProductosPedidoConfirmado(
                    purchasedItems = purchasedItems,
                    subtotal = subtotal,
                    discount = discount,
                    shippingCost = shippingCost,
                    totalPaid = totalPaid
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF0EAE4))
            Spacer(modifier = Modifier.height(14.dp))

            DatosEntregaYEnvioConfirmado(
                displayPaymentMethod = displayPaymentMethod,
                recipientLine = recipientLine,
                displayAddress = displayAddress,
                purchasedItems = purchasedItems,
                shippingCost = shippingCost,
                hasOrderDetails = hasOrderDetails
            )
        }
    }
}
