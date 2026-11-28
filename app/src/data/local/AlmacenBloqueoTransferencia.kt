package com.example.data.local

import android.content.Context
import android.net.Uri
import android.os.Looper
import android.webkit.MimeTypeMap
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.model.BankAccountInfo
import com.example.data.observability.MonitorErrores
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

@Immutable
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

/**
 * Almacén persistente cifrado y asíncrono para el estado de bloqueo de transferencia bancaria,
 * respaldado por Jetpack Preferences [DataStore] y [CifradoKeystoreAesGcm].
 *
 * Migra automáticamente instalaciones anteriores desde `SharedPreferences("elegan_transfer_lock_state")`
 * (tanto el formato cifrado v1 como claves heredadas en texto plano).
 */
object AlmacenBloqueoTransferencia {
    private const val PROOF_DIR_NAME = "pending_transfer_proof"

    internal val KEY_ACTIVE = booleanPreferencesKey("is_active")
    internal val KEY_ENCRYPTED_STATE = stringPreferencesKey("encrypted_state_v1")

    // Claves antiguas: se conservan solo para migrar el estado heredado a la versión cifrada en DataStore.
    private val KEY_USER_ID = stringPreferencesKey("user_id")
    private val KEY_SUBTOTAL = stringPreferencesKey("subtotal")
    private val KEY_DISCOUNT = stringPreferencesKey("discount")
    private val KEY_SHIPPING = stringPreferencesKey("shipping")
    private val KEY_TOTAL = stringPreferencesKey("total")
    private val KEY_COUPON_CODE = stringPreferencesKey("coupon_code")
    private val KEY_CUSTOMER_NAME = stringPreferencesKey("customer_name")
    private val KEY_EMAIL = stringPreferencesKey("email")
    private val KEY_PHONE = stringPreferencesKey("phone")
    private val KEY_ADDRESS = stringPreferencesKey("address")
    private val KEY_CITY = stringPreferencesKey("city")
    private val KEY_SELECTED_ACCOUNT_ID = stringPreferencesKey("selected_account_id")
    private val KEY_PENDING_ORDER_ID = stringPreferencesKey("pending_transfer_order_id")
    private val KEY_PROOF_FILE_PATH = stringPreferencesKey("payment_proof_file_path")
    private val KEY_BANK_ACCOUNTS_JSON = stringPreferencesKey("bank_accounts_json")

    private const val KEY_ALIAS = "elegan_transfer_state_aes_key_v1"

    private data class CacheEntry(
        val encryptedRaw: String?,
        val estado: EstadoBloqueoTransferencia?
    )

    private val cifrador = CifradoKeystoreAesGcm(keyAlias = KEY_ALIAS)
    private val writeMutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private fun store(context: Context): DataStore<Preferences> =
        AlmacenDataStoreElegan.obtenerDataStorePreferencias(
            context = context,
            storeName = AlmacenDataStoreElegan.TRANSFER_LOCK_STORE_NAME,
            legacySharedPrefsName = AlmacenDataStoreElegan.TRANSFER_LOCK_STORE_NAME
        )

    private fun cacheKey(context: Context): String =
        AlmacenDataStoreElegan.claveArchivo(context, AlmacenDataStoreElegan.TRANSFER_LOCK_STORE_NAME)

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
                add(
                    BankAccountInfo(
                        id = id,
                        bankName = account.optString("bankName", ""),
                        accountNumber = account.optString("accountNumber", ""),
                        accountType = account.optString("accountType", ""),
                        holderName = account.optString("holderName", ""),
                        holderDocument = account.optString("holderDocument", "")
                    )
                )
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

