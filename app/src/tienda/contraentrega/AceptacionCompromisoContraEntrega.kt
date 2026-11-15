package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.example.ui.theme.EleganCharcoal

@Composable
fun AceptacionCompromisoContraEntrega(
    hasAcceptedResponsibility: Boolean,
    isSubmitting: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (hasAcceptedResponsibility) Color(0xFFF9F3EA) else Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasAcceptedResponsibility) EleganBurgundy else Color(0xFFD9D0C7)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSubmitting) {
                onCheckedChange(!hasAcceptedResponsibility)
            }
            .testTag("contraentrega_accept_checkbox")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = hasAcceptedResponsibility,
                onCheckedChange = { if (!isSubmitting) onCheckedChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = EleganBurgundy,
                    uncheckedColor = Color(0xFF8C8279)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Handshake,
                    contentDescription = null,
                    tint = if (hasAcceptedResponsibility) EleganBurgundy else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Entiendo cómo funciona el servicio contra entrega, la inversión en producto y logística de Elegan, y confirmo mi compromiso responsable de recibir y pagar el pedido.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EleganCharcoal,
                        fontWeight = if (hasAcceptedResponsibility) FontWeight.SemiBold else FontWeight.Normal,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }

    if (!hasAcceptedResponsibility) {
        Text(
            text = "Marca la casilla de compromiso responsable para habilitar la confirmación del pedido.",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF7A7A7A)
            )
        )
    }
}
