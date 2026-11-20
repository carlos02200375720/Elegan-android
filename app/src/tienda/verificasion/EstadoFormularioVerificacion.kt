package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.UserProfile
import com.example.data.remote.MobileSession

@Stable
internal class EstadoFormularioVerificacion(
    initialName: String,
    initialEmail: String,
    initialPhone: String,
    initialAddress: String,
    initialCity: String,
    initialShowTransferPage: Boolean = false
) {
    var customerName by mutableStateOf(initialName)
    var email by mutableStateOf(initialEmail)
    var phone by mutableStateOf(initialPhone)
    var address by mutableStateOf(initialAddress)
    var city by mutableStateOf(initialCity)

    val paymentOptions = listOf(OPCION_PAGO_TRANSFERENCIA, OPCION_PAGO_CONTRA_ENTREGA)
    var selectedPayment by mutableStateOf(paymentOptions[0])
    var showTransferPage by mutableStateOf(initialShowTransferPage)
    var showCashOnDeliveryPage by mutableStateOf(false)

    fun isDeliveryFormValid(): Boolean =
        customerName.isNotBlank() &&
            email.isNotBlank() &&
            phone.isNotBlank() &&
            address.isNotBlank() &&
            city.isNotBlank()

    fun mensajeCampoFaltante(): String {
        val faltantes = buildList {
            if (customerName.isBlank()) add("nombre")
            if (email.isBlank()) add("correo")
            if (phone.isBlank()) add("teléfono")
            if (address.isBlank()) add("dirección")
            if (city.isBlank()) add("ciudad")
        }
        return if (faltantes.isEmpty()) {
            "Completa los datos de entrega para continuar con tu pedido."
        } else {
            "Completa los campos obligatorios: ${faltantes.joinToString(", ")}."
        }
    }
}

@Composable
internal fun rememberEstadoFormularioVerificacion(
    userProfile: UserProfile?,
    restoredTransferLock: EstadoBloqueoTransferencia? = null
): EstadoFormularioVerificacion {
    val context = LocalContext.current
    val user = MobileSession.user(context)
    val estado = remember {
        EstadoFormularioVerificacion(
            initialName = restoredTransferLock?.customerName?.takeIf { it.isNotBlank() }
                ?: user?.name ?: userProfile?.displayName ?: "",
            initialEmail = restoredTransferLock?.email?.takeIf { it.isNotBlank() }
                ?: user?.email ?: userProfile?.email ?: "",
            initialPhone = restoredTransferLock?.phone?.takeIf { it.isNotBlank() }
                ?: userProfile?.phone ?: "",
            initialAddress = restoredTransferLock?.address?.takeIf { it.isNotBlank() }
                ?: userProfile?.address ?: "",
            initialCity = restoredTransferLock?.city?.takeIf { it.isNotBlank() }
                ?: userProfile?.city ?: "",
            initialShowTransferPage = restoredTransferLock != null
        )
    }

    LaunchedEffect(userProfile) {
        userProfile?.let { profile ->
            if (estado.customerName.isBlank() && profile.displayName.isNotBlank()) estado.customerName = profile.displayName
            if (estado.email.isBlank() && profile.email.isNotBlank()) estado.email = profile.email
            if (estado.phone.isBlank() && profile.phone.isNotBlank()) estado.phone = profile.phone
            if (estado.address.isBlank() && profile.address.isNotBlank()) estado.address = profile.address
            if (estado.city.isBlank() && profile.city.isNotBlank()) estado.city = profile.city
        }
    }

    return estado
}
