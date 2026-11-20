package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.data.observability.MonitorErrores
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Almacenamiento cifrado de sesión móvil respaldado directamente por Android Keystore (AES-256-GCM),
 * sin depender de la librería alfa descontinuada `androidx.security:security-crypto`.
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

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "elegan_mobile_session_aes_key_v1"
    private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENCRYPTED_PREFIX = "v1:"

    private val lock = Any()

    private fun isRobolectric(): Boolean =
        "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    private fun rawPreferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun obtenerOCrearClaveMaestra(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) return existing

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun cifrarValor(plainText: String): String {
        if (isRobolectric()) return plainText
        val secretKey = obtenerOCrearClaveMaestra()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        require(iv != null && iv.size == GCM_IV_LENGTH_BYTES) { "IV inválido generado por AndroidKeyStore." }
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, payload, iv.size, encryptedBytes.size)
        return ENCRYPTED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun descifrarValor(stored: String?): String? {
        if (stored == null) return null
        if (isRobolectric()) return stored
        if (!stored.startsWith(ENCRYPTED_PREFIX)) return null

        val encoded = stored.removePrefix(ENCRYPTED_PREFIX)
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        if (payload.size <= GCM_IV_LENGTH_BYTES) return null

        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val secretKey = obtenerOCrearClaveMaestra()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    private fun leerSeguro(context: Context, key: String): String? = synchronized(lock) {
        val prefs = rawPreferences(context)
        val raw = prefs.getString(key, null) ?: return@synchronized null
        if (isRobolectric()) return@synchronized raw
        return@synchronized try {
            val decoded = descifrarValor(raw)
            if (decoded == null) {
                recuperarAlmacenInvalido(context, null)
            }
            decoded
        } catch (e: Exception) {
            recuperarAlmacenInvalido(context, e)
            null
        }
    }

    private fun recuperarAlmacenInvalido(context: Context, error: Exception?) {
        if (error != null) {
            MonitorErrores.registrarAdvertencia(
                "MobileSession",
                "Almacenamiento cifrado de sesión inválido o heredado; se limpiará para recrearlo.",
                error
            )
        }
        runCatching { rawPreferences(context).edit().clear().apply() }
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
            if (keyStore.containsAlias(LEGACY_MASTER_KEY_ALIAS)) keyStore.deleteEntry(LEGACY_MASTER_KEY_ALIAS)
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

        synchronized(lock) {
            fun escribir() {
                rawPreferences(context).edit()
                    .putString(TOKEN, cifrarValor(response.token))
                    .putString(USER_ID, cifrarValor(response.user.effectiveId))
                    .putString(EMAIL, cifrarValor(response.user.email.orEmpty()))
                    .putString(NAME, cifrarValor(response.user.name))
                    .putString(PHOTO, cifrarValor(response.user.effectivePhotoUrl))
                    .putString(ROLE, cifrarValor(role))
                    .putString(PROVIDER, cifrarValor(response.user.provider))
                    .apply()
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

    fun user(context: Context): AuthUser? {
        val id = userId(context) ?: return null
        val token = token(context) ?: return null
        if (token.startsWith("local_session_")) return null
        return AuthUser(
            id = id,
            email = leerSeguro(context, EMAIL).orEmpty(),
            name = leerSeguro(context, NAME).orEmpty(),
            photoUrl = leerSeguro(context, PHOTO).orEmpty(),
            role = leerSeguro(context, ROLE).orEmpty().ifBlank { "customer" },
            provider = leerSeguro(context, PROVIDER).orEmpty().ifBlank { "password" }
        )
    }

    fun clear(context: Context) = synchronized(lock) {
        rawPreferences(context).edit().clear().apply()
    }
}
