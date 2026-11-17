package com.example.data.repository

import android.content.Context
import com.example.data.model.Order
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RepositorioPedidos(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito
) {
    private val ordersList = CopyOnWriteArrayList<Order>()
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())

    fun observeOrders(): Flow<List<Order>> = ordersFlow.asStateFlow()

    fun createOrder(
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