    private fun extraerEstadoHeredadoTextoPlano(prefs: Preferences): EstadoBloqueoTransferencia {
        val legacyAccounts = runCatching {
            val array = JSONArray(prefs[KEY_BANK_ACCOUNTS_JSON].orEmpty().ifBlank { "[]" })
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val account = array.optJSONObject(index) ?: continue
                    val id = account.optString("id", "")
                    if (id.isBlank()) continue
                    add(
                        BankAccountInfo(
                            id = id,
                            bankName = account.optString("bankName", ""),
                            accountNumber = account.optString("accountNumber", ""),
                            accountType = account.optString("accountType", ""),
                            holderName = account.optString("holderName", ""),
                            holderDocument = account.optString("holderDocument", "")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())

        return EstadoBloqueoTransferencia(
            userId = prefs[KEY_USER_ID].orEmpty(),
            subtotal = prefs[KEY_SUBTOTAL]?.toDoubleOrNull() ?: 0.0,
            discount = prefs[KEY_DISCOUNT]?.toDoubleOrNull() ?: 0.0,
            shipping = prefs[KEY_SHIPPING]?.toDoubleOrNull() ?: 0.0,
            total = prefs[KEY_TOTAL]?.toDoubleOrNull() ?: 0.0,
            couponCode = prefs[KEY_COUPON_CODE].orEmpty(),
            customerName = prefs[KEY_CUSTOMER_NAME].orEmpty(),
            email = prefs[KEY_EMAIL].orEmpty(),
            phone = prefs[KEY_PHONE].orEmpty(),
            address = prefs[KEY_ADDRESS].orEmpty(),
            city = prefs[KEY_CITY].orEmpty(),
            selectedAccountId = prefs[KEY_SELECTED_ACCOUNT_ID].orEmpty(),
            pendingTransferOrderId = prefs[KEY_PENDING_ORDER_ID].orEmpty().takeIf { it.isNotBlank() },
            paymentProofFilePath = prefs[KEY_PROOF_FILE_PATH].orEmpty().takeIf { it.isNotBlank() },
            bankAccounts = legacyAccounts
        )
    }

    private fun filtrarYValidarEstado(
        context: Context,
        estado: EstadoBloqueoTransferencia?,
        expectedUserId: String?
    ): EstadoBloqueoTransferencia? {
        if (estado == null) return null
        if (!expectedUserId.isNullOrBlank() && estado.userId.isNotBlank() && estado.userId != expectedUserId) {
            return null
        }

        val validProofPath = estado.paymentProofFilePath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                val file = File(path)
                val proofDir = File(context.applicationContext.filesDir, PROOF_DIR_NAME).canonicalPath
                val fileCanonical = runCatching { file.canonicalPath }.getOrNull()
                if (fileCanonical != null &&
                    fileCanonical.startsWith(proofDir + File.separator) &&
                    file.isFile &&
                    file.length() > 0L
                ) {
                    file.absolutePath
                } else {
                    null
                }
            }

        return estado.copy(paymentProofFilePath = validProofPath)
    }

    private fun decodificarPreferencias(
        prefs: Preferences,
        dataStore: DataStore<Preferences>,
        cacheKey: String?
    ): EstadoBloqueoTransferencia? {
        if (prefs[KEY_ACTIVE] != true) {
            if (cacheKey != null) cache[cacheKey] = CacheEntry(null, null)
            return null
        }

        val encrypted = prefs[KEY_ENCRYPTED_STATE]
        if (!encrypted.isNullOrBlank()) {
            val cached = cacheKey?.let { cache[it] }
            if (cached != null && cached.encryptedRaw == encrypted && cached.estado != null) {
                return cached.estado
            }
            return runCatching {
                val plain = cifrador.descifrar(encrypted)
                    ?: error("Formato de estado de transferencia no reconocido.")
                jsonAEstado(plain)
            }
                .onSuccess { decoded ->
                    if (cacheKey != null) {
                        cache[cacheKey] = CacheEntry(encrypted, decoded)
                    }
                }
                .getOrElse { error ->
                    MonitorErrores.registrarAdvertencia(
                        tag = "BloqueoTransferencia",
                        mensaje = "Estado de transferencia cifrado inválido; se restablecerá DataStore.",
                        error = error
                    )
                    cifrador.limpiarCacheClave()
                    if (cacheKey != null) cache[cacheKey] = CacheEntry(null, null)
                    backgroundScope.launch {
                        writeMutex.withLock {
                            runCatching { dataStore.edit { it.clear() } }
                        }
                    }
                    null
                }
        }

        // Migración transparente de instalaciones que aún tenían cada campo en texto plano.
        val legacy = extraerEstadoHeredadoTextoPlano(prefs)
        backgroundScope.launch {
            runCatching { guardarEn(dataStore, legacy, cacheKey) }
        }
        if (cacheKey != null) {
            cache[cacheKey] = CacheEntry(null, legacy)
        }
        return legacy
    }

    fun observarBloqueo(
        context: Context,
        expectedUserId: String? = null
    ): Flow<EstadoBloqueoTransferencia?> =
        observarBloqueoDesde(
            context = context,
            dataStore = store(context),
            cacheKey = cacheKey(context),
            expectedUserId = expectedUserId
        )

    internal fun observarBloqueoDesde(
        context: Context,
        dataStore: DataStore<Preferences>,
        cacheKey: String? = null,
        expectedUserId: String? = null
    ): Flow<EstadoBloqueoTransferencia?> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    MonitorErrores.registrarAdvertencia(
                        tag = "BloqueoTransferencia",
                        mensaje = "Error de lectura en DataStore de transferencia; se emite estado vacío.",
                        error = error
                    )
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs ->
                val raw = decodificarPreferencias(prefs, dataStore, cacheKey)
                filtrarYValidarEstado(context, raw, expectedUserId)
            }
            .distinctUntilChanged()

