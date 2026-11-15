package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun TarjetaResumenEntregaPerfil(
    phone: String,
    address: String,
    city: String,
    onEditClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("saved_delivery_summary_card"),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(36.dp)
                    .testTag("edit_delivery_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editar dirección de entrega",
                    tint = EleganBurgundy,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dirección de Entrega Guardada",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundyDark
                    )
                )

                if (phone.isNotBlank()) {
                    FilaDatoEntrega(Icons.Default.Phone, "Teléfono", phone, alignTop = false)
                }
                if (address.isNotBlank()) {
                    FilaDatoEntrega(Icons.Default.LocationOn, "Dirección", address, alignTop = true)
                }
                if (city.isNotBlank()) {
                    FilaDatoEntrega(Icons.Default.LocationCity, "Ciudad / Provincia", city, alignTop = false)
                }
            }
        }
    }
}

@Composable
private fun FilaDatoEntrega(
    icon: ImageVector,
    label: String,
    value: String,
    alignTop: Boolean
) {
    Row(verticalAlignment = if (alignTop) Alignment.Top else Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EleganBurgundy,
            modifier = (if (alignTop) Modifier.padding(top = 2.dp) else Modifier).size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF7A726B))
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = EleganCharcoal,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}
