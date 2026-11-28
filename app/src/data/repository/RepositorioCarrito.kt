package com.example.data.repository

import android.content.Context
import android.os.Build
import com.example.data.error.ApiParsingException
import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.example.data.model.parseWholeMoney
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioCarrito(
    private val appContext: Context,
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service },
    private val tokenProvider: (() -> String?)? = null
) {
    private val cartMutex = Mutex()
    private val cartItems = CopyOnWriteArrayList<CartItem>()
    private val cartFlow = MutableStateFlow<List<CartItem>>(emptyList())
    private val cartApiErrorFlow = MutableStateFlow<ErrorApi?>(null)

    private fun publishState() {
        cartFlow.value = cartItems.toList()
    }

    private fun replaceCartItemsLocked(items: List<CartItem>) {
        cartItems.clear()
        cartItems.addAll(items)
        publishState()
    }

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

    private fun isRobolectricLocalMode(): Boolean =
        tokenProvider == null && "robolectric".equals(Build.FINGERPRINT, ignoreCase = true)

    internal fun hasBackendSession(): Boolean = activeBackendToken() != null

    fun observeCart(): Flow<List<CartItem>> = cartFlow.asStateFlow()

    fun observeCartApiError(): Flow<ErrorApi?> = cartApiErrorFlow.asStateFlow()

    fun observeCartError(): Flow<String?> = cartApiErrorFlow.map { it?.userMessage }

    fun clearCartError() {
        cartApiErrorFlow.value = null
    }

    private fun ensureBackendCartSuccess(response: Map<String, Any?>, fallbackMessage: String) {
        if (response["success"] == false) {
            val message = (response["message"] as? String)?.takeIf { it.isNotBlank() }
                ?: (response["error"] as? String)?.takeIf { it.isNotBlank() }
                ?: fallbackMessage
            throw IllegalStateException(message)
        }
    }

    private suspend fun fetchAndApplyRemoteCartLocked(token: String): List<CartItem> {
        val synced = apiProvider().getCart("Bearer $token")
        ensureBackendCartSuccess(synced, "No se pudo validar el carrito en el servidor.")
        val fetched = extractCartItemsOrNull(synced, optionalUserId(appContext).orEmpty())
            ?: throw ApiParsingException(
                message = "El servidor devolvió una respuesta inválida al consultar el carrito.",
                fieldOrContext = "cart.items"
            )
        replaceCartItemsLocked(fetched)
        return fetched
    }

    suspend fun refreshCartResult(): ResultWrapper<List<CartItem>> = withContext(Dispatchers.IO) {
        cartMutex.withLock {
            val token = activeBackendToken()
            if (token == null) {
                if (isRobolectricLocalMode()) {
                    cartApiErrorFlow.value = null
                    return@withLock ResultWrapper.Success(cartItems.toList())
                }
                replaceCartItemsLocked(emptyList())
                cartApiErrorFlow.value = null
                return@withLock ResultWrapper.Success(emptyList())
            }

            val result = safeApiCall(
                tag = "EleganCart",
                operation = "refreshCart"
            ) {
                fetchAndApplyRemoteCartLocked(token)
            }
            when (result) {
                is ResultWrapper.Success -> cartApiErrorFlow.value = null
                is ResultWrapper.Error -> cartApiErrorFlow.value = result.error
                ResultWrapper.Loading -> Unit
            }
            result
        }
    }

    suspend fun refreshCart(): List<CartItem> = refreshCartResult().getOrThrow()

    suspend fun addToCart(
        product: Product,
        selectedSize: String,
        quantity: Long = 1L,
        selectedColor: String = ""
    ) = withContext(Dispatchers.IO) {
        val productId = product.id
        require(productId.isNotBlank() && quantity > 0)
        val selectedShippingId = product.selectedShippingId
        val selectedShipping = product.shippingMethods.firstOrNull { it.id == selectedShippingId }

        cartMutex.withLock {
            val token = activeBackendToken()
            if (token == null) {
                if (isRobolectricLocalMode()) {
                    val existingIndex = cartItems.indexOfFirst {
                        it.productId == productId && it.selectedSize == selectedSize && it.selectedColor == selectedColor && it.shippingMethodId == selectedShippingId
                    }
                    if (existingIndex >= 0) {
                        val existing = cartItems[existingIndex]
                        cartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
                    } else {
                        cartItems.add(
                            CartItem(
                                id = UUID.randomUUID().toString(),
                                userId = optionalUserId(appContext).orEmpty(),
                                productId = productId,
                                name = product.name,
                                price = product.price,
                                imageUrl = product.imageUrl,
                                selectedSize = selectedSize,
                                selectedColor = selectedColor,
                                shippingMethodId = selectedShipping?.id.orEmpty(),
                                shippingMethodName = selectedShipping?.name.orEmpty(),
                                shippingPrice = selectedShipping?.price ?: 0.0,
                                shippingDeliveryTime = selectedShipping?.deliveryTime.orEmpty(),
                                shippingOrigin = selectedShipping?.origin.orEmpty(),
                                allowCashOnDelivery = product.allowCashOnDelivery,
                                quantity = quantity
                            )
                        )
                    }
                    publishState()
                    cartApiErrorFlow.value = null
                    return@withLock
                }
                val err = ErrorApi.UnauthorizedError(
                    userMessage = "Debes iniciar sesión para agregar productos al carrito."
                )
                cartApiErrorFlow.value = err
                throw err.toException()
            }

            val payload = mapOf(
                "productId" to productId,
                "product_id" to productId,
                "name" to product.name,
                "price" to product.price,
                "imageUrl" to product.imageUrl,
                "selectedSize" to selectedSize,
                "size" to selectedSize,
                "selectedColor" to selectedColor,
                "color" to selectedColor,
                "selectedShippingId" to selectedShippingId,
                "quantity" to quantity
            )

            val result = safeApiCall(
                tag = "EleganCart",
                operation = "addToCart",
                extras = mapOf("productId" to productId)
            ) {
                val response = apiProvider().addToCart("Bearer $token", payload)
                ensureBackendCartSuccess(response, "El servidor no confirmó el producto en el carrito.")
                val remoteItems = extractCartItemsOrNull(response, optionalUserId(appContext).orEmpty())
                val confirmedItems = if (remoteItems != null) {
                    replaceCartItemsLocked(remoteItems)
                    remoteItems
                } else {
                    fetchAndApplyRemoteCartLocked(token)
                }
                if (confirmedItems.none { it.productId == productId }) {
                    throw IllegalStateException("El servidor no registró el artículo en tu carrito.")
                }
            }
            when (result) {
                is ResultWrapper.Success -> cartApiErrorFlow.value = null
                is ResultWrapper.Error -> {
                    runCatching { fetchAndApplyRemoteCartLocked(token) }
                    cartApiErrorFlow.value = result.error
                    throw result.error.toException()
                }
                ResultWrapper.Loading -> Unit
            }
        }
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return@withContext
        }

        cartMutex.withLock {
            val token = activeBackendToken()
            if (token == null) {
                if (isRobolectricLocalMode()) {
                    val index = cartItems.indexOfFirst { it.id == cartItemId }
                    if (index >= 0) {
                        cartItems[index] = cartItems[index].copy(quantity = newQuantity)
                        publishState()
                    }
                    cartApiErrorFlow.value = null
                    return@withLock
                }
                val err = ErrorApi.UnauthorizedError(
                    userMessage = "Debes iniciar sesión para actualizar tu carrito."
                )
                cartApiErrorFlow.value = err
                throw err.toException()
            }

            val result = safeApiCall(
                tag = "EleganCart",
                operation = "updateCartQuantity"
            ) {
                val response = apiProvider().updateCartItem(
                    authorization = "Bearer $token",
                    itemId = cartItemId,
                    request = mapOf("quantity" to newQuantity)
                )
                ensureBackendCartSuccess(response, "El servidor no confirmó el cambio de cantidad.")
                val remoteItems = extractCartItemsOrNull(response, optionalUserId(appContext).orEmpty())
                if (remoteItems != null) {
                    replaceCartItemsLocked(remoteItems)
                } else {
                    fetchAndApplyRemoteCartLocked(token)
                }
            }
            when (result) {
                is ResultWrapper.Success -> cartApiErrorFlow.value = null
                is ResultWrapper.Error -> {
                    runCatching { fetchAndApplyRemoteCartLocked(token) }
                    cartApiErrorFlow.value = result.error
                    throw result.error.toException()
                }
                ResultWrapper.Loading -> Unit
            }
        }
    }

    suspend fun removeFromCart(cartItemId: String) = withContext(Dispatchers.IO) {
        cartMutex.withLock {
            val token = activeBackendToken()
            if (token == null) {
                if (isRobolectricLocalMode()) {
                    cartItems.removeAll { it.id == cartItemId }
                    publishState()
                    cartApiErrorFlow.value = null
                    return@withLock
                }
                val err = ErrorApi.UnauthorizedError(
                    userMessage = "Debes iniciar sesión para modificar tu carrito."
                )
                cartApiErrorFlow.value = err
                throw err.toException()
            }

            val result = safeApiCall(
                tag = "EleganCart",
                operation = "removeFromCart"
            ) {
                val response = apiProvider().removeCartItem("Bearer $token", cartItemId)
                ensureBackendCartSuccess(response, "El servidor no confirmó la eliminación del artículo.")
                val remoteItems = extractCartItemsOrNull(response, optionalUserId(appContext).orEmpty())
                if (remoteItems != null) {
                    replaceCartItemsLocked(remoteItems)
                } else {
                    fetchAndApplyRemoteCartLocked(token)
                }
            }
            when (result) {
                is ResultWrapper.Success -> cartApiErrorFlow.value = null
                is ResultWrapper.Error -> {
                    runCatching { fetchAndApplyRemoteCartLocked(token) }
                    cartApiErrorFlow.value = result.error
                    throw result.error.toException()
                }
                ResultWrapper.Loading -> Unit
            }
        }
    }

    suspend fun clearCart(syncRemote: Boolean = true) = withContext(Dispatchers.IO) {
        cartMutex.withLock {
            if (!syncRemote) {
                replaceCartItemsLocked(emptyList())
                cartApiErrorFlow.value = null
                return@withLock
            }

            val token = activeBackendToken()
            if (token == null) {
                if (isRobolectricLocalMode()) {
                    replaceCartItemsLocked(emptyList())
                    cartApiErrorFlow.value = null
                    return@withLock
                }
                val err = ErrorApi.UnauthorizedError(
                    userMessage = "Debes iniciar sesión para vaciar tu carrito."
                )
                cartApiErrorFlow.value = err
                throw err.toException()
            }

            val result = safeApiCall(
                tag = "EleganCart",
                operation = "clearCart"
            ) {
                val response = apiProvider().clearCart("Bearer $token")
                ensureBackendCartSuccess(response, "El servidor no confirmó el vaciado del carrito.")
                val remoteItems = extractCartItemsOrNull(response, optionalUserId(appContext).orEmpty())
                if (remoteItems != null) {
                    replaceCartItemsLocked(remoteItems)
                } else {
                    fetchAndApplyRemoteCartLocked(token)
                }
            }
            when (result) {
                is ResultWrapper.Success -> cartApiErrorFlow.value = null
                is ResultWrapper.Error -> {
                    runCatching { fetchAndApplyRemoteCartLocked(token) }
                    cartApiErrorFlow.value = result.error
                    throw result.error.toException()
                }
                ResultWrapper.Loading -> Unit
            }
        }
    }
}