    suspend fun tieneBloqueoActivoAsync(
        context: Context,
        expectedUserId: String? = null
    ): Boolean = cargarAsync(context, expectedUserId) != null

    suspend fun guardarAsync(context: Context, estado: EstadoBloqueoTransferencia) {
        guardarEn(
            dataStore = store(context),
            estado = estado,
            cacheKey = cacheKey(context)
        )
    }

    internal suspend fun guardarEn(
        dataStore: DataStore<Preferences>,
        estado: EstadoBloqueoTransferencia,
        cacheKey: String? = null
    ) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val encrypted = cifrador.cifrar(estadoComoJson(estado))
            if (cacheKey != null) {
                cache[cacheKey] = CacheEntry(encrypted, estado)
            }
            dataStore.edit { prefs ->
                prefs.clear()
                prefs[KEY_ACTIVE] = true
                prefs[KEY_ENCRYPTED_STATE] = encrypted
            }
        }
    }

    suspend fun cargarAsync(
        context: Context,
        expectedUserId: String? = null
    ): EstadoBloqueoTransferencia? = withContext(Dispatchers.IO) {
        observarBloqueo(context, expectedUserId).first()
    }

    suspend fun limpiarAsync(context: Context) {
        limpiarEn(
            context = context,
            dataStore = store(context),
            cacheKey = cacheKey(context)
        )
    }

    internal suspend fun limpiarEn(
        context: Context,
        dataStore: DataStore<Preferences>,
        cacheKey: String? = null
    ) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            if (cacheKey != null) {
                cache[cacheKey] = CacheEntry(null, null)
            }
            dataStore.edit { it.clear() }
        }
        eliminarCopiaComprobante(context.applicationContext)
    }

    fun obtenerDesdeCache(
        context: Context,
        expectedUserId: String? = null
    ): EstadoBloqueoTransferencia? {
        val entry = cache[cacheKey(context)] ?: return null
        return filtrarYValidarEstado(context, entry.estado, expectedUserId)
    }

    fun tieneBloqueoActivo(context: Context, expectedUserId: String? = null): Boolean =
        cargar(context, expectedUserId) != null

    fun guardar(context: Context, estado: EstadoBloqueoTransferencia) {
        runBlocking(Dispatchers.IO) {
            guardarAsync(context, estado)
        }
    }

    fun cargar(context: Context, expectedUserId: String? = null): EstadoBloqueoTransferencia? {
        val key = cacheKey(context)
        cache[key]?.let { entry ->
            return filtrarYValidarEstado(context, entry.estado, expectedUserId)
        }
        return runBlocking(Dispatchers.IO) {
            cargarAsync(context, expectedUserId)
        }
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

    fun limpiar(context: Context) {
        val key = cacheKey(context)
        cache[key] = CacheEntry(null, null)
        runBlocking(Dispatchers.IO) {
            writeMutex.withLock {
                store(context).edit { it.clear() }
            }
        }
        val appCtx = context.applicationContext
        if (cifrador.isRobolectric() || Looper.myLooper() != Looper.getMainLooper()) {
            eliminarCopiaComprobante(appCtx)
        } else {
            backgroundScope.launch {
                eliminarCopiaComprobante(appCtx)
            }
        }
    }
}
