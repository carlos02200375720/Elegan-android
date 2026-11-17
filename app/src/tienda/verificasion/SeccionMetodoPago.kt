package com.example.ui

import android.net.Uri
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocalShipping
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

internal const val OPCION_PAGO_PAYPAL = "PayPal (Pago en Línea Seguro)"
internal const val OPCION_PAGO_TARJETA = "Tarjeta de Crédito / Débito (Visa / Mastercard)"
internal const val OPCION_PAGO_CONTRAENTREGA = "Pago Contra Entrega (Efectivo al recibir)"
internal const val OPCION_PAGO_TRANSFERENCIA = "Transferencia Bancaria Inmediata"

@Composable
internal fun SeccionMetodoPago(
    paymentOptions: List<String>,
    selectedPayment: String,
    paymentProofUri: Uri?,
    onSelectOption: (String) -> Unit,
    onOpenSelectedAction: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = EleganBurgundy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "3. Método de Pago",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        for (option in paymentOptions) {
            val isSelected = option == selectedPayment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFFF9F3EA) else Color.Transparent)
                    .clickable { onSelectOption(option) }
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { onSelectOption(option) },
                    colors = RadioButtonDefaults.colors(selectedColor = EleganBurgundy)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = EleganCharcoal,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }

        when (selectedPayment) {
            OPCION_PAGO_PAYPAL -> TarjetaAccionMetodoPago(
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = Color(0xFF003087),
                text = "Pagar con tu cuenta o tarjeta vía PayPal",
                testTag = "open_paypal_payment_btn",
                onClick = { onOpenSelectedAction(OPCION_PAGO_PAYPAL) }
            )
            OPCION_PAGO_TARJETA -> TarjetaAccionMetodoPago(
                icon = Icons.Default.CreditCard,
                iconTint = EleganBurgundy,
                text = "Ingresar datos de tarjeta dentro de la app",
                testTag = "open_card_payment_btn",
                onClick = { onOpenSelectedAction(OPCION_PAGO_TARJETA) }
            )
            OPCION_PAGO_CONTRAENTREGA -> TarjetaAccionMetodoPago(
                icon = Icons.Default.LocalShipping,
                iconTint = EleganBurgundy,
                text = "Ver instrucciones y compromiso contra entrega",
                testTag = "open_cash_on_delivery_btn",
                onClick = { onOpenSelectedAction(OPCION_PAGO_CONTRAENTREGA) }
            )
            OPCION_PAGO_TRANSFERENCIA -> TarjetaAccionMetodoPago(
                icon = if (paymentProofUri != null) Icons.Default.CheckCircle else Icons.Default.AccountBalance,
                iconTint = if (paymentProofUri != null) Color(0xFF2E7D32) else EleganBurgundy,
                text = if (paymentProofUri != null) {
                    "Comprobante adjuntado • Ver cuentas"
                } else {
                    "Ver cuentas bancarias y subir comprobante"
                },
                testTag = "open_bank_transfer_sheet_btn",
                onClick = { onOpenSelectedAction(OPCION_PAGO_TRANSFERENCIA) }
            )
        }
    }
}

@Composable
private fun TarjetaAccionMetodoPago(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    testTag: String,
    onClick: () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFFAF6F1),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EleganCharcoal,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}
