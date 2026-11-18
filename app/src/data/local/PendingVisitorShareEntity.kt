package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_visitor_shares")
internal data class PendingVisitorShareEntity(
    @PrimaryKey
    val eventId: String,
    val productId: String,
    val visitorId: String,
    val createdAt: Long = System.currentTimeMillis()
)
