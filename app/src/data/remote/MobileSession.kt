package com.example.data.remote

import android.content.Context
import kotlinx.coroutines.flow.Flow

/**
 * Sesión móvil autenticada por Elegan Backend respaldada por Jetpack Preferences DataStore.
 * El token y la identidad se conservan cifrados con Android Keystore (AES-256-GCM) y se gestionan
 * de manera asíncrona y reactiva mediante corrutinas y [Flow].
 */
object MobileSession {
    fun observeSession(context: Context): Flow<EstadoSesionMovil> =
        AlmacenCifradoSesion.observeSession(context)

    fun observeUser(context: Context): Flow<AuthUser?> =
        AlmacenCifradoSesion.observeUser(context)

    fun observeToken(context: Context): Flow<String?> =
        AlmacenCifradoSesion.observeToken(context)

    suspend fun saveAsync(context: Context, response: AuthResponse) =
        AlmacenCifradoSesion.saveAsync(context, response)

    suspend fun userIdAsync(context: Context): String? =
        AlmacenCifradoSesion.userIdAsync(context)

    suspend fun userAsync(context: Context): AuthUser? =
        AlmacenCifradoSesion.userAsync(context)

    suspend fun tokenAsync(context: Context): String? =
        AlmacenCifradoSesion.tokenAsync(context)

    suspend fun clearAsync(context: Context) =
        AlmacenCifradoSesion.clearAsync(context)

    suspend fun deleteAccountAsync(context: Context) =
        AlmacenCifradoSesion.clearAsync(context)

    fun cachedSession(context: Context): EstadoSesionMovil? =
        AlmacenCifradoSesion.cachedSession(context)

    fun save(context: Context, response: AuthResponse) =
        AlmacenCifradoSesion.save(context, response)

    fun userId(context: Context): String? =
        AlmacenCifradoSesion.userId(context)

    fun user(context: Context): AuthUser? =
        AlmacenCifradoSesion.user(context)

    fun token(context: Context): String? =
        AlmacenCifradoSesion.token(context)

    fun clear(context: Context) =
        AlmacenCifradoSesion.clear(context)

    fun deleteAccount(context: Context) =
        AlmacenCifradoSesion.clear(context)

    suspend fun registerUser(
        context: Context,
        name: String,
        email: String,
        password: String
    ): AuthResponse {
        val response = ApiAutenticacionMovil.service.register(
            RegistroRequest(email = email.trim(), password = password, name = name.trim())
        )
        saveAsync(context, response)
        return response
    }

    suspend fun loginUser(context: Context, email: String, password: String): AuthResponse {
        val response = ApiAutenticacionMovil.service.login(
            InicioSesionRequest(email = email.trim(), password = password)
        )
        saveAsync(context, response)
        return response
    }

    suspend fun loginWithGoogleToken(context: Context, googleIdToken: String): AuthResponse {
        val response = ApiAutenticacionMovil.service.loginGoogle(
            GoogleAuthRequest(idToken = googleIdToken)
        )
        saveAsync(context, response)
        return response
    }

    suspend fun validateSession(context: Context): AuthUser {
        val token = tokenAsync(context)
            ?: throw IllegalStateException("No hay una sesión guardada.")
        if (token.startsWith("local_session_")) {
            clearAsync(context)
            throw IllegalStateException("La sesión anterior era local y ya no es válida.")
        }
        val user = ApiAutenticacionMovil.service.me("Bearer $token").user
        saveAsync(context, AuthResponse(token = token, user = user))
        return user
    }

    suspend fun logout(context: Context) {
        val token = tokenAsync(context)
        try {
            if (!token.isNullOrBlank() && !token.startsWith("local_session_")) {
                ApiAutenticacionMovil.service.logout("Bearer $token")
            }
        } finally {
            clearAsync(context)
        }
    }
}
