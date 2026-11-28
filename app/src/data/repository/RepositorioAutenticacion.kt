package com.example.data.repository

import android.content.Context
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import com.example.data.remote.RestablecerContrasenaRequest
import com.example.data.remote.SolicitarRecuperacionRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/**
 * Repositorio de autenticación y recuperación de contraseña.
 * Centraliza las operaciones de sesión para que las pantallas de `ui.auth`
 * no dependan directamente de Retrofit ni de los DTOs de red.
 */
class RepositorioAutenticacion internal constructor(
    private val apiProvider: () -> EleganAuthApi
) {
    constructor() : this(apiProvider = { ApiAutenticacionMovil.service })

    fun observarUsuarioActual(context: Context): Flow<AuthUser?> = MobileSession.observeUser(context)

    fun observarTokenActual(context: Context): Flow<String?> = MobileSession.observeToken(context)

    suspend fun usuarioActualAsync(context: Context): AuthUser? = MobileSession.userAsync(context)

    suspend fun limpiarSesionAsync(context: Context) = MobileSession.clearAsync(context)

    fun usuarioActual(context: Context): AuthUser? = MobileSession.user(context)

    fun userIdActual(context: Context): String? = MobileSession.userId(context)

    fun tokenActual(context: Context): String? = MobileSession.token(context)

    fun limpiarSesion(context: Context) = MobileSession.clear(context)

    suspend fun registrar(
        context: Context,
        name: String,
        email: String,
        password: String
    ): AuthResponse = withContext(Dispatchers.IO) {
        MobileSession.registerUser(context, name, email, password)
    }

    suspend fun iniciarSesion(
        context: Context,
        email: String,
        password: String
    ): AuthResponse = withContext(Dispatchers.IO) {
        MobileSession.loginUser(context, email, password)
    }

    suspend fun validarSesion(context: Context): AuthUser = withContext(Dispatchers.IO) {
        MobileSession.validateSession(context)
    }

    suspend fun cerrarSesion(context: Context) = withContext(Dispatchers.IO) {
        MobileSession.logout(context)
    }

    suspend fun solicitarCodigoRecuperacion(email: String) = withContext(Dispatchers.IO) {
        try {
            apiProvider().requestPasswordReset(SolicitarRecuperacionRequest(email.trim()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val httpCode = (e as? HttpException)?.code()
            MonitorErrores.registrarError(
                tag = "PasswordRecovery",
                mensaje = "Error al solicitar código de recuperación de contraseña",
                error = e,
                extras = buildMap {
                    put("step", "request_reset")
                    if (httpCode != null) put("httpCode", httpCode.toString())
                }
            )
            throw IllegalStateException(mapRecoveryError(httpCode, isResetStep = false), e)
        }
    }

    suspend fun restablecerContrasena(
        email: String,
        code: String,
        newPassword: String
    ) = withContext(Dispatchers.IO) {
        try {
            apiProvider().resetPassword(
                RestablecerContrasenaRequest(
                    email = email.trim(),
                    code = code.trim(),
                    newPassword = newPassword
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val httpCode = (e as? HttpException)?.code()
            MonitorErrores.registrarError(
                tag = "PasswordRecovery",
                mensaje = "Error al restablecer la contraseña",
                error = e,
                extras = buildMap {
                    put("step", "confirm_reset")
                    if (httpCode != null) put("httpCode", httpCode.toString())
                }
            )
            throw IllegalStateException(mapRecoveryError(httpCode, isResetStep = true), e)
        }
    }

    private fun mapRecoveryError(httpCode: Int?, isResetStep: Boolean): String = when (httpCode) {
        400 -> if (!isResetStep) {
            "Revisa el correo electrónico ingresado e inténtalo de nuevo."
        } else {
            "El código no es válido o ha vencido. Solicita uno nuevo."
        }
        503 -> "El servicio de correo no está configurado o no está disponible. Inténtalo más tarde."
        else -> "No se pudo completar la solicitud. Comprueba tu conexión e inténtalo de nuevo."
    }
}
