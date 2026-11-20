package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class RepositorioPedidos(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito,
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val tokenProvider: (() -> String?)? = null
) {
    private val ordersList = CopyOnWriteArrayList<Order>()
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    private fun activeBackendToken(): String? {
        if (tokenProvider != null) {
            return tokenProvider.invoke()?.takeIf {
                it.isNotBlank() && !it.startsWith("local_session_")
            }
        }
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    suspend fun refreshOrders(): List<Order> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext ordersList.toList()
        try {
            val response = apiProvider().getOrders("Bearer $token")
            val remoteOrders = extractOrdersJson(response, optionalUserId(appContext).orEmpty())
            ordersList.clear()
            ordersList.addAll(remoteOrders)
            ordersFlow.value = ordersList.toList()
            remoteOrders
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia(
                tag = "EleganOrders",
                mensaje = "No se pudieron obtener las órdenes desde el backend.",
                error = e,
                extras = mapOf("operation" to "refreshOrders")
            )
            ordersList.toList()
        }
    }

    suspend fun validateCoupon(code: String): Map<String, Any?> = withContext(Dispatchers.IO) {
        val token = activeBackendToken()
            ?: throw IllegalStateException("Debes iniciar sesión para validar un cupón.")
        try {
            apiProvider().validateCoupon(
                authorization = "Bearer $token",
                request = mapOf("code" to code.trim().uppercase())
            )
        } catch (e: retrofit2.HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
            val message = when {
                errorBody.contains("COUPON_INVALID_OR_INACTIVE", ignoreCase = true) ->
                    "El cupón no existe o está desactivado."
                errorBody.contains("CART_EMPTY", ignoreCase = true) ->
                    "Tu carrito está vacío. Agrega productos antes de aplicar un cupón."
                else -> "No se pudo validar el cupón. Inténtalo de nuevo."
            }
            throw IllegalStateException(message, e)
        }
    }

    suspend fun getBankAccounts(): List<BankAccountInfo> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: throw IllegalStateException("Debes iniciar sesión.")
        val response = apiProvider().getBankAccounts("Bearer $token")
        val raw = response["items"] as? List<*> ?: emptyList<Any?>()
        raw.mapNotNull { entry ->
            val row = entry as? Map<*, *> ?: return@mapNotNull null
            val id = row["id"]?.toString().orEmpty()
            if (id.isBlank()) return@mapNotNull null
            BankAccountInfo(
                id = id,
                bankName = row["bankName"]?.toString().orEmpty(),
                accountNumber = row["accountNumber"]?.toString().orEmpty(),
                accountType = row["accountType"]?.toString().orEmpty(),
                holderName = row["holderName"]?.toString().orEmpty(),
                holderDocument = row["holderDocument"]?.toString().orEmpty()
            )
        }
    }

    suspend fun createPaymentOrder(
        paymentMethod: String, bankAccountId: String, cashOnDeliveryAccepted: Boolean,
        customerName: String, email: String, phone: String, address: String, city: String,
        couponCode: String
    ): String = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: throw IllegalStateException("Debes iniciar sesión para realizar tu pedido.")
        val payload = mapOf(
            "paymentMethod" to paymentMethod,
            "bankAccountId" to bankAccountId,
            "cashOnDeliveryAccepted" to cashOnDeliveryAccepted,
            "cashOnDeliveryTermsVersion" to if (cashOnDeliveryAccepted) "2026-11-19" else "",
            "customerName" to customerName.trim(), "email" to email.trim(),
            "phone" to phone.trim(), "address" to address.trim(), "city" to city.trim(),
            "couponCode" to couponCode.trim().uppercase()
        )
        val response = apiProvider().createPaymentOrder("Bearer $token", payload)
        val order = response["order"] as? Map<*, *> ?: response
        val id = (order["id"] as? String) ?: (response["id"] as? String)
            ?: throw IllegalStateException("El servidor no devolvió el número de pedido.")
        runCatching { carritoRepo.refreshCart() }
        refreshOrders()
        id
    }

    fun clearUserData() {
        ordersList.clear()
        ordersFlow.value = emptyList()
    }
}

internal fun extractCapturedOrderIdJson(response: Map<String, Any?>): String? {
    val nested = (response["order"] as? Map<*, *>)
        ?: (response["item"] as? Map<*, *>)
        ?: (response["data"] as? Map<*, *>)
        ?: response
    return listOf("orderId", "order_id", "id", "_id")
        .firstNotNullOfOrNull { key ->
            (nested[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (response[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
        }
}

internal fun extractOrdersJson(response: Map<String, Any?>, fallbackUserId: String): List<Order> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["orders"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        mapOrderFromJsonMap(map, fallbackUserId)
    }
}

private fun extractSingleOrderJson(response: Map<String, Any?>, fallbackUserId: String): Order? {
    val map = (response["order"] as? Map<*, *>)
        ?: (response["item"] as? Map<*, *>)
        ?: (response["data"] as? Map<*, *>)
        ?: return null
    return mapOrderFromJsonMap(map, fallbackUserId)
}

private fun mapOrderFromJsonMap(map: Map<*, *>, fallbackUserId: String): Order? {
    val id = firstOrderString(map, "id", "_id", "orderId", "order_id")
    if (id.isBlank()) return null
    val rawTotal = map["total"] ?: map["amount"]
    val total = when (rawTotal) {
        is Number -> rawTotal.toDouble()
        is String -> rawTotal.trim().toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
    val rawCount = map["itemCount"] ?: map["item_count"]
    val itemCount = when (rawCount) {
        is Number -> rawCount.toLong().coerceAtLeast(1L)
        is String -> rawCount.trim().toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        else -> (map["items"] as? List<*>)?.size?.toLong()?.coerceAtLeast(1L) ?: 1L
    }
    val summary = firstOrderString(map, "itemsSummary", "items_summary").ifBlank {
        val rawItems = map["items"] as? List<*>
        rawItems?.mapNotNull { it as? Map<*, *> }?.joinToString(", ") { item ->
            val q = (item["quantity"] as? Number)?.toLong() ?: 1L
            val n = (item["name"] as? String)?.trim().orEmpty()
            if (n.isNotBlank()) "${q}x $n" else ""
        }?.takeIf { it.isNotBlank() }.orEmpty()
    }
    return Order(
        id = id,
        userId = firstOrderString(map, "userId", "user_id").ifBlank { fallbackUserId },
        customerName = firstOrderString(map, "customerName", "customer_name", "name"),
        email = firstOrderString(map, "email"),
        phone = firstOrderString(map, "phone"),
        address = firstOrderString(map, "address"),
        city = firstOrderString(map, "city"),
        paymentMethod = when (firstOrderString(map, "paymentMethod", "payment_method")) {
            "bank_transfer" -> "Transferencia bancaria"
            "cash_on_delivery" -> "Pago contra entrega"
            else -> firstOrderString(map, "paymentMethod", "payment_method").ifBlank { "No especificado" }
        },
        paymentProofUrl = firstOrderString(map, "paymentProofUrl", "payment_proof_url"),
        total = total,
        itemCount = itemCount,
        status = firstOrderString(map, "status").ifBlank { "confirmado" },
        itemsSummary = summary,
        createdAt = firstOrderString(map, "createdAt", "created_at").ifBlank { null }
    )
}

private fun firstOrderString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val v = (map[key] as? String)?.trim()
        if (!v.isNullOrBlank()) return v
    }
    return ""
}
