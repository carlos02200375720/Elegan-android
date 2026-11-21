package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.RestablecerContrasenaRequest
import com.example.data.remote.SolicitarRecuperacionRequest
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import kotlinx.coroutines.CancellationException
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

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color(0xFF5F5A56),
        cursorColor = EleganBurgundy,
        focusedBorderColor = EleganBurgundy,
        unfocusedBorderColor = Color(0xFFDDD5CE),
        focusedLabelColor = EleganBurgundy,
        unfocusedLabelColor = Color(0xFF5F5A56),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color(0xFFF6F4F2)
    )

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        containerColor = Color.White,
        titleContentColor = EleganCharcoal,
        textContentColor = EleganCharcoal,
        title = { Text(if (codeSent) "Restablecer contraseña" else "Recuperar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!codeSent) {
                    Text("Escribe el correo de tu cuenta. Si está registrada con correo y contraseña, recibirás un código.")
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; error = null },
                        enabled = !loading,
                        isError = error != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_recovery_email_input"),
                        label = { Text("Correo electrónico") },
                        singleLine = true,
                        colors = fieldColors
                    )
                } else {
                    Text("Introduce el código de 6 dígitos que enviamos a tu correo y crea una contraseña nueva.")
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6); error = null },
                        enabled = !loading,
                        isError = error != null && code.length != 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_recovery_code_input"),
                        label = { Text("Código de 6 dígitos") },
                        singleLine = true,
                        colors = fieldColors
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; error = null },
                        enabled = !loading,
                        isError = error != null && newPassword.length < 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_recovery_new_password_input"),
                        label = { Text("Nueva contraseña (mínimo 8 caracteres)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = fieldColors
                    )
                }

                info?.let { mensajeInfo ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFEAF5EE),
                        border = BorderStroke(1.dp, Color(0xFFB7DEC4)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_recovery_info")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircleOutline,
                                contentDescription = null,
                                tint = Color(0xFF1F6E3D),
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mensajeInfo,
                                color = Color(0xFF1A4F2E),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }

                error?.let { mensajeError ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_recovery_error")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = mensajeError,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp
                                )
                            )
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !loading,
                modifier = Modifier.testTag("password_recovery_confirm_btn"),
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
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    val httpCode = (e as? HttpException)?.code()
                                    MonitorErrores.registrarError(
                                        tag = "PasswordRecovery",
                                        mensaje = if (!codeSent) {
                                            "Error al solicitar código de recuperación de contraseña"
                                        } else {
                                            "Error al restablecer la contraseña"
                                        },
                                        error = e,
                                        extras = buildMap {
                                            put("step", if (!codeSent) "request_reset" else "confirm_reset")
                                            if (httpCode != null) put("httpCode", httpCode.toString())
                                        }
                                    )
                                    error = when (httpCode) {
                                        400 -> if (!codeSent) {
                                            "Revisa el correo electrónico ingresado e inténtalo de nuevo."
                                        } else {
                                            "El código no es válido o ha vencido. Solicita uno nuevo."
                                        }
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
            TextButton(
                enabled = !loading,
                modifier = Modifier.testTag("password_recovery_dismiss_btn"),
                onClick = {
                    if (codeSent && !loading) {
                        codeSent = false
                        code = ""
                        newPassword = ""
                        error = null
                        info = null
                    } else if (!loading) {
                        onDismiss()
                    }
                }
            ) {
                Text(if (codeSent) "Volver" else "Cancelar")
            }
        }
    )
}
