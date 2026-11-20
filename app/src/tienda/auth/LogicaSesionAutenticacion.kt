package com.example.ui.auth

import android.content.Context
import com.example.data.observability.MonitorErrores
import com.example.data.remote.MobileSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.HttpException

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
        passwordInput.length < if (isRegister) 8 else 1 ->
            if (isRegister) "La contraseña debe tener al menos 8 caracteres." else "Ingresa tu contraseña."
        isRegister && passwordInput.length > 128 -> "La contraseña no puede superar 128 caracteres."
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
            withContext(Dispatchers.IO) {
                if (isRegister) {
                    MobileSession.registerUser(context, name, email, passwordInput)
                } else {
                    MobileSession.loginUser(context, email, passwordInput)
                }
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarError("Auth", "Error de autenticación con Elegan Backend", e)
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
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withContext(Dispatchers.IO) {
                MobileSession.validateSession(context)
            }
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                MobileSession.clear(context)
            }
            onUnauthenticated()
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("Auth", "No se pudo validar la sesión con el backend.", e)
            if (MobileSession.user(context) != null) onAuthSuccess() else onUnauthenticated()
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            withContext(Dispatchers.IO) { MobileSession.logout(context) }
            onSignOutComplete()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("Auth", "No se pudo notificar el cierre de sesión al backend.", e)
            MobileSession.clear(context)
            onSignOutComplete()
        }
    }
}

fun mapAuthError(e: Throwable): String {
    val status = (e as? HttpException)?.code()
    return when (status) {
        400 -> "Los datos enviados no son válidos. Revisa el formulario."
        401 -> "Correo electrónico o contraseña incorrectos."
        403 -> "Esta cuenta no tiene permiso para acceder."
        409 -> "Este correo electrónico ya está registrado. Selecciona Iniciar Sesión."
        503 -> "El servicio de autenticación no está disponible en este momento."
        else -> when {
            e.message.orEmpty().contains("Unable to resolve host", true) ||
                e.message.orEmpty().contains("timeout", true) ->
                "No se pudo conectar con Elegan. Comprueba tu conexión a Internet."
            else -> e.localizedMessage ?: "Ocurrió un error al iniciar sesión. Inténtalo de nuevo."
        }
    }
}
