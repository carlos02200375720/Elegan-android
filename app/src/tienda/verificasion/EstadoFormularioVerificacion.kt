package com.example.ui

import android.net.Uri
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
    initialCity: String
) {
    var customerName by mutableStateOf(initialName)
    var email by mutableStateOf(initialEmail)
    var phone by mutableStateOf(initialPhone)
    var address by mutableStateOf(initialAddress)
    var city by mutableStateOf(initialCity)

    val paymentOptions = listOf(
        OPCION_PAGO_TARJETA,
        OPCION_PAGO_CONTRAENTREGA,
        OPCION_PAGO_TRANSFERENCIA
    )
    var selectedPayment by mutableStateOf(paymentOptions[0])
    var isSubmitting by mutableStateOf(false)

    var showBankTransferPage by mutableStateOf(false)
    var showCardPaymentPage by mutableStateOf(false)
    var showCashOnDeliveryPage by mutableStateOf(false)
    var showProofDisclosureDialog by mutableStateOf(false)
    var paymentProofUri by mutableStateOf<Uri?>(null)

    fun isDeliveryFormValid(): Boolean =
        customerName.isNotBlank() && email.isNotBlank() && address.isNotBlank() && city.isNotBlank()

    fun mensajeCampoFaltante(option: String): String = when (option) {
        OPCION_PAGO_TARJETA -> "Completa los datos de entrega para continuar con el pago con tarjeta."
        OPCION_PAGO_CONTRAENTREGA -> "Completa los datos de entrega para ver las instrucciones de pago contra entrega."
        else -> "Completa los datos de entrega para continuar con la transferencia."
    }
}

@Composable
internal fun rememberEstadoFormularioVerificacion(
    userProfile: UserProfile?
): EstadoFormularioVerificacion {
    val context = LocalContext.current
    val user = MobileSession.user(context)
    val estado = remember {
        EstadoFormularioVerificacion(
            initialName = user?.name ?: userProfile?.displayName ?: "",
            initialEmail = user?.email ?: userProfile?.email ?: "",
            initialPhone = userProfile?.phone ?: "",
            initialAddress = userProfile?.address ?: "",
            initialCity = userProfile?.city ?: ""
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
