package com.example.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun SeccionEntregaPerfil(
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    isEditingDelivery: Boolean,
    onStartEdit: () -> Unit,
    onSaveClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = EleganBurgundy)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mis Datos de Entrega",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
        )
    }
    Spacer(modifier = Modifier.height(14.dp))

    if (!isEditingDelivery && (phone.isNotBlank() || address.isNotBlank() || city.isNotBlank())) {
        TarjetaResumenEntregaPerfil(
            phone = phone,
            address = address,
            city = city,
            onEditClick = onStartEdit
        )
    } else {
        CampoEntregaPerfil(phone, onPhoneChange, "Teléfono de Contacto", "profile_phone_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaPerfil(address, onAddressChange, "Dirección Habitual", "profile_address_input")
        Spacer(modifier = Modifier.height(10.dp))
        CampoEntregaPerfil(city, onCityChange, "Ciudad / Provincia", "profile_city_input")
        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onSaveClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = EleganBurgundy,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("save_profile_btn")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Guardar Cambios", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CampoEntregaPerfil(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth().testTag(testTag),
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EleganBurgundy,
            unfocusedBorderColor = Color(0xFFDDD5CE),
            focusedTextColor = EleganCharcoal,
            unfocusedTextColor = EleganCharcoal
        )
    )
}
