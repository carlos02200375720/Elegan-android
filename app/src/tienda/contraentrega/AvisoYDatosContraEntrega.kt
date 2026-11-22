package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun TarjetaAvisoCompromisoContraEntrega(message: String = "") {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFF8EE),
        border = BorderStroke(1.2.dp, EleganGold.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contraentrega_responsibility_notice")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = EleganBurgundyDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compromiso y Responsabilidad",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundyDark
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message.ifBlank {
                    "Confirma tu pedido con responsabilidad. Verifica que tu dirección y teléfono estén correctos y asegúrate de estar disponible para recibirlo y pagar al momento de la entrega."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EleganCharcoal,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

@Composable
fun TarjetaDatosEntregaContraEntrega(
    customerName: String,
    phone: String,
    address: String,
    city: String,
    itemCount: Long,
    amountFormatted: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Datos de Entrega Confirmados",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cliente: ${customerName.ifBlank { "No especificado" }}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EleganCharcoal,
                    fontWeight = FontWeight.SemiBold
                )
            )
            if (phone.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Teléfono: $phone",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Dirección: ${listOf(address, city).filter { it.isNotBlank() }.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555))
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$itemCount artículo(s) • Total a pagar al recibir: RD$ $amountFormatted",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = EleganBurgundy,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}
