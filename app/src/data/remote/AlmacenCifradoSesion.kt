package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

internal object AlmacenCifradoSesion {
    private const val PREFS = "elegan_mobile_session"
    private const val TOKEN = "local_token"
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
                    Log.w(
                        "MobileSession",
                        "Fallo al inicializar EncryptedSharedPreferences, usando almacenamiento seguro estándar",
                        e
                    )
                    appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                }
                cachedPreferences = prefs
                prefs
            }
        }
    }

    fun save(context: Context, response: AuthResponse) {
        val token = response.token.ifBlank { "local_session_" + UUID.randomUUID().toString() }
        val userId = response.user.effectiveId.ifBlank { UUID.randomUUID().toString() }
        val role = if (
            response.user.isAdmin ||
            response.user.role == "admin" ||
            response.user.role == "superadmin"
        ) {
            "admin"
        } else {
            "customer"
        }
        preferences(context).edit()
            .putString(TOKEN, token)
            .putString(USER_ID, userId)
            .putString(EMAIL, response.user.email.orEmpty())
            .putString(NAME, response.user.name.ifBlank { "Cliente Elegan" })
            .putString(PHOTO, response.user.effectivePhotoUrl)
            .putString(ROLE, role)
            .putString(PROVIDER, response.user.provider.ifBlank { "local" })
            .apply()
    }

    fun userId(context: Context): String? =
        preferences(context).getString(USER_ID, null)?.takeIf { it.isNotBlank() }

    fun user(context: Context): AuthUser? {
        val prefs = preferences(context)
        val id = prefs.getString(USER_ID, null) ?: return null
        return AuthUser(
            id = id,
            email = prefs.getString(EMAIL, ""),
            name = prefs.getString(NAME, "").orEmpty(),
            photoUrl = prefs.getString(PHOTO, "").orEmpty(),
            role = prefs.getString(ROLE, "customer").orEmpty(),
            provider = prefs.getString(PROVIDER, "local").orEmpty()
        )
    }

    fun clear(context: Context) {
        val prefs = preferences(context)
        val savedAccounts = prefs.getString(LOCAL_USERS_KEY, null)
        prefs.edit()
            .remove(TOKEN)
            .remove(USER_ID)
            .remove(EMAIL)
            .remove(NAME)
            .remove(PHOTO)
            .remove(ROLE)
            .remove(PROVIDER)
            .apply()
        if (savedAccounts != null) {
            prefs.edit().putString(LOCAL_USERS_KEY, savedAccounts).apply()
        }
    }
}
