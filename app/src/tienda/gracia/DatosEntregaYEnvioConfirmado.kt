package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

private data class ResumenEnvioSeleccionado(
    val name: String,
    val price: Double,
    val deliveryTime: String,
    val origin: String
)

@Composable
fun DatosEntregaYEnvioConfirmado(
    displayPaymentMethod: String,
    recipientLine: String,
    displayAddress: String,
    purchasedItems: List<CartItem> = emptyList(),
    shippingCost: Double = purchasedItems.sumOf { it.shippingPrice },
    hasOrderDetails: Boolean = purchasedItems.isNotEmpty()
) {
    val selectedShippings = purchasedItems
        .mapNotNull { item ->
            val name = item.shippingMethodName.trim()
            val deliveryTime = item.shippingDeliveryTime.trim()
            val origin = item.shippingOrigin.trim()
            if (name.isBlank() && deliveryTime.isBlank() && origin.isBlank()) {
                null
            } else {
                ResumenEnvioSeleccionado(
                    name = name,
                    price = item.shippingPrice,
                    deliveryTime = deliveryTime,
                    origin = origin
                )
            }
        }
        .distinct()

    FilaDetalleConfirmacion(
        icon = Icons.Default.Payments,
        tint = EleganBurgundy,
        label = stringResource(R.string.gracia_payment_method_label),
        primaryValue = displayPaymentMethod,
        verticalAlignment = Alignment.Top,
        valueTestTag = "gracia_payment_method"
    )

    if (displayAddress.isNotBlank() || recipientLine.isNotBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = stringResource(R.string.gracia_delivery_address_label),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                if (recipientLine.isNotBlank()) {
                    Text(
                        text = recipientLine,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        )
                    )
                }
                if (displayAddress.isNotBlank()) {
                    Text(
                        text = displayAddress,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF555555),
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }
    }

    if (selectedShippings.isNotEmpty() || hasOrderDetails) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.testTag("gracia_shipping_summary")
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.gracia_shipping_summary_label),
                    style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                )
                if (selectedShippings.isNotEmpty()) {
                    selectedShippings.forEachIndexed { index, shipping ->
                        val titleLine = if (shipping.name.isNotBlank()) {
                            "${shipping.name} • ${formatearEnvioRd(shipping.price)}"
                        } else {
                            stringResource(R.string.gracia_shipping_cost_line, formatearEnvioRd(shipping.price))
                        }
                        Text(
                            text = titleLine,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            ),
                            modifier = if (index == 0) Modifier.testTag("gracia_shipping_method") else Modifier
                        )
                        if (shipping.deliveryTime.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.gracia_shipping_delivery_time, shipping.deliveryTime),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF555555),
                                    lineHeight = 18.sp
                                ),
                                modifier = if (index == 0) Modifier.testTag("gracia_delivery_time") else Modifier
                            )
                        }
                        if (shipping.origin.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.gracia_shipping_origin, shipping.origin),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF555555),
                                    lineHeight = 18.sp
                                )
                            )
                        }
                    }
                    if (selectedShippings.size > 1 || (purchasedItems.size > 1 && shippingCost != selectedShippings.first().price)) {
                        Text(
                            text = stringResource(R.string.gracia_shipping_total_line, formatearEnvioRd(shippingCost)),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = EleganCharcoal
                            )
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.gracia_shipping_cost_line, formatearEnvioRd(shippingCost)),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        modifier = Modifier.testTag("gracia_shipping_method")
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    FilaDetalleConfirmacion(
        icon = Icons.Default.Diamond,
        tint = EleganGold,
        label = stringResource(R.string.gracia_packaging_label),
        primaryValue = stringResource(R.string.gracia_packaging_value),
        verticalAlignment = Alignment.CenterVertically
    )
}

@Composable
private fun FilaDetalleConfirmacion(
    icon: ImageVector,
    tint: Color,
    label: String,
    primaryValue: String,
    verticalAlignment: Alignment.Vertical,
    valueTestTag: String? = null
) {
    Row(verticalAlignment = verticalAlignment) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                ),
                modifier = if (valueTestTag != null) Modifier.testTag(valueTestTag) else Modifier
            )
        }
    }
}
