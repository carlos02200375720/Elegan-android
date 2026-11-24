package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

@Composable
fun TarjetaResumenPedidoCarrito(
    subtotal: Double,
    discountAmount: Double,
    shippingCost: Double,
    total: Double,
    isCompact: Boolean,
    appliedCouponCode: String = ""
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isUltraCompact = maxWidth < 310.dp
            val cardPad = when {
                isUltraCompact -> 10.dp
                isCompact -> 12.dp
                else -> 16.dp
            }

            Column(modifier = Modifier.padding(cardPad)) {
                Text(
                    text = "Resumen del Pedido",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontSize = if (isUltraCompact) 15.sp else 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                SummaryRow(label = "Subtotal", value = formatearPrecioRd(subtotal))

                if (discountAmount > 0.0) {
                    val discountLabel = if (appliedCouponCode.isNotBlank()) {
                        "Descuento (${appliedCouponCode.trim()})"
                    } else {
                        "Descuento"
                    }
                    SummaryRow(
                        label = discountLabel,
                        value = formatearDescuentoRd(discountAmount),
                        valueColor = Color(0xFF1E7E34)
                    )
                }

                SummaryRow(
                    label = "Envío",
                    value = formatearEnvioRd(shippingCost),
                    valueColor = if (shippingCost == 0.0) Color(0xFF1E7E34) else EleganCharcoal
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFFE5DDD5))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Final",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontSize = if (isUltraCompact) 14.5.sp else 16.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatearPrecioRd(total),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EleganBurgundyDark,
                            fontSize = when {
                                isUltraCompact -> 17.5.sp
                                isCompact -> 19.sp
                                else -> 21.sp
                            }
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    valueColor: Color = EleganCharcoal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}
