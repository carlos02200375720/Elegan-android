package com.example.data.local

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utilidad compartida de cifrado AES-256-GCM respaldada por Android Keystore.
 * Centraliza la creación de claves, generación de IV y recuperación ante claves invalidadas
 * para evitar duplicar código criptográfico entre distintos almacenes locales.
 */
internal class CifradoKeystoreAesGcm(
    private val keyAlias: String,
    private val legacyAliases: List<String> = emptyList()
) {
    private val lock = Any()
    private var cachedSecretKey: SecretKey? = null

    fun isRobolectric(): Boolean =
        "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    private fun obtenerOCrearClave(): SecretKey {
        cachedSecretKey?.let { return it }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) {
            cachedSecretKey = existing
            return existing
        }

        val spec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(spec)
        return keyGenerator.generateKey().also { cachedSecretKey = it }
    }

    fun cifrar(plainText: String): String = synchronized(lock) {
        if (isRobolectric()) return@synchronized plainText
        val secretKey = obtenerOCrearClave()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        require(iv != null && iv.size == GCM_IV_LENGTH_BYTES) {
            "IV AES-GCM inválido generado por AndroidKeyStore."
        }
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encryptedBytes.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encryptedBytes, 0, payload, iv.size, encryptedBytes.size)
        ENCRYPTED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun descifrar(stored: String?): String? = synchronized(lock) {
        if (stored == null) return@synchronized null
        if (isRobolectric()) return@synchronized stored
        if (!stored.startsWith(ENCRYPTED_PREFIX)) return@synchronized null

        val encoded = stored.removePrefix(ENCRYPTED_PREFIX)
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        if (payload.size <= GCM_IV_LENGTH_BYTES) return@synchronized null

        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val ciphertext = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val secretKey = obtenerOCrearClave()
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val plainBytes = cipher.doFinal(ciphertext)
        String(plainBytes, StandardCharsets.UTF_8)
    }

    fun limpiarCacheClave() = synchronized(lock) {
        cachedSecretKey = null
    }

    fun eliminarClaves() = synchronized(lock) {
        cachedSecretKey = null
        runCatching {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
            legacyAliases.forEach { legacy ->
                if (keyStore.containsAlias(legacy)) keyStore.deleteEntry(legacy)
            }
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val ENCRYPTED_PREFIX = "v1:"
    }
}
