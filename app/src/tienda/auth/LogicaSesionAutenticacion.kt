package com.example.ui.auth

import android.content.Context
import android.util.Log
import com.example.data.remote.MobileSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun validarYEjecutarAutenticacion(
    context: Context,
    coroutineScope: CoroutineScope,
    isRegister: Boolean,
    nameInput: String,
    emailInput: String,
    passwordInput: String,
    confirmPasswordInput: String,
    acceptedTerms: Boolean,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val email = emailInput.trim()
    val name = nameInput.trim()

    val validationError = when {
        isRegister && name.isBlank() -> "Por favor ingresa tu nombre completo."
        email.isBlank() || !email.contains("@") -> "Por favor ingresa un correo electrónico válido."
        passwordInput.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        isRegister && passwordInput != confirmPasswordInput -> "Las contraseñas no coinciden."
        isRegister && !acceptedTerms ->
            "Debes leer y aceptar los Términos y Condiciones y la Política de Privacidad."
        else -> null
    }

    if (validationError != null) {
        onError(validationError)
        return
    }

    onLoadingChange(true)
    onError(null)

    coroutineScope.launch {
        try {
            val response = withContext(Dispatchers.IO) {
                if (isRegister) {
                    val displayName = name.ifBlank { "Cliente Elegan" }
                    MobileSession.registerLocalUser(context, displayName, email, passwordInput)
                } else {
                    MobileSession.loginLocalUser(context, email, passwordInput)
                }
            }
            MobileSession.save(context, response)
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("Auth", "Error de autenticación", e)
            onError(mapAuthError(e))
        } finally {
            onLoadingChange(false)
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
) {
    val savedUser = MobileSession.user(context)
    if (savedUser != null) {
        onAuthSuccess()
    } else {
        onUnauthenticated()
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        MobileSession.clear(context)
        onSignOutComplete()
    }
}

fun mapAuthError(e: Throwable): String {
    val message = e.message ?: ""
    return when {
        message.contains("ya está registrado", ignoreCase = true) ||
            message.contains("ya registrado", ignoreCase = true) ->
            "Este correo electrónico ya está registrado. Selecciona Iniciar Sesión."
        message.contains("incorrecta", ignoreCase = true) ||
            message.contains("incorrectos", ignoreCase = true) ->
            "Correo electrónico o contraseña incorrectos."
        else ->
            e.localizedMessage ?: "Ocurrió un error al iniciar sesión. Inténtalo de nuevo."
    }
}
