package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun BannerSeguridadSsl() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE8F5E9))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Transacción 100% segura y encriptada por Elegan SSL",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
internal fun SeccionDireccionEntrega(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, contentDescription = null, tint = EleganBurgundy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "1. Dirección de Entrega",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        CampoEntregaVerificacion(customerName, onCustomerNameChange, "Nombre y Apellidos *", "checkout_name_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaVerificacion(email, onEmailChange, "Correo Electrónico *", "checkout_email_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaVerificacion(phone, onPhoneChange, "Teléfono de Contacto *", "checkout_phone_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaVerificacion(address, onAddressChange, "Calle, Número y Apartamento *", "checkout_address_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaVerificacion(city, onCityChange, "Ciudad y Código Postal *", "checkout_city_input")
    }
}

@Composable
private fun CampoEntregaVerificacion(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.Black,
            unfocusedTextColor = Color.Black,
            cursorColor = EleganBurgundy,
            focusedBorderColor = EleganBurgundy,
            unfocusedBorderColor = Color(0xFFDDD5CE),
            focusedLabelColor = EleganBurgundy,
            unfocusedLabelColor = Color(0xFF5F5A56),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
