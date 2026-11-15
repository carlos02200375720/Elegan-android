package com.example.data.remote

import android.content.Context

/**
 * Fachada pública para la gestión de sesión móvil y credenciales locales.
 * Cifrado en reposo mediante AES-256.
 */
object MobileSession {
    const val SUPER_ADMIN_EMAIL = "cg0220037@gmail.com"

    fun save(context: Context, response: AuthResponse) =
        AlmacenCifradoSesion.save(context, response)

    fun userId(context: Context): String? =
        AlmacenCifradoSesion.userId(context)

    fun user(context: Context): AuthUser? =
        AlmacenCifradoSesion.user(context)

    fun clear(context: Context) =
        AlmacenCifradoSesion.clear(context)

    fun deleteAccount(context: Context) =
        AlmacenCuentasLocales.deleteCurrentAccount(context)

    fun registerLocalUser(
        context: Context,
        name: String,
        email: String,
        password: String
    ): AuthResponse = RegistroYLoginLocal.registerLocalUser(context, name, email, password)

    fun loginLocalUser(context: Context, email: String, password: String): AuthResponse =
        RegistroYLoginLocal.loginLocalUser(context, email, password)

    fun loginAdminLocally(context: Context): AuthResponse =
        AccesosRapidosYGoogle.loginAdminLocally(context)

    fun loginClientLocally(context: Context): AuthResponse =
        AccesosRapidosYGoogle.loginClientLocally(context)

    fun loginWithGoogleTokenLocally(context: Context, googleIdToken: String): AuthResponse =
        AccesosRapidosYGoogle.loginWithGoogleTokenLocally(context, googleIdToken)
}
