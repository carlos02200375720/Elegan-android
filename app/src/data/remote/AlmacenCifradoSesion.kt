package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.CifradoKeystoreAesGcm
import com.example.data.observability.MonitorErrores

/**
 * Almacenamiento cifrado de sesión móvil respaldado por Android Keystore (AES-256-GCM)
 * mediante la utilidad compartida [CifradoKeystoreAesGcm].
 */
internal object AlmacenCifradoSesion {
    private const val PREFS = "elegan_mobile_session"
    private const val TOKEN = "auth_token"
    private const val USER_ID = "user_id"
    private const val EMAIL = "email"
    private const val NAME = "name"
    private const val PHOTO = "photo_url"
    private const val ROLE = "role"
    private const val PROVIDER = "provider"

    private const val KEY_ALIAS = "elegan_mobile_session_aes_key_v1"
    private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    private val cifrador = CifradoKeystoreAesGcm(
        keyAlias = KEY_ALIAS,
        legacyAliases = listOf(LEGACY_MASTER_KEY_ALIAS)
    )
    private val lock = Any()
    private val decryptedCache = HashMap<String, Pair<String, String>>()

    private fun rawPreferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun leerSeguro(context: Context, key: String): String? = synchronized(lock) {
        val prefs = rawPreferences(context)
        val raw = prefs.getString(key, null)
        if (raw == null) {
            decryptedCache.remove(key)
            return@synchronized null
        }
        if (cifrador.isRobolectric()) return@synchronized raw
        val cached = decryptedCache[key]
        if (cached != null && cached.first == raw) {
            return@synchronized cached.second
        }
        return@synchronized try {
            val decoded = cifrador.descifrar(raw)
            if (decoded == null) {
                recuperarAlmacenInvalido(context, null)
            } else {
                decryptedCache[key] = raw to decoded
            }
            decoded
        } catch (e: Exception) {
            recuperarAlmacenInvalido(context, e)
            null
        }
    }

    private fun recuperarAlmacenInvalido(context: Context, error: Exception?) {
        decryptedCache.clear()
        if (error != null) {
            MonitorErrores.registrarAdvertencia(
                "MobileSession",
                "Almacenamiento cifrado de sesión inválido o heredado; se limpiará para recrearlo.",
                error
            )
        }
        runCatching { rawPreferences(context).edit().clear().apply() }
        cifrador.eliminarClaves()
    }

    fun save(context: Context, response: AuthResponse) {
        require(response.token.isNotBlank()) { "El backend devolvió un token vacío." }
        require(response.user.effectiveId.isNotBlank()) { "El backend devolvió un usuario sin identificador." }

        val role = if (
            response.user.isAdmin ||
            response.user.role == "admin" ||
            response.user.role == "superadmin"
        ) "admin" else "customer"

        synchronized(lock) {
            fun escribir() {
                val plainValues = listOf(
                    TOKEN to response.token,
                    USER_ID to response.user.effectiveId,
                    EMAIL to response.user.email.orEmpty(),
                    NAME to response.user.name,
                    PHOTO to response.user.effectivePhotoUrl,
                    ROLE to role,
                    PROVIDER to response.user.provider
                )
                val encryptedPairs = plainValues.map { (key, plain) ->
                    Triple(key, cifrador.cifrar(plain), plain)
                }
                val editor = rawPreferences(context).edit()
                encryptedPairs.forEach { (key, encrypted, plain) ->
                    editor.putString(key, encrypted)
                    if (!cifrador.isRobolectric()) {
                        decryptedCache[key] = encrypted to plain
                    }
                }
                editor.apply()
            }

            try {
                escribir()
            } catch (firstError: Exception) {
                recuperarAlmacenInvalido(context, firstError)
                try {
                    escribir()
                } catch (recoveryError: Exception) {
                    MonitorErrores.registrarError(
                        "MobileSession",
                        "No se pudo guardar la sesión cifrada en Android Keystore.",
                        recoveryError
                    )
                    throw IllegalStateException("No se pudo guardar el almacenamiento seguro de sesión.", recoveryError)
                }
            }
        }
    }

    fun token(context: Context): String? =
        leerSeguro(context, TOKEN)?.takeIf { it.isNotBlank() }

    fun userId(context: Context): String? =
        leerSeguro(context, USER_ID)?.takeIf { it.isNotBlank() }

    fun user(context: Context): AuthUser? = synchronized(lock) {
        val id = userId(context) ?: return@synchronized null
        val token = token(context) ?: return@synchronized null
        if (token.startsWith("local_session_")) return@synchronized null
        AuthUser(
            id = id,
            email = leerSeguro(context, EMAIL).orEmpty(),
            name = leerSeguro(context, NAME).orEmpty(),
            photoUrl = leerSeguro(context, PHOTO).orEmpty(),
            role = leerSeguro(context, ROLE).orEmpty().ifBlank { "customer" },
            provider = leerSeguro(context, PROVIDER).orEmpty().ifBlank { "password" }
        )
    }

    fun clear(context: Context) = synchronized(lock) {
        decryptedCache.clear()
        rawPreferences(context).edit().clear().apply()
    }
}
