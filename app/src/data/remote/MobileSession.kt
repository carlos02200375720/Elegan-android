package com.example.data.remote

import android.content.Context

/**
 * Sesión móvil autenticada por Elegan Backend.
 * El token se conserva cifrado en el dispositivo; las credenciales se validan en el servidor.
 */
object MobileSession {
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
        save(context, response)
        return response
    }

    suspend fun loginUser(context: Context, email: String, password: String): AuthResponse {
        val response = ApiAutenticacionMovil.service.login(
            InicioSesionRequest(email = email.trim(), password = password)
        )
        save(context, response)
        return response
    }

    suspend fun loginWithGoogleToken(context: Context, googleIdToken: String): AuthResponse {
        val response = ApiAutenticacionMovil.service.loginGoogle(
            GoogleAuthRequest(idToken = googleIdToken)
        )
        save(context, response)
        return response
    }

    suspend fun validateSession(context: Context): AuthUser {
        val token = token(context)
            ?: throw IllegalStateException("No hay una sesión guardada.")
        if (token.startsWith("local_session_")) {
            clear(context)
            throw IllegalStateException("La sesión anterior era local y ya no es válida.")
        }
        val user = ApiAutenticacionMovil.service.me("Bearer $token").user
        save(context, AuthResponse(token = token, user = user))
        return user
    }

    suspend fun logout(context: Context) {
        val token = token(context)
        try {
            if (!token.isNullOrBlank() && !token.startsWith("local_session_")) {
                ApiAutenticacionMovil.service.logout("Bearer $token")
            }
        } finally {
            clear(context)
        }
    }
}
