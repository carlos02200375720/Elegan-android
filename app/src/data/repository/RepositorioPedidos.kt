package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.Order
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

internal class RepositorioPedidos(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito
) {
    private val ordersPrefs by lazy {
        appContext.getSharedPreferences("elegan_orders_cache", Context.MODE_PRIVATE)
    }
    private val ordersLoadMutex = Mutex()

    @Volatile
    private var loadedOrdersKey: String? = null
    private val localOrdersList = CopyOnWriteArrayList<Order>()
    private val localOrdersFlow = MutableStateFlow<List<Order>>(emptyList())

    private fun cacheKey(userKey: String = userCacheKey(appContext)): String = "orders_list_$userKey"

    suspend fun ensureLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedOrdersKey == currentKey) return
        ordersLoadMutex.withLock {
            if (loadedOrdersKey == currentKey) return
            val loaded = withContext(Dispatchers.IO) { loadPersistedOrders(currentKey) }
            localOrdersList.clear()
            if (loaded.isNotEmpty()) localOrdersList.addAll(loaded)
            localOrdersFlow.value = localOrdersList.toList()
            loadedOrdersKey = currentKey
        }
    }

    private fun loadPersistedOrders(userKey: String): List<Order> {
        val rawOrders = ordersPrefs.getString(cacheKey(userKey), null) ?: return emptyList()
        return try {
            val array = JSONArray(rawOrders)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        Order(
                            id = obj.optString("id", ""),
                            userId = obj.optString("userId", ""),
                            customerName = obj.optString("customerName", ""),
                            email = obj.optString("email", ""),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            city = obj.optString("city", ""),
                            paymentMethod = obj.optString("paymentMethod", "Tarjeta"),
                            paymentProofUrl = obj.optString("paymentProofUrl", ""),
                            total = obj.optDouble("total", 0.0),
                            itemCount = obj.optLong("itemCount", 1L).coerceAtLeast(1L),
                            status = obj.optString("status", "confirmado"),
                            itemsSummary = obj.optString("itemsSummary", ""),
                            createdAt = obj.optString("createdAt", "").takeIf { it.isNotBlank() }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudieron leer los pedidos locales: ${e.message}")
            emptyList()
        }
    }

    private suspend fun savePersistedOrders(
        snapshot: List<Order>,
        userKey: String = userCacheKey(appContext)
    ) {
        withContext(Dispatchers.IO) {
            try {
                val ordersArray = JSONArray()
                snapshot.forEach { order ->
                    ordersArray.put(
                        JSONObject().apply {
                            put("id", order.id)
                            put("userId", order.userId)
                            put("customerName", order.customerName)
                            put("email", order.email)
                            put("phone", order.phone)
                            put("address", order.address)
                            put("city", order.city)
                            put("paymentMethod", order.paymentMethod)
                            put("paymentProofUrl", order.paymentProofUrl)
                            put("total", order.total)
                            put("itemCount", order.itemCount)
                            put("status", order.status)
                            put("itemsSummary", order.itemsSummary)
                            put("createdAt", order.createdAt.orEmpty())
                        }
                    )
                }
                ordersPrefs.edit().putString(cacheKey(userKey), ordersArray.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudieron guardar los pedidos localmente: ${e.message}")
            }
        }
    }

    fun observeOrders(): Flow<List<Order>> = flow {
        ensureLoaded()
        localOrdersFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

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
        carritoRepo.ensureLoaded()
        ensureLoaded()

        val localId = "order_" + UUID.randomUUID().toString().take(8)
        val persistedProofUrl = if (paymentProofUri.isNotBlank()) {
            persistirArchivoMultimediaLocal(appContext, paymentProofUri, localId, "payment_proof")
        } else ""

        val initialOrder = Order(
            id = localId,
            userId = optionalUserId(appContext).orEmpty(),
            customerName = customerName,
            email = email,
            phone = phone,
            address = address,
            city = city,
            paymentMethod = paymentMethod,
            paymentProofUrl = persistedProofUrl,
            total = total,
            itemCount = itemCount.coerceAtLeast(1L),
            status = "confirmado",
            itemsSummary = itemsSummary,
            createdAt = nowIsoUtc()
        )

        localOrdersList.add(0, initialOrder)
        val snapshot = localOrdersList.toList()
        localOrdersFlow.value = snapshot
        savePersistedOrders(snapshot)
        carritoRepo.clearCart()
        return localId
    }

    fun paymentProofUrls(): List<String> = localOrdersList.map { it.paymentProofUrl }

    fun clearUserData(userKey: String) {
        ordersPrefs.edit().remove(cacheKey(userKey)).commit()
        localOrdersList.clear()
        localOrdersFlow.value = emptyList()
        loadedOrdersKey = null
    }
}
