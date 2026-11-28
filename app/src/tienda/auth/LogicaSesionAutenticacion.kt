package com.example.ui.auth

import android.content.Context
import com.example.data.di.obtenerContenedorElegan
import com.example.data.observability.MonitorErrores
import com.example.data.repository.RepositorioAutenticacion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
    onAuthSuccess: () -> Unit,
    authRepository: RepositorioAutenticacion = context.obtenerContenedorElegan().authRepository
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
            if (isRegister) {
                authRepository.registrar(context, name, email, passwordInput)
            } else {
                authRepository.iniciarSesion(context, email, passwordInput)
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

internal fun ejecutarPasoRecuperacionContrasena(
    scope: CoroutineScope,
    codeSent: Boolean,
    email: String,
    code: String,
    newPassword: String,
    onLoadingChange: (Boolean) -> Unit,
    onError: (String?) -> Unit,
    onInfo: (String?) -> Unit,
    onCodeSentSuccess: () -> Unit,
    onPasswordResetSuccess: () -> Unit,
    authRepository: RepositorioAutenticacion = RepositorioAutenticacion()
) {
    val correo = email.trim()
    when {
        !codeSent && (!correo.contains("@") || !correo.contains(".")) -> {
            onError("Escribe un correo electrónico válido.")
            return
        }
        codeSent && code.length != 6 -> {
            onError("Escribe el código de 6 dígitos.")
            return
        }
        codeSent && newPassword.length < 8 -> {
            onError("La nueva contraseña debe tener al menos 8 caracteres.")
            return
        }
    }

    onLoadingChange(true)
    onError(null)
    onInfo(null)
    scope.launch {
        try {
            if (!codeSent) {
                authRepository.solicitarCodigoRecuperacion(correo)
                onCodeSentSuccess()
                onInfo("Si el correo corresponde a una cuenta compatible, recibirás el código. Revisa también spam.")
            } else {
                authRepository.restablecerContrasena(correo, code, newPassword)
                onPasswordResetSuccess()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onError(e.message ?: "No se pudo completar la solicitud. Comprueba tu conexión e inténtalo de nuevo.")
        } finally {
            onLoadingChange(false)
        }
    }
}

fun attemptAutoSignIn(
    context: Context,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope,
    authRepository: RepositorioAutenticacion = context.obtenerContenedorElegan().authRepository
) {
    scope.launch {
        try {
            authRepository.validarSesion(context)
            onAuthSuccess()
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                authRepository.limpiarSesion(context)
            }
            onUnauthenticated()
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("Auth", "No se pudo validar la sesión con el backend.", e)
            if (authRepository.usuarioActual(context) != null) onAuthSuccess() else onUnauthenticated()
        }
    }
}

fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope,
    authRepository: RepositorioAutenticacion = context.obtenerContenedorElegan().authRepository
) {
    scope.launch {
        try {
            authRepository.cerrarSesion(context)
            onSignOutComplete()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("Auth", "No se pudo notificar el cierre de sesión al backend.", e)
            authRepository.limpiarSesion(context)
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
