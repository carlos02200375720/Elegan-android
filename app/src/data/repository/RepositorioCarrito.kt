package com.example.data.repository

import android.content.Context
import com.example.data.model.CartItem
import com.example.data.model.Product
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class RepositorioCarrito(private val appContext: Context) {

    private val cartItems = CopyOnWriteArrayList<CartItem>()
    private val cartFlow = MutableStateFlow<List<CartItem>>(emptyList())

    private fun publishState() {
        cartFlow.value = cartItems.toList()
    }

    fun observeCart(): Flow<List<CartItem>> = cartFlow.asStateFlow()

    fun addToCart(product: Product, selectedSize: String, quantity: Long = 1L, selectedColor: String = "") {
        require(product.id.isNotBlank() && quantity > 0)
        val existingIndex = cartItems.indexOfFirst {
            it.productId == product.id && it.selectedSize == selectedSize && it.selectedColor == selectedColor
        }
        if (existingIndex >= 0) {
            val existing = cartItems[existingIndex]
            cartItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            cartItems.add(
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

    fun updateCartQuantity(cartItemId: String, newQuantity: Long) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId)
            return
        }
        val index = cartItems.indexOfFirst { it.id == cartItemId }
        if (index >= 0) {
            cartItems[index] = cartItems[index].copy(quantity = newQuantity)
            publishState()
        }
    }

    fun removeFromCart(cartItemId: String) {
        cartItems.removeAll { it.id == cartItemId }
        publishState()
    }

    fun clearCart() {
        cartItems.clear()
        publishState()
    }
}
