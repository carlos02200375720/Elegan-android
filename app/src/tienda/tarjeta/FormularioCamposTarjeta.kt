package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun FormularioCamposTarjeta(
    cardHolder: String,
    onCardHolderChange: (String) -> Unit,
    formattedCardNumber: String,
    onCardNumberDigitsChange: (String) -> Unit,
    formattedExpiry: String,
    onCardExpiryDigitsChange: (String) -> Unit,
    cardCvv: String,
    onCardCvvChange: (String) -> Unit
) {
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = EleganBurgundy,
        unfocusedBorderColor = Color(0xFFDDD5CE)
    )

    OutlinedTextField(
        value = cardHolder,
        onValueChange = onCardHolderChange,
        label = { Text("Nombre en la Tarjeta *") },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_holder_input")
    )

    OutlinedTextField(
        value = formattedCardNumber,
        onValueChange = { input ->
            onCardNumberDigitsChange(input.filter { it.isDigit() }.take(16))
        },
        label = { Text("Número de Tarjeta *") },
        placeholder = { Text("0000 0000 0000 0000") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(10.dp),
        colors = fieldColors,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("card_number_input")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = formattedExpiry,
            onValueChange = { input ->
                onCardExpiryDigitsChange(input.filter { it.isDigit() }.take(4))
            },
            label = { Text("Vence (MM/AA) *") },
            placeholder = { Text("12/28") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors,
            modifier = Modifier
                .weight(1f)
                .testTag("card_expiry_input")
        )

        OutlinedTextField(
            value = cardCvv,
            onValueChange = { input ->
                onCardCvvChange(input.filter { it.isDigit() }.take(4))
            },
            label = { Text("CVV *") },
            placeholder = { Text("123") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(10.dp),
            colors = fieldColors,
            modifier = Modifier
                .weight(1f)
                .testTag("card_cvv_input")
        )
    }
}

@Composable
fun SuperposicionProcesandoPagoTarjeta() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = EleganBurgundy,
                modifier = Modifier.size(40.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Pago aprobado. Registrando tu pedido...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
        }
    }
}
