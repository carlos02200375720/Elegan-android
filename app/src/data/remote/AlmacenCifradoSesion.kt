package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
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

    @Volatile
    private var cachedPreferences: SharedPreferences? = null

    fun preferences(context: Context): SharedPreferences {
        cachedPreferences?.let { return it }

        val appContext = context.applicationContext
        return synchronized(this) {
            cachedPreferences ?: run {
                val prefs = if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) {
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                } else {
                    crearPreferenciasCifradasConRecuperacion(appContext)
                }
                cachedPreferences = prefs
                prefs
            }
        }
    }

    private fun crearPreferenciasCifradasConRecuperacion(context: Context): SharedPreferences {
        fun crear(): SharedPreferences {
            val prefs = EncryptedSharedPreferences.create(
                context,
                PREFS,
                MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            // Fuerza la lectura de los valores ahora; el descifrado puede fallar al acceder a ellos.
            prefs.all
            return prefs
        }

        return try {
            crear()
        } catch (firstError: Exception) {
            Log.e("MobileSession", "Almacenamiento cifrado inválido; se eliminará la sesión local y se recreará.", firstError)
            try {
                context.deleteSharedPreferences(PREFS)
                eliminarClaveMaestraInvalida()
                crear()
            } catch (recoveryError: Exception) {
                Log.e("MobileSession", "No se pudo recuperar el almacenamiento cifrado de sesión.", recoveryError)
                throw IllegalStateException("No se pudo recuperar el almacenamiento seguro de sesión.", recoveryError)
            }
        }
    }

    private fun eliminarClaveMaestraInvalida() {
        try {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            val alias = "_androidx_security_master_key_"
            if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        } catch (e: Exception) {
            Log.w("MobileSession", "No se pudo eliminar la clave maestra anterior; se intentará recrear.", e)
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