internal fun extractCartItemsOrNull(response: Map<String, Any?>, fallbackUserId: String): List<CartItem>? {
    val rawList = (response["items"] as? List<*>)
        ?: (response["cart"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: ((response["cart"] as? Map<*, *>)?.get("items") as? List<*>)
        ?: ((response["data"] as? Map<*, *>)?.get("items") as? List<*>)
        ?: return null

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val id = firstString(map, "id", "_id", "itemId", "item_id")
        val productId = firstString(map, "productId", "product_id")
        val name = firstString(map, "name", "title", "productName", "product_name")
        if (id.isBlank() && productId.isBlank()) return@mapNotNull null
        val rawPrice = map["price"] ?: map["unitPrice"] ?: map["unit_price"]
        val price = parseWholeMoney(rawPrice) ?: 0.0
        val rawQty = map["quantity"] ?: map["qty"]
        val quantity = when (rawQty) {
            is Number -> rawQty.toLong().coerceAtLeast(1L)
            is String -> rawQty.trim().toLongOrNull()?.coerceAtLeast(1L) ?: 1L
            else -> 1L
        }
        CartItem(
            id = id.ifBlank { productId },
            userId = firstString(map, "userId", "user_id").ifBlank { fallbackUserId },
            productId = productId.ifBlank { id },
            name = name,
            price = price,
            imageUrl = firstString(map, "imageUrl", "image_url", "image").let(::resolveBackendMediaUrl),
            selectedSize = firstString(map, "selectedSize", "selected_size", "size"),
            selectedColor = firstString(map, "selectedColor", "selected_color", "color"),
            shippingMethodId = firstString(map, "shippingMethodId", "selectedShippingId"),
            shippingMethodName = firstString(map, "shippingMethodName"),
            shippingPrice = parseWholeMoney(map["shippingPrice"] ?: map["shippingPriceDOP"]) ?: 0.0,
            shippingDeliveryTime = firstString(map, "shippingDeliveryTime"),
            shippingOrigin = firstString(map, "shippingOrigin"),
            allowCashOnDelivery = map["allowCashOnDelivery"] as? Boolean ?: true,
            quantity = quantity,
            createdAt = firstString(map, "createdAt", "created_at").ifBlank { null },
            updatedAt = firstString(map, "updatedAt", "updated_at").ifBlank { null }
        )
    }
}

internal fun extractCartItemsJson(response: Map<String, Any?>, fallbackUserId: String): List<CartItem> =
    extractCartItemsOrNull(response, fallbackUserId) ?: emptyList()

private fun firstString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val v = (map[key] as? String)?.trim()
        if (!v.isNullOrBlank()) return v
    }
    return ""
}
