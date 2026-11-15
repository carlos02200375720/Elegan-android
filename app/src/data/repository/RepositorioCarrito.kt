package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.CartItem
import com.example.data.model.Product
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal class RepositorioCarrito(private val appContext: Context) {

    private val cartPrefs by lazy {
        appContext.getSharedPreferences("elegan_cart_cache", Context.MODE_PRIVATE)
    }
    private val cartLoadMutex = Mutex()

    @Volatile
    private var loadedCartKey: String? = null
    private val localCartItems = CopyOnWriteArrayList<CartItem>()
    private val localCartFlow = MutableStateFlow<List<CartItem>>(emptyList())

    private fun cacheKey(userKey: String = userCacheKey(appContext)): String = "cart_items_$userKey"

    suspend fun ensureLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedCartKey == currentKey) return
        cartLoadMutex.withLock {
            if (loadedCartKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedCart(currentKey) }
            localCartItems.clear()
            if (loaded.isNotEmpty()) localCartItems.addAll(loaded)
            localCartFlow.value = localCartItems.toList()
            loadedCartKey = currentKey
        }
    }

    private fun loadPersistedCart(userKey: String): List<CartItem> {
        val raw = cartPrefs.getString(cacheKey(userKey), null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        CartItem(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            productId = obj.optString("productId", ""),
                            name = obj.optString("name", ""),
                            price = obj.optDouble("price", 0.0),
                            imageUrl = obj.optString("imageUrl", ""),
                            selectedSize = obj.optString("selectedSize", ""),
                            selectedColor = obj.optString("selectedColor", ""),
                            quantity = obj.optLong("quantity", 1L).coerceAtLeast(1L)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer el carrito local persistido: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedCart(items: List<CartItem>) {
        withContext(Dispatchers.IO) {
            try {
                val array = JSONArray()
                for (item in items) {
                    array.put(
                        JSONObject().apply {
                            put("id", item.id)
                            put("userId", item.userId)
                            put("productId", item.productId)
                            put("name", item.name)
                            put("price", item.price)
                            put("imageUrl", item.imageUrl)
                            put("selectedSize", item.selectedSize)
                            put("selectedColor", item.selectedColor)
                            put("quantity", item.quantity)
                        }
                    )
                }
                cartPrefs.edit().putString(cacheKey(), array.toString()).apply()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar el carrito localmente: ${e.message}")
            }
        }
    }

    private suspend fun publishState() {
        val snapshot = localCartItems.toList()
        localCartFlow.value = snapshot
        savePersistedCart(snapshot)
    }

    fun observeCart(): Flow<List<CartItem>> = flow {
        ensureLoaded()
        localCartFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun addToCart(product: Product, selectedSize: String, quantity: Long = 1L, selectedColor: String = "") {
        require(product.id.isNotBlank() && quantity > 0)
        ensureLoaded()
        val existingIndex = localCartItems.indexOfFirst {
            it.productId == product.id && it.selectedSize == selectedSize && it.selectedColor == selectedColor
        }
        if (existingIndex >= 0) {
            val existing = localCartItems[existingIndex]
            localCartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            localCartItems.add(
                CartItem(
                    id = UUID.randomUUID().toString(),
                    userId = optionalUserId(appContext).orEmpty(),
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    imageUrl = product.imageUrl,
                    selectedSize = selectedSize,
                    selectedColor = selectedColor,
                    quantity = quantity
                )
            )
        }
        publishState()
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        ensureLoaded()
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }
        val index = localCartItems.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            localCartItems[index] = localCartItems[index].copy(quantity = newQuantity)
            publishState()
        }
    }

    suspend fun removeFromCart(cartItemId: String) {
        ensureLoaded()
        localCartItems.removeAll { it.id == cartItemId }
        publishState()
    }

    suspend fun clearCart() {
        ensureLoaded()
        localCartItems.clear()
        publishState()
    }

    fun clearUserData(userKey: String) {
        cartPrefs.edit().remove(cacheKey(userKey)).commit()
        localCartItems.clear()
        localCartFlow.value = emptyList()
        loadedCartKey = null
    }
}
