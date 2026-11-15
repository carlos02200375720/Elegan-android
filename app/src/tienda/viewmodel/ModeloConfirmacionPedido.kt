package com.example.ui.viewmodel

import com.example.data.model.CartItem

data class OrderConfirmationDetails(
    val orderId: String,
    val items: List<CartItem>,
    val total: Double,
    val paymentMethod: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val city: String
)
