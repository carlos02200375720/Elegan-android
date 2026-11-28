package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.error.ApiParsingException
import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.CashOnDeliveryConfig
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.model.parseWholeMoney
import com.example.data.model.toCashOnDeliveryConfig
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.util.concurrent.CopyOnWriteArrayList
import org.json.JSONObject
import retrofit2.HttpException
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
    private val ordersApiErrorFlow = MutableStateFlow<ErrorApi?>(null)

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

    fun observeOrdersApiError(): Flow<ErrorApi?> = ordersApiErrorFlow.asStateFlow()

    suspend fun refreshOrdersResult(): ResultWrapper<List<Order>> {
        val token = activeBackendToken() ?: return ResultWrapper.Success(ordersList.toList())
        val result = safeApiCall(
            tag = "EleganOrders",
            operation = "refreshOrders"
        ) {
            val response = apiProvider().getOrders("Bearer $token")
            val remoteOrders = extractOrdersJson(response, optionalUserId(appContext).orEmpty())
            ordersList.clear()
            ordersList.addAll(remoteOrders)
            ordersFlow.value = ordersList.toList()
            remoteOrders
        }
        when (result) {
            is ResultWrapper.Success -> ordersApiErrorFlow.value = null
            is ResultWrapper.Error -> ordersApiErrorFlow.value = result.error
            ResultWrapper.Loading -> Unit
        }
        return result
    }

    suspend fun refreshOrders(): List<Order> =
        refreshOrdersResult().getOrElse { ordersList.toList() }

    suspend fun validateCoupon(code: String, selectedCartItemIds: List<String>): Map<String, Any?> = withContext(Dispatchers.IO) {
        val token = activeBackendToken()
            ?: throw IllegalStateException("Debes iniciar sesión para validar un cupón.")
        try {
            apiProvider().validateCoupon(
                authorization = "Bearer $token",
                request = mapOf("code" to code.trim().uppercase(), "selectedCartItemIds" to selectedCartItemIds)
            )
        } catch (e: HttpException) {
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

    suspend fun getCashOnDeliveryConfigResult(): ResultWrapper<CashOnDeliveryConfig> =
        safeApiCall(
            tag = "EleganOrders",
            operation = "getCashOnDeliveryConfig"
        ) {
            apiProvider().getCashOnDeliveryConfig().toCashOnDeliveryConfig()
        }

    suspend fun getCashOnDeliveryConfig(): CashOnDeliveryConfig =
        getCashOnDeliveryConfigResult().getOrThrow()

    suspend fun getBankAccountsResult(): ResultWrapper<List<BankAccountInfo>> {
        val token = activeBackendToken()
            ?: return ResultWrapper.Error(ErrorApi.UnauthorizedError(userMessage = "Debes iniciar sesión."))
        return safeApiCall(
            tag = "EleganOrders",
            operation = "getBankAccounts"
        ) {
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
    }

    suspend fun getBankAccounts(): List<BankAccountInfo> =
        getBankAccountsResult().getOrThrow()

    suspend fun createPaymentOrder(
        paymentMethod: String, bankAccountId: String, cashOnDeliveryAccepted: Boolean,
        customerName: String, email: String, phone: String, address: String, city: String,
        couponCode: String, selectedCartItemIds: List<String>
    ): String = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: throw IllegalStateException("Debes iniciar sesión para realizar tu pedido.")
        val payload = mapOf(
            "paymentMethod" to paymentMethod,
            "bankAccountId" to bankAccountId,
            "cashOnDeliveryAccepted" to cashOnDeliveryAccepted,
            "cashOnDeliveryTermsVersion" to if (cashOnDeliveryAccepted) "2026-11-19" else "",
            "customerName" to customerName.trim(), "email" to email.trim(),
            "phone" to phone.trim(), "address" to address.trim(), "city" to city.trim(),
            "couponCode" to couponCode.trim().uppercase(),
            "selectedCartItemIds" to selectedCartItemIds
        )
        val response = try {
            apiProvider().createPaymentOrder("Bearer $token", payload)
        } catch (e: HttpException) {
            val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
            val detail = runCatching {
                body?.let { JSONObject(it).optString("detail").takeIf(String::isNotBlank) }
            }.getOrNull() ?: body?.takeIf { it.isNotBlank() }
            throw ErrorCrearPedidoApi(e.code(), detail, e)
        }
        val order = response["order"] as? Map<*, *> ?: response
        val id = (order["id"] as? String) ?: (response["id"] as? String)
            ?: throw ApiParsingException(
                message = "El servidor no devolvió el número de pedido.",
                fieldOrContext = "order.id"
            )
        runCatching { carritoRepo.refreshCart() }
        refreshOrders()
        id
    }

    fun clearUserData() {
        ordersList.clear()
        ordersFlow.value = emptyList()
        ordersApiErrorFlow.value = null
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

    val parsedItems = extractOrderItemsFromMap(map)
    val shippingMap = (map["shipping"] as? Map<*, *>)
        ?: (map["shippingMethod"] as? Map<*, *>)
        ?: (map["shipping_method"] as? Map<*, *>)

    val total = firstOrderDouble(map, "total", "totalDOP", "total_dop", "amount")
    val explicitSubtotal = firstOrderDouble(map, "subtotal", "sub_total", "subtotalDOP", "subtotal_dop")
    val computedSubtotal = parsedItems.sumOf { it.price * it.quantity }
    val subtotal = if (explicitSubtotal > 0.0) explicitSubtotal else computedSubtotal

    val discount = firstOrderDouble(
        map,
        "discount",
        "discountAmount",
        "discount_amount",
        "discountAmountDOP",
        "discount_amount_dop"
    )

    val explicitShippingCost = firstOrderDouble(
        map,
        "shippingCost",
        "shipping_cost",
        "shippingPrice",
        "shipping_price",
        "shippingTotal",
        "shipping_total",
        "shippingCostDOP",
        "shippingPriceDOP",
        "shipping_dop"
    ).let { direct ->
        if (direct > 0.0) direct
        else if (shippingMap != null) firstOrderDouble(shippingMap, "price", "cost", "amount", "priceDOP")
        else 0.0
    }
    val computedItemsShipping = parsedItems.sumOf { it.shippingPrice }
    val shippingCost = if (explicitShippingCost > 0.0) explicitShippingCost else computedItemsShipping

    val shippingMethodName = firstOrderString(
        map,
        "shippingMethodName",
        "shipping_method_name",
        "shippingName",
        "shipping_name",
        "shippingMethod",
        "shipping_method"
    ).ifBlank {
        shippingMap?.let { firstOrderString(it, "name", "title", "methodName", "label") }.orEmpty()
    }.ifBlank {
        parsedItems.map { it.shippingMethodName }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
    }

    val shippingDeliveryTime = firstOrderString(
        map,
        "shippingDeliveryTime",
        "shipping_delivery_time",
        "deliveryTime",
        "delivery_time",
        "estimatedDelivery",
        "estimated_delivery"
    ).ifBlank {
        shippingMap?.let { firstOrderString(it, "deliveryTime", "delivery_time", "estimatedDelivery", "eta") }.orEmpty()
    }.ifBlank {
        parsedItems.map { it.shippingDeliveryTime }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
    }

    val shippingOrigin = firstOrderString(
        map,
        "shippingOrigin",
        "shipping_origin",
        "origin"
    ).ifBlank {
        shippingMap?.let { firstOrderString(it, "origin", "shippingOrigin", "country") }.orEmpty()
    }.ifBlank {
        parsedItems.map { it.shippingOrigin }.filter { it.isNotBlank() }.distinct().joinToString(" / ")
    }

    val rawCount = map["itemCount"] ?: map["item_count"]
    val itemCount = when (rawCount) {
        is Number -> rawCount.toLong().coerceAtLeast(1L)
        is String -> rawCount.trim().toLongOrNull()?.coerceAtLeast(1L) ?: 1L
        else -> parsedItems.sumOf { it.quantity }.takeIf { it > 0L } ?: 1L
    }

    val summary = firstOrderString(map, "itemsSummary", "items_summary").ifBlank {
        parsedItems.joinToString(", ") { item ->
            if (item.name.isNotBlank()) "${item.quantity}x ${item.name}" else ""
        }.trim().trimEnd(',')
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
        subtotal = subtotal,
        discount = discount,
        shippingCost = shippingCost,
        shippingMethodName = shippingMethodName,
        shippingDeliveryTime = shippingDeliveryTime,
        shippingOrigin = shippingOrigin,
        total = total,
        itemCount = itemCount,
        status = firstOrderString(map, "status").ifBlank { "confirmado" },
        itemsSummary = summary,
        items = parsedItems,
        createdAt = firstOrderString(map, "createdAt", "created_at").ifBlank { null }
    )
}

private fun extractOrderItemsFromMap(map: Map<*, *>): List<OrderItem> {
    val rawItems = (map["items"] as? List<*>)
        ?: (map["orderItems"] as? List<*>)
        ?: (map["order_items"] as? List<*>)
        ?: (map["products"] as? List<*>)
        ?: return emptyList()

    return rawItems.mapNotNull { entry ->
        val itemMap = entry as? Map<*, *> ?: return@mapNotNull null
        val nestedProduct = itemMap["product"] as? Map<*, *>
        val nestedShipping = (itemMap["shipping"] as? Map<*, *>)
            ?: (itemMap["shippingMethod"] as? Map<*, *>)
            ?: (itemMap["shipping_method"] as? Map<*, *>)

        val productId = firstOrderString(itemMap, "productId", "product_id", "id", "_id")
            .ifBlank { nestedProduct?.let { firstOrderString(it, "id", "_id") }.orEmpty() }
        val name = firstOrderString(itemMap, "name", "title", "productName", "product_name")
            .ifBlank { nestedProduct?.let { firstOrderString(it, "name", "title") }.orEmpty() }

        val rawImageUrl = firstOrderString(
            itemMap,
            "imageUrl",
            "image_url",
            "image",
            "photoUrl",
            "photo_url",
            "thumbnail",
            "coverImage"
        ).ifBlank {
            nestedProduct?.let {
                firstOrderString(it, "imageUrl", "image_url", "image", "coverImage")
            }.orEmpty()
        }
        val imageUrl = resolveBackendMediaUrl(rawImageUrl)

        val price = firstOrderDouble(
            itemMap,
            "price",
            "unitPrice",
            "unit_price",
            "priceDOP",
            "price_dop"
        ).let { direct ->
            if (direct > 0.0) direct
            else nestedProduct?.let { firstOrderDouble(it, "price", "priceDOP") } ?: 0.0
        }

        val rawQty = itemMap["quantity"] ?: itemMap["qty"]
        val quantity = when (rawQty) {
            is Number -> rawQty.toLong().coerceAtLeast(1L)
            is String -> rawQty.trim().toLongOrNull()?.coerceAtLeast(1L) ?: 1L
            else -> 1L
        }

        val shippingMethodName = firstOrderString(
            itemMap,
            "shippingMethodName",
            "shipping_method_name",
            "shippingName",
            "shipping_name",
            "shippingMethod",
            "shipping_method"
        ).ifBlank {
            nestedShipping?.let { firstOrderString(it, "name", "title", "methodName", "label") }.orEmpty()
        }

        val shippingPrice = firstOrderDouble(
            itemMap,
            "shippingPrice",
            "shipping_price",
            "shippingPriceDOP",
            "shipping_price_dop",
            "shippingCost",
            "shipping_cost"
        ).let { direct ->
            if (direct > 0.0) direct
            else nestedShipping?.let { firstOrderDouble(it, "price", "cost", "amount", "priceDOP") } ?: 0.0
        }

        val shippingDeliveryTime = firstOrderString(
            itemMap,
            "shippingDeliveryTime",
            "shipping_delivery_time",
            "deliveryTime",
            "delivery_time",
            "estimatedDelivery",
            "estimated_delivery"
        ).ifBlank {
            nestedShipping?.let { firstOrderString(it, "deliveryTime", "delivery_time", "estimatedDelivery", "eta") }.orEmpty()
        }

        val shippingOrigin = firstOrderString(
            itemMap,
            "shippingOrigin",
            "shipping_origin",
            "origin"
        ).ifBlank {
            nestedShipping?.let { firstOrderString(it, "origin", "shippingOrigin", "country") }.orEmpty()
        }

        if (name.isBlank() && productId.isBlank()) return@mapNotNull null

        OrderItem(
            productId = productId,
            name = name.ifBlank { "Producto" },
            imageUrl = imageUrl,
            price = price,
            quantity = quantity,
            selectedSize = firstOrderString(itemMap, "selectedSize", "selected_size", "size"),
            selectedColor = firstOrderString(itemMap, "selectedColor", "selected_color", "color"),
            shippingMethodName = shippingMethodName,
            shippingPrice = shippingPrice,
            shippingDeliveryTime = shippingDeliveryTime,
            shippingOrigin = shippingOrigin
        )
    }
}

private fun firstOrderDouble(map: Map<*, *>, vararg keys: String): Double {
    for (key in keys) {
        val parsed = parseWholeMoney(map[key]) ?: continue
        return parsed
    }
    return 0.0
}

private fun firstOrderString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val v = (map[key] as? String)?.trim()
        if (!v.isNullOrBlank()) return v
    }
    return ""
}


