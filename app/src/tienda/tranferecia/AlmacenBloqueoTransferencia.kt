package com.example.ui

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.webkit.MimeTypeMap
import com.example.data.model.BankAccountInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

data class EstadoBloqueoTransferencia(
    val userId: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val shipping: Double = 0.0,
    val total: Double = 0.0,
    val couponCode: String = "",
    val customerName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val selectedAccountId: String = "",
    val pendingTransferOrderId: String? = null,
    val paymentProofFilePath: String? = null,
    val bankAccounts: List<BankAccountInfo> = emptyList()
)

object AlmacenBloqueoTransferencia {
    private const val PREFS_NAME = "elegan_transfer_lock_state"
    private const val PROOF_DIR_NAME = "pending_transfer_proof"
    private const val KEY_ACTIVE = "is_active"
    private const val KEY_ENCRYPTED_STATE = "encrypted_state_v1"

    // Claves antiguas: se conservan solo para migrar el estado existente a la versión cifrada.
    private const val KEY_USER_ID = "user_id"
    private const val KEY_SUBTOTAL = "subtotal"
    private const val KEY_DISCOUNT = "discount"
    private const val KEY_SHIPPING = "shipping"
    private const val KEY_TOTAL = "total"
    private const val KEY_COUPON_CODE = "coupon_code"
    private const val KEY_CUSTOMER_NAME = "customer_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_PHONE = "phone"
    private const val KEY_ADDRESS = "address"
    private const val KEY_CITY = "city"
    private const val KEY_SELECTED_ACCOUNT_ID = "selected_account_id"
    private const val KEY_PENDING_ORDER_ID = "pending_transfer_order_id"
    private const val KEY_PROOF_FILE_PATH = "payment_proof_file_path"
    private const val KEY_BANK_ACCOUNTS_JSON = "bank_accounts_json"

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "elegan_transfer_state_aes_key_v1"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val ENCRYPTED_PREFIX = "v1:"
    private val lock = Any()
    private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var cachedSecretKey: SecretKey? = null
    private var cachedEncryptedRaw: String? = null
    private var cachedEstado: EstadoBloqueoTransferencia? = null

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isRobolectric(): Boolean =
        "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    private fun obtenerOCrearClave(): SecretKey {
        cachedSecretKey?.let { return it }

        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        if (existing != null) {
            cachedSecretKey = existing
            return existing
        }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
            .also { cachedSecretKey = it }
    }

    private fun cifrarEstado(plainText: String): String {
        // Robolectric no implementa AndroidKeyStore; este bypass solo existe en pruebas JVM.
        if (isRobolectric()) return plainText

        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, obtenerOCrearClave())
        val iv = cipher.iv
        require(iv != null && iv.size == GCM_IV_LENGTH_BYTES) { "IV AES-GCM inválido." }
        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
        val payload = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, payload, 0, iv.size)
        System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
        return ENCRYPTED_PREFIX + Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun descifrarEstado(stored: String): String {
        if (isRobolectric()) return stored
        require(stored.startsWith(ENCRYPTED_PREFIX)) { "Formato de estado de transferencia no reconocido." }

        val payload = Base64.decode(stored.removePrefix(ENCRYPTED_PREFIX), Base64.NO_WRAP)
        require(payload.size > GCM_IV_LENGTH_BYTES) { "Estado de transferencia cifrado incompleto." }
        val iv = payload.copyOfRange(0, GCM_IV_LENGTH_BYTES)
        val encrypted = payload.copyOfRange(GCM_IV_LENGTH_BYTES, payload.size)
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, obtenerOCrearClave(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
    }

    private fun estadoComoJson(estado: EstadoBloqueoTransferencia): String {
        val accounts = JSONArray().apply {
            estado.bankAccounts.forEach { account ->
                put(JSONObject().apply {
                    put("id", account.id)
                    put("bankName", account.bankName)
                    put("accountNumber", account.accountNumber)
                    put("accountType", account.accountType)
                    put("holderName", account.holderName)
                    put("holderDocument", account.holderDocument)
                })
            }
        }

        return JSONObject().apply {
            put("userId", estado.userId)
            put("subtotal", estado.subtotal)
            put("discount", estado.discount)
            put("shipping", estado.shipping)
            put("total", estado.total)
            put("couponCode", estado.couponCode)
            put("customerName", estado.customerName)
            put("email", estado.email)
            put("phone", estado.phone)
            put("address", estado.address)
            put("city", estado.city)
            put("selectedAccountId", estado.selectedAccountId)
            put("pendingTransferOrderId", estado.pendingTransferOrderId)
            put("paymentProofFilePath", estado.paymentProofFilePath)
            put("bankAccounts", accounts)
        }.toString()
    }

    private fun jsonAEstado(raw: String): EstadoBloqueoTransferencia {
        val json = JSONObject(raw)
        val accountsJson = json.optJSONArray("bankAccounts") ?: JSONArray()
        val accounts = buildList(accountsJson.length()) {
            for (index in 0 until accountsJson.length()) {
                val account = accountsJson.optJSONObject(index) ?: continue
                val id = account.optString("id", "")
                if (id.isBlank()) continue
                add(BankAccountInfo(
                    id = id,
                    bankName = account.optString("bankName", ""),
                    accountNumber = account.optString("accountNumber", ""),
                    accountType = account.optString("accountType", ""),
                    holderName = account.optString("holderName", ""),
                    holderDocument = account.optString("holderDocument", "")
                ))
            }
        }

        return EstadoBloqueoTransferencia(
            userId = json.optString("userId", ""),
            subtotal = json.optDouble("subtotal", 0.0),
            discount = json.optDouble("discount", 0.0),
            shipping = json.optDouble("shipping", 0.0),
            total = json.optDouble("total", 0.0),
            couponCode = json.optString("couponCode", ""),
            customerName = json.optString("customerName", ""),
            email = json.optString("email", ""),
            phone = json.optString("phone", ""),
            address = json.optString("address", ""),
            city = json.optString("city", ""),
            selectedAccountId = json.optString("selectedAccountId", ""),
            pendingTransferOrderId = json.optString("pendingTransferOrderId", "").takeIf { it.isNotBlank() },
            paymentProofFilePath = json.optString("paymentProofFilePath", "").takeIf { it.isNotBlank() },
            bankAccounts = accounts
        )
    }

    private fun cargarEstadoInterno(context: Context): EstadoBloqueoTransferencia? {
        val sp = prefs(context)
        if (!sp.getBoolean(KEY_ACTIVE, false)) {
            cachedEncryptedRaw = null
            cachedEstado = null
            return null
        }

        val encrypted = sp.getString(KEY_ENCRYPTED_STATE, null)
        if (!encrypted.isNullOrBlank()) {
            if (encrypted == cachedEncryptedRaw && cachedEstado != null) {
                return cachedEstado
            }
            return runCatching { jsonAEstado(descifrarEstado(encrypted)) }
                .onSuccess { decoded ->
                    cachedEncryptedRaw = encrypted
                    cachedEstado = decoded
                }
                .getOrElse {
                    // Si el contenido fue alterado o la clave dejó de estar disponible, no se usa
                    // información incompleta ni se deja un bloqueo ilegible permanentemente.
                    cachedSecretKey = null
                    cachedEncryptedRaw = null
                    cachedEstado = null
                    sp.edit().clear().apply()
                    null
                }
        }

        // Migración transparente de instalaciones que aún guardaban cada dato en texto plano.
        val legacyAccounts = runCatching {
            val array = JSONArray(sp.getString(KEY_BANK_ACCOUNTS_JSON, "[]").orEmpty())
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val account = array.optJSONObject(index) ?: continue
                    val id = account.optString("id", "")
                    if (id.isBlank()) continue
                    add(BankAccountInfo(
                        id = id,
                        bankName = account.optString("bankName", ""),
                        accountNumber = account.optString("accountNumber", ""),
                        accountType = account.optString("accountType", ""),
                        holderName = account.optString("holderName", ""),
                        holderDocument = account.optString("holderDocument", "")
                    ))
                }
            }
        }.getOrDefault(emptyList())

        val legacy = EstadoBloqueoTransferencia(
            userId = sp.getString(KEY_USER_ID, "").orEmpty(),
            subtotal = sp.getString(KEY_SUBTOTAL, "0.0")?.toDoubleOrNull() ?: 0.0,
            discount = sp.getString(KEY_DISCOUNT, "0.0")?.toDoubleOrNull() ?: 0.0,
            shipping = sp.getString(KEY_SHIPPING, "0.0")?.toDoubleOrNull() ?: 0.0,
            total = sp.getString(KEY_TOTAL, "0.0")?.toDoubleOrNull() ?: 0.0,
            couponCode = sp.getString(KEY_COUPON_CODE, "").orEmpty(),
            customerName = sp.getString(KEY_CUSTOMER_NAME, "").orEmpty(),
            email = sp.getString(KEY_EMAIL, "").orEmpty(),
            phone = sp.getString(KEY_PHONE, "").orEmpty(),
            address = sp.getString(KEY_ADDRESS, "").orEmpty(),
            city = sp.getString(KEY_CITY, "").orEmpty(),
            selectedAccountId = sp.getString(KEY_SELECTED_ACCOUNT_ID, "").orEmpty(),
            pendingTransferOrderId = sp.getString(KEY_PENDING_ORDER_ID, "").orEmpty().takeIf { it.isNotBlank() },
            paymentProofFilePath = sp.getString(KEY_PROOF_FILE_PATH, "").orEmpty().takeIf { it.isNotBlank() },
            bankAccounts = legacyAccounts
        )
        guardar(context, legacy)
        return legacy
    }

    fun tieneBloqueoActivo(context: Context, expectedUserId: String? = null): Boolean = synchronized(lock) {
        val estado = cargarEstadoInterno(context) ?: return@synchronized false
        if (!expectedUserId.isNullOrBlank() && estado.userId.isNotBlank() && estado.userId != expectedUserId) {
            return@synchronized false
        }
        true
    }

    fun guardar(context: Context, estado: EstadoBloqueoTransferencia) = synchronized(lock) {
        val encrypted = cifrarEstado(estadoComoJson(estado))
        cachedEncryptedRaw = encrypted
        cachedEstado = estado
        prefs(context).edit()
            .clear()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_ENCRYPTED_STATE, encrypted)
            .apply()
    }

    fun cargar(context: Context, expectedUserId: String? = null): EstadoBloqueoTransferencia? = synchronized(lock) {
        val estado = cargarEstadoInterno(context) ?: return@synchronized null
        if (!expectedUserId.isNullOrBlank() && estado.userId.isNotBlank() && estado.userId != expectedUserId) {
            return@synchronized null
        }

        val validProofPath = estado.paymentProofFilePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path -> File(path).takeIf { it.isFile && it.length() > 0L }?.absolutePath }

        estado.copy(paymentProofFilePath = validProofPath)
    }

    fun guardarCopiaComprobante(context: Context, sourceUri: Uri): String? {
        return try {
            val appCtx = context.applicationContext
            val resolver = appCtx.contentResolver
            val mimeType = resolver.getType(sourceUri)?.lowercase().orEmpty()
            val extension = when {
                mimeType == "image/png" -> "png"
                mimeType == "image/webp" -> "webp"
                mimeType == "image/jpeg" || mimeType == "image/jpg" -> "jpg"
                else -> {
                    val fromExt = MimeTypeMap.getFileExtensionFromUrl(sourceUri.toString())?.lowercase()
                    when (fromExt) {
                        "png" -> "png"
                        "webp" -> "webp"
                        else -> "jpg"
                    }
                }
            }
            val dir = File(appCtx.filesDir, PROOF_DIR_NAME)
            if (dir.exists()) {
                dir.listFiles()?.forEach { it.delete() }
            } else {
                dir.mkdirs()
            }
            val destFile = File(dir, "comprobante_${System.currentTimeMillis()}.$extension")
            resolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            if (destFile.isFile && destFile.length() > 0L) destFile.absolutePath else null
        } catch (_: Exception) {
            null
        }
    }

    fun eliminarCopiaComprobante(context: Context) {
        try {
            val dir = File(context.applicationContext.filesDir, PROOF_DIR_NAME)
            if (dir.exists()) dir.deleteRecursively()
        } catch (_: Exception) {
        }
    }

    fun limpiar(context: Context) = synchronized(lock) {
        cachedEncryptedRaw = null
        cachedEstado = null
        prefs(context).edit().clear().apply()
        val appCtx = context.applicationContext
        if (isRobolectric() || Looper.myLooper() != Looper.getMainLooper()) {
            eliminarCopiaComprobante(appCtx)
        } else {
            cleanupScope.launch {
                eliminarCopiaComprobante(appCtx)
            }
        }
    }
}
