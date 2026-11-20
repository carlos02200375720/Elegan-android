package com.example.ui

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun DatosEntregaYEnvioConfirmado(
    displayPaymentMethod: String,
    recipientLine: String,
    displayAddress: String
) {
    FilaDetalleConfirmacion(
        icon = Icons.Default.Payments,
        tint = EleganBurgundy,
        label = "Método de Pago",
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
                    text = "Dirección de Entrega",
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

    Spacer(modifier = Modifier.height(14.dp))

    FilaDetalleConfirmacion(
        icon = Icons.Default.LocalShipping,
        tint = EleganBurgundy,
        label = "Entrega Estimada",
        primaryValue = "2 a 3 días hábiles (Express)",
        verticalAlignment = Alignment.CenterVertically
    )

    Spacer(modifier = Modifier.height(14.dp))

    FilaDetalleConfirmacion(
        icon = Icons.Default.Diamond,
        tint = EleganGold,
        label = "Embalaje Exclusivo",
        primaryValue = "Caja rígida protegida con sello de lacre",
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
