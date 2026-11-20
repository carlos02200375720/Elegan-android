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
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun DialogoEliminarCuenta(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text(
                text = "Eliminar mi cuenta",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal,
                    fontFamily = FontFamily.Serif
                )
            )
        },
        text = {
            Text(
                text = "¿Estás seguro? Se eliminarán tu cuenta, perfil, carrito, publicaciones guardadas, conversaciones y comprobantes. Los registros de compra que deban conservarse por obligaciones legales se desvincularán de tu cuenta y se anonimizarán.",
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
                    containerColor = Color(0xFFC62828),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_account_btn")
            ) {
                Text("Eliminar cuenta", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_account_btn")
            ) {
                Text("Cancelar", color = EleganCharcoal)
            }
        }
    )
}
