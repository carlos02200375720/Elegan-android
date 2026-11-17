package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.ProductJson
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

internal class RepositorioCarrito(private val appContext: Context) {

    private val cartItems = CopyOnWriteArrayList<CartItem>()
    private val cartFlow = MutableStateFlow<List<CartItem>>(emptyList())

    private fun publishState() {
        cartFlow.value = cartItems.toList()
    }

    private fun activeBackendToken(): String? {
        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) return null
        return MobileSession.token(appContext)?.takeIf {
            it.isNotBlank() && !it.startsWith("local_session_")
        }
    }

    fun observeCart(): Flow<List<CartItem>> = cartFlow.asStateFlow()

    suspend fun refreshCart(): List<CartItem> = withContext(Dispatchers.IO) {
        val token = activeBackendToken() ?: return@withContext cartItems.toList()
        try {
            val response = ApiAutenticacionMovil.service.getCart("Bearer $token")
            val remoteItems = extractCartItemsJson(response, optionalUserId(appContext).orEmpty())
            cartItems.clear()
            cartItems.addAll(remoteItems)
            publishState()
            remoteItems
        } catch (e: Exception) {
            Log.w("EleganCart", "No se pudo sincronizar el carrito desde el backend.", e)
            cartItems.toList()
        }
    }

    suspend fun addToCart(
        product: ProductJson,
        selectedSize: String,
        quantity: Long = 1L,
        selectedColor: String = ""
    ) = withContext(Dispatchers.IO) {
        val productId = product.productId()
        require(productId.isNotBlank() && quantity > 0)

        val token = activeBackendToken()
        if (token != null) {
            try {
                val payload = mapOf(
                    "productId" to productId,
                    "product_id" to productId,
                    "name" to product.productName(),
                    "price" to product.productPrice(),
                    "imageUrl" to product.productImageUrl(),
                    "selectedSize" to selectedSize,
                    "size" to selectedSize,
                    "selectedColor" to selectedColor,
                    "color" to selectedColor,
                    "quantity" to quantity
                )
                val response = ApiAutenticacionMovil.service.addToCart("Bearer $token", payload)
                val remoteItems = extractCartItemsJson(response, optionalUserId(appContext).orEmpty())
                if (remoteItems.isNotEmpty()) {
                    cartItems.clear()
                    cartItems.addAll(remoteItems)
                    publishState()
                    return@withContext
                } else {
                    val synced = ApiAutenticacionMovil.service.getCart("Bearer $token")
                    val fetched = extractCartItemsJson(synced, optionalUserId(appContext).orEmpty())
                    if (fetched.isNotEmpty()) {
                        cartItems.clear()
                        cartItems.addAll(fetched)
                        publishState()
                        return@withContext
                    }
                }
            } catch (e: Exception) {
                Log.w("EleganCart", "No se pudo agregar al carrito en el backend; se conserva en sesión.", e)
            }
        }

        val existingIndex = cartItems.indexOfFirst {
            it.productId == productId && it.selectedSize == selectedSize && it.selectedColor == selectedColor
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
                    name = product.productName(),
                    price = product.productPrice(),
                    imageUrl = product.productImageUrl(),
                    selectedSize = selectedSize,
                    selectedColor = selectedColor,
                    quantity = quantity
                )
            )
        }
        publishState()
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return@withContext
        }
        val index = cartItems.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            cartItems[index] = cartItems[index].copy(quantity = newQuantity)
            publishState()
        }

        val token = activeBackendToken() ?: return@withContext
        try {
            val response = ApiAutenticacionMovil.service.updateCartItem(
                authorization = "Bearer $token",
                itemId = cartItemId,
                request = mapOf("quantity" to newQuantity)
            )
            val remoteItems = extractCartItemsJson(response, optionalUserId(appContext).orEmpty())
            if (remoteItems.isNotEmpty()) {
                cartItems.clear()
                cartItems.addAll(remoteItems)
                publishState()
            }
        } catch (e: Exception) {
            Log.w("EleganCart", "No se pudo actualizar cantidad en el backend.", e)
        }
    }

    suspend fun removeFromCart(cartItemId: String) = withContext(Dispatchers.IO) {
        cartItems.removeAll { it.id == cartItemId }
        publishState()

        val token = activeBackendToken() ?: return@withContext
        try {
            ApiAutenticacionMovil.service.removeCartItem("Bearer $token", cartItemId)
        } catch (e: Exception) {
            Log.w("EleganCart", "No se pudo eliminar artículo del carrito en el backend.", e)
        }
    }

    suspend fun clearCart(syncRemote: Boolean = true) = withContext(Dispatchers.IO) {
        cartItems.clear()
        publishState()

        if (!syncRemote) return@withContext
        val token = activeBackendToken() ?: return@withContext
        try {
            ApiAutenticacionMovil.service.clearCart("Bearer $token")
        } catch (e: Exception) {
            Log.w("EleganCart", "No se pudo vaciar el carrito en el backend.", e)
        }
    }
}

internal fun extractCartItemsJson(response: Map<String, Any?>, fallbackUserId: String): List<CartItem> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["cart"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: ((response["cart"] as? Map<*, *>)?.get("items") as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val id = firstString(map, "id", "_id", "itemId", "item_id")
        val productId = firstString(map, "productId", "product_id")
        val name = firstString(map, "name", "title", "productName", "product_name")
        if (id.isBlank() && productId.isBlank()) return@mapNotNull null
        val rawPrice = map["price"] ?: map["unitPrice"] ?: map["unit_price"]
        val price = when (rawPrice) {
            is Number -> rawPrice.toDouble()
            is String -> rawPrice.trim().toDoubleOrNull() ?: 0.0
            else -> 0.0
        }
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
            quantity = quantity,
            createdAt = firstString(map, "createdAt", "created_at").ifBlank { null },
            updatedAt = firstString(map, "updatedAt", "updated_at").ifBlank { null }
        )
    }
}

private fun firstString(map: Map<*, *>, vararg keys: String): String {
    for (key in keys) {
        val v = (map[key] as? String)?.trim()
        if (!v.isNullOrBlank()) return v
    }
    return ""
}
