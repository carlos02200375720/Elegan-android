package com.example.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.RestablecerContrasenaRequest
import com.example.data.remote.SolicitarRecuperacionRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

@Composable
internal fun DialogoRecuperacionContrasena(
    onDismiss: () -> Unit,
    onPasswordReset: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text(if (codeSent) "Restablecer contraseña" else "Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!codeSent) {
                    Text("Escribe el correo de tu cuenta. Si está registrada con correo y contraseña, recibirás un código.")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Correo electrónico") },
                        singleLine = true
                    )
                } else {
                    Text("Introduce el código de 6 dígitos que enviamos a tu correo y crea una contraseña nueva.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6); error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Código de 6 dígitos") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; error = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nueva contraseña (mínimo 8 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
                info?.let { Text(it) }
                error?.let { Text(it) }
                if (loading) {
                    Spacer(Modifier.height(4.dp))
                    CircularProgressIndicator()
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                onClick = {
                    val correo = email.trim()
                    when {
                        !codeSent && (!correo.contains("@") || !correo.contains(".")) ->
                            error = "Escribe un correo electrónico válido."
                        codeSent && code.length != 6 ->
                            error = "Escribe el código de 6 dígitos."
                        codeSent && newPassword.length < 8 ->
                            error = "La nueva contraseña debe tener al menos 8 caracteres."
                        else -> {
                            loading = true
                            error = null
                            info = null
                            scope.launch {
                                try {
                                    withContext(Dispatchers.IO) {
                                        if (!codeSent) {
                                            ApiAutenticacionMovil.service.requestPasswordReset(
                                                SolicitarRecuperacionRequest(correo)
                                            )
                                        } else {
                                            ApiAutenticacionMovil.service.resetPassword(
                                                RestablecerContrasenaRequest(correo, code, newPassword)
                                            )
                                        }
                                    }
                                    if (!codeSent) {
                                        codeSent = true
                                        info = "Si el correo corresponde a una cuenta compatible, recibirás el código. Revisa también spam."
                                    } else {
                                        onPasswordReset()
                                        onDismiss()
                                    }
                                } catch (e: Exception) {
                                    error = when ((e as? HttpException)?.code()) {
                                        400 -> "El código no es válido o ha vencido. Solicita uno nuevo."
                                        503 -> "El servicio de correo no está configurado o no está disponible. Inténtalo más tarde."
                                        else -> "No se pudo completar la solicitud. Comprueba tu conexión e inténtalo de nuevo."
                                    }
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    }
                }
            ) {
                Text(if (codeSent) "Guardar contraseña" else "Enviar código")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                if (codeSent && !loading) {
                    codeSent = false
                    code = ""
                    newPassword = ""
                    error = null
                    info = null
                } else if (!loading) onDismiss()
            }) {
                Text(if (codeSent) "Volver" else "Cancelar")
            }
        }
    )
}
