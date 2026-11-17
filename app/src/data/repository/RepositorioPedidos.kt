package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Order
import com.example.data.model.PayPalOrderSession
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class RepositorioPedidos(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito
) {
    private val ordersList = CopyOnWriteArrayList<Order>()
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    private fun activeBackendToken(): String? {
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    suspend fun refreshOrders(): List<Order> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext ordersList.toList()
        try {
            val response = ApiAutenticacionMovil.service.getOrders("Bearer $token")
            val remoteOrders = extractOrdersJson(response, optionalUserId(appContext).orEmpty())
            ordersList.clear()
            ordersList.addAll(remoteOrders)
            ordersFlow.value = ordersList.toList()
            remoteOrders
        } catch (e: Exception) {
            Log.w("EleganOrders", "No se pudieron obtener las órdenes desde el backend.", e)
            ordersList.toList()
        }
    }

    /**
     * Inicia una orden de pago PayPal en el backend (`POST /api/movil/payments/paypal/orders`).
     * Nota: `POST /api/movil/orders` devuelve HTTP 410 en el backend y fue reemplazado por PayPal.
     */
    suspend fun createPayPalOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        cartItems: List<CartItem>
    ): PayPalOrderSession = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión para realizar un pago con PayPal.")

        val payload = mapOf(
            "customerName" to customerName.trim(),
            "name" to customerName.trim(),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "address" to address.trim(),
            "city" to city.trim(),
            "total" to total,
            "amount" to total,
            "currency" to "USD",
            "itemCount" to itemCount.coerceAtLeast(1L),
            "itemsSummary" to itemsSummary,
            "returnUrl" to "elegan://paypal/return",
            "return_url" to "elegan://paypal/return",
            "cancelUrl" to "elegan://paypal/cancel",
            "cancel_url" to "elegan://paypal/cancel",
            "items" to cartItems.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "product_id" to item.productId,
                    "name" to item.name,
                    "price" to item.price,
                    "quantity" to item.quantity,
                    "selectedSize" to item.selectedSize,
                    "selectedColor" to item.selectedColor
                )
            }
        )

        val response = ApiAutenticacionMovil.service.createPayPalOrder(
            authorization = "Bearer $token",
            request = payload
        )
        extractPayPalSessionJson(response)
            ?: throw IllegalStateException("El servidor no devolvió los datos de aprobación de PayPal.")
    }

    /**
     * Captura y confirma una orden PayPal (`POST /api/movil/payments/paypal/orders/{paypal_order_id}/capture`).
     */
    suspend fun capturePayPalOrder(
        paypalOrderId: String,
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String
    ): String = withContext(Dispatchers.IO) {
        require(paypalOrderId.isNotBlank())
        val token = MobileSession.token(appContext)?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Debes iniciar sesión para confirmar el pago con PayPal.")

        val payload = mapOf(
            "customerName" to customerName.trim(),
            "email" to email.trim(),
            "phone" to phone.trim(),
            "address" to address.trim(),
            "city" to city.trim(),
            "total" to total,
            "itemCount" to itemCount.coerceAtLeast(1L),
            "itemsSummary" to itemsSummary
        )

        val response = ApiAutenticacionMovil.service.capturePayPalOrder(
            authorization = "Bearer $token",
            paypalOrderId = paypalOrderId,
            request = payload
        )
        val createdOrderId = extractCapturedOrderIdJson(response) ?: paypalOrderId

        val capturedOrder = extractSingleOrderJson(response, optionalUserId(appContext).orEmpty())
            ?: Order(
                id = createdOrderId,
                userId = optionalUserId(appContext).orEmpty(),
                customerName = customerName,
                email = email,
                phone = phone,
                address = address,
                city = city,
                paymentMethod = "PayPal",
                total = total,
                itemCount = itemCount.coerceAtLeast(1L),
                status = "pagado",
                itemsSummary = itemsSummary,
                createdAt = nowIsoUtc()
            )

        ordersList.removeAll { it.id == capturedOrder.id }
        ordersList.add(0, capturedOrder)
        ordersFlow.value = ordersList.toList()
        carritoRepo.clearCart(syncRemote = true)
        refreshOrders()
        capturedOrder.id
    }

    suspend fun createOrder(
        customerName: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        paymentMethod: String,
        total: Double,
        itemCount: Long,
        itemsSummary: String,
        paymentProofUri: String = ""
    ): String {
        val orderId = "order_" + UUID.randomUUID().toString().take(8)
        val initialOrder = Order(
            id = orderId,
            userId = optionalUserId(appContext).orEmpty(),
            customerName = customerName,
            email = email,
            phone = phone,
            address = address,
            city = city,
            paymentMethod = paymentMethod,
            paymentProofUrl = paymentProofUri,
            total = total,
            itemCount = itemCount.coerceAtLeast(1L),
            status = "confirmado",
            itemsSummary = itemsSummary,
            createdAt = nowIsoUtc()
        )

        ordersList.add(0, initialOrder)
        ordersFlow.value = ordersList.toList()
        carritoRepo.clearCart()
        return orderId
    }

    fun clearUserData() {
        ordersList.clear()
        ordersFlow.value = emptyList()
    }
}

internal fun extractPayPalSessionJson(response: Map<String, Any?>): PayPalOrderSession? {
    val nested = (response["order"] as? Map<*, *>)
        ?: (response["item"] as? Map<*, *>)
        ?: (response["data"] as? Map<*, *>)
        ?: response

    val orderId = listOf("paypalOrderId", "paypal_order_id", "orderId", "order_id", "id")
        .firstNotNullOfOrNull { key ->
            (nested[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (response[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
        }
        ?: return null

    var approveUrl = listOf(
        "approveUrl", "approve_url", "approvalUrl", "approval_url", "checkoutUrl", "checkout_url", "url"
    ).firstNotNullOfOrNull { key ->
        (nested[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
            ?: (response[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
    }.orEmpty()

    if (approveUrl.isBlank()) {
        val links = (nested["links"] as? List<*>) ?: (response["links"] as? List<*>)
        approveUrl = links?.mapNotNull { it as? Map<*, *> }?.firstOrNull { link ->
            val rel = (link["rel"] as? String)?.lowercase().orEmpty()
            rel == "approve" || rel == "payer-action"
        }?.let { (it["href"] as? String)?.trim() }.orEmpty()
    }

    if (approveUrl.isBlank()) return null

    val status = ((nested["status"] as? String) ?: (response["status"] as? String) ?: "CREATED").trim()
    return PayPalOrderSession(
        paypalOrderId = orderId,
        approveUrl = approveUrl,
        status = status
    )
}

internal fun extractCapturedOrderIdJson(response: Map<String, Any?>): String? {
    val nested = (response["order"] as? Map<*, *>)
        ?: (response["item"] as? Map<*, *>)
        ?: (response["data"] as? Map<*, *>)
        ?: response
    return listOf("orderId", "order_id", "id", "_id", "paypalOrderId", "paypal_order_id")
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
        paymentMethod = firstOrderString(map, "paymentMethod", "payment_method").ifBlank { "PayPal" },
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
