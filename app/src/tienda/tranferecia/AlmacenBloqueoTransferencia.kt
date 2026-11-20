package com.example.ui

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.example.data.model.BankAccountInfo
import java.io.File
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

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun tieneBloqueoActivo(context: Context, expectedUserId: String? = null): Boolean {
        val sp = prefs(context)
        if (!sp.getBoolean(KEY_ACTIVE, false)) return false
        val savedUserId = sp.getString(KEY_USER_ID, "").orEmpty()
        if (!expectedUserId.isNullOrBlank() && savedUserId.isNotBlank() && savedUserId != expectedUserId) {
            return false
        }
        return true
    }

    fun guardar(context: Context, estado: EstadoBloqueoTransferencia) {
        val accountsJson = JSONArray().apply {
            estado.bankAccounts.forEach { acc ->
                put(
                    JSONObject().apply {
                        put("id", acc.id)
                        put("bankName", acc.bankName)
                        put("accountNumber", acc.accountNumber)
                        put("accountType", acc.accountType)
                        put("holderName", acc.holderName)
                        put("holderDocument", acc.holderDocument)
                    }
                )
            }
        }.toString()

        prefs(context).edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_USER_ID, estado.userId)
            .putString(KEY_SUBTOTAL, estado.subtotal.toString())
            .putString(KEY_DISCOUNT, estado.discount.toString())
            .putString(KEY_SHIPPING, estado.shipping.toString())
            .putString(KEY_TOTAL, estado.total.toString())
            .putString(KEY_COUPON_CODE, estado.couponCode)
            .putString(KEY_CUSTOMER_NAME, estado.customerName)
            .putString(KEY_EMAIL, estado.email)
            .putString(KEY_PHONE, estado.phone)
            .putString(KEY_ADDRESS, estado.address)
            .putString(KEY_CITY, estado.city)
            .putString(KEY_SELECTED_ACCOUNT_ID, estado.selectedAccountId)
            .putString(KEY_PENDING_ORDER_ID, estado.pendingTransferOrderId.orEmpty())
            .putString(KEY_PROOF_FILE_PATH, estado.paymentProofFilePath.orEmpty())
            .putString(KEY_BANK_ACCOUNTS_JSON, accountsJson)
            .commit()
    }

    fun cargar(context: Context, expectedUserId: String? = null): EstadoBloqueoTransferencia? {
        val sp = prefs(context)
        if (!sp.getBoolean(KEY_ACTIVE, false)) return null
        val savedUserId = sp.getString(KEY_USER_ID, "").orEmpty()
        if (!expectedUserId.isNullOrBlank() && savedUserId.isNotBlank() && savedUserId != expectedUserId) {
            return null
        }

        val rawProofPath = sp.getString(KEY_PROOF_FILE_PATH, "").orEmpty()
        val validProofPath = rawProofPath
            .takeIf { it.isNotBlank() }
            ?.let { path -> File(path).takeIf { it.isFile && it.length() > 0L }?.absolutePath }

        val rawPendingOrderId = sp.getString(KEY_PENDING_ORDER_ID, "").orEmpty().takeIf { it.isNotBlank() }

        val accounts = parseBankAccounts(sp.getString(KEY_BANK_ACCOUNTS_JSON, "[]").orEmpty())

        return EstadoBloqueoTransferencia(
            userId = savedUserId,
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
            pendingTransferOrderId = rawPendingOrderId,
            paymentProofFilePath = validProofPath,
            bankAccounts = accounts
        )
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
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
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
        prefs(context).edit().clear().commit()
        eliminarCopiaComprobante(context)
    }

    private fun parseBankAccounts(rawJson: String): List<BankAccountInfo> {
        if (rawJson.isBlank()) return emptyList()
        return try {
            val array = JSONArray(rawJson)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    if (id.isBlank()) continue
                    add(
                        BankAccountInfo(
                            id = id,
                            bankName = obj.optString("bankName", ""),
                            accountNumber = obj.optString("accountNumber", ""),
                            accountType = obj.optString("accountType", ""),
                            holderName = obj.optString("holderName", ""),
                            holderDocument = obj.optString("holderDocument", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
