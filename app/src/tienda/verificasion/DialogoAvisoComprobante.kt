package com.example.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun DialogoAvisoComprobante(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "Subir comprobante de pago",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal,
                    fontFamily = FontFamily.Serif
                )
            )
        },
        text = {
            Text(
                text = "Selecciona la foto de tu comprobante de transferencia para verificar tu pago. La imagen seleccionada se utilizará únicamente para validar este pedido y se mantendrá privada.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4A4A4A),
                    lineHeight = 19.sp
                )
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EleganBurgundy,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_proof_disclosure_btn")
            ) {
                Text("Seleccionar foto", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_proof_disclosure_btn")
            ) {
                Text("Cancelar", color = EleganCharcoal)
            }
        }
    )
}