internal class ErrorCrearPedidoApi(
    val statusCode: Int,
    val backendDetail: String?,
    cause: Throwable
) : IllegalStateException(
    when (backendDetail) {
        "CART_EMPTY" ->
            "Tu carrito está vacío en el servidor. Vuelve al carrito, actualízalo y vuelve a intentar."
        "DELIVERY_FIELDS_REQUIRED" ->
            "Faltan datos de entrega. Revisa tu nombre, teléfono, dirección y ciudad."
        "INVALID_PAYMENT_METHOD" ->
            "El método de pago no es válido. Actualiza la aplicación e inténtalo otra vez."
        "BANK_ACCOUNT_UNAVAILABLE" ->
            "La cuenta bancaria seleccionada ya no está disponible. Recarga las cuentas bancarias."
        "COD_DISABLED" ->
            "El pago contra entrega fue desactivado por la tienda. Selecciona otro método de pago."
        "COD_PRODUCT_NOT_ALLOWED" ->
            "Uno de los productos seleccionados no permite pago contra entrega. Selecciona transferencia bancaria."
        "CART_ITEMS_CHANGED" ->
            "El carrito cambió. Actualiza los productos seleccionados e inténtalo de nuevo."
        "CART_ITEMS_SELECTION_REQUIRED" ->
            "Selecciona al menos un producto antes de confirmar el pedido."
        else ->
            "El servidor rechazó el pedido (HTTP $statusCode: ${backendDetail ?: "sin detalle"})."
    },
    cause
)
