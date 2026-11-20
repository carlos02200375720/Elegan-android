package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

internal const val OPCION_PAGO_TRANSFERENCIA = "Transferencia bancaria"
internal const val OPCION_PAGO_CONTRA_ENTREGA = "Pago contra entrega"

@Composable
internal fun SeccionMetodoPago(
    paymentOptions: List<String>,
    selectedPayment: String,
    onSelectOption: (String) -> Unit,
    onOpenSelectedAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = EleganBurgundy)
            Spacer(modifier = Modifier.width(8.dp))
            Text("3. Método de Pago", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EleganCharcoal))
        }
        Spacer(modifier = Modifier.height(12.dp))
        paymentOptions.forEach { option ->
            val selected = option == selectedPayment
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(if (selected) Color(0xFFF9F3EA) else Color.Transparent)
                    .clickable { onSelectOption(option) }.padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = selected, onClick = { onSelectOption(option) }, colors = RadioButtonDefaults.colors(selectedColor = EleganBurgundy))
                Spacer(modifier = Modifier.width(6.dp))
                Text(option, style = MaterialTheme.typography.bodyMedium.copy(color = EleganCharcoal, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal))
            }
        }
        if (selectedPayment == OPCION_PAGO_TRANSFERENCIA) {
            ActionCard(Icons.Default.AccountBalance, "Ver cuentas bancarias y subir comprobante", "open_bank_transfer_btn") { onOpenSelectedAction(OPCION_PAGO_TRANSFERENCIA) }
        } else if (selectedPayment == OPCION_PAGO_CONTRA_ENTREGA) {
            ActionCard(Icons.Default.Payments, "Continuar con pago al recibir el pedido", "open_cod_payment_btn") { onOpenSelectedAction(OPCION_PAGO_CONTRA_ENTREGA) }
        }
    }
}

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tag: String, onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(10.dp))
    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFFAF6F1), border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag(tag)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Start) {
            Icon(icon, contentDescription = null, tint = EleganBurgundy, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text, style = MaterialTheme.typography.bodySmall.copy(color = EleganCharcoal, fontWeight = FontWeight.SemiBold))
        }
    }
}
