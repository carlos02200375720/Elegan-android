package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal object AlmacenCifradoSesion {
    private const val PREFS = "elegan_mobile_session"
    private const val TOKEN = "auth_token"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"
    private const val NAME = "name"
    private const val PHOTO = "photo_url"
    private const val ROLE = "role"
    private const val PROVIDER = "provider"
    internal const val LOCAL_USERS_KEY = "local_registered_accounts"

    @Volatile
    private var cachedPreferences: SharedPreferences? = null

    fun preferences(context: Context): SharedPreferences {
        cachedPreferences?.let { return it }

        val appContext = context.applicationContext
        return synchronized(this) {
            cachedPreferences ?: run {
                val prefs = try {
                    EncryptedSharedPreferences.create(
                        appContext,
                        PREFS,
                        MasterKey.Builder(appContext)
                            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                            .build(),
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                    )
                } catch (e: Exception) {
                    Log.e("MobileSession", "No se pudo inicializar el almacenamiento cifrado de sesión.", e)
                    throw IllegalStateException("No se pudo proteger la sesión en este dispositivo.", e)
                }
                cachedPreferences = prefs
                prefs
            }
        }
    }

    fun save(context: Context, response: AuthResponse) {
        require(response.token.isNotBlank()) { "El backend devolvió un token vacío." }
        require(response.user.effectiveId.isNotBlank()) { "El backend devolvió un usuario sin identificador." }

        val role = if (
            response.user.isAdmin ||
            response.user.role == "admin" ||
            response.user.role == "superadmin"
        ) "admin" else "customer"

        preferences(context).edit()
            .putString(TOKEN, response.token)
            .putString(USER_ID, response.user.effectiveId)
            .putString(EMAIL, response.user.email.orEmpty())
            .putString(NAME, response.user.name)
            .putString(PHOTO, response.user.effectivePhotoUrl)
            .putString(ROLE, role)
            .putString(PROVIDER, response.user.provider)
            .apply()
    }

    fun token(context: Context): String? =
        preferences(context).getString(TOKEN, null)?.takeIf { it.isNotBlank() }

    fun userId(context: Context): String? =
        preferences(context).getString(USER_ID, null)?.takeIf { it.isNotBlank() }

    fun user(context: Context): AuthUser? {
        val prefs = preferences(context)
        val id = prefs.getString(USER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        val token = token(context) ?: return null
        if (token.startsWith("local_session_")) return null
        return AuthUser(
            id = id,
            email = prefs.getString(EMAIL, ""),
            name = prefs.getString(NAME, "").orEmpty(),
            photoUrl = prefs.getString(PHOTO, "").orEmpty(),
            role = prefs.getString(ROLE, "customer").orEmpty(),
            provider = prefs.getString(PROVIDER, "password").orEmpty()
        )
    }

    fun clear(context: Context) {
        preferences(context).edit().clear().apply()
    }
}
