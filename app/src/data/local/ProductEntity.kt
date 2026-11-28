package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Product
import com.example.data.model.ProductShippingMethod
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(
    tableName = "cached_products",
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["inStock"]),
        Index(value = ["sortOrder"])
    ]
)
internal data class ProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String,
    val categoryId: String,
    val categoryName: String,
    val price: Double,
    val imageUrl: String,
    val videoUrl: String,
    val hasVideo: Boolean,
    val inStock: Boolean,
    val allowCashOnDelivery: Boolean,
    val mediaFormat: String,
    val selectedShippingId: String,
    val reviewCount: Long,
    val likesCount: Long?,
    val commentsCount: Long?,
    val savesCount: Long?,
    val sharesCount: Long?,
    val imagesJson: String,
    val galleryVideoUrlsJson: String,
    val sizesJson: String,
    val colorsJson: String,
    val shippingMethodsJson: String,
    val sortOrder: Int,
    val syncedAtMs: Long
)

@Serializable
private data class SerializedShippingMethod(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val deliveryTime: String = "",
    val origin: String = ""
)

private val productRoomJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

private val stringListSerializer = ListSerializer(String.serializer())
private val shippingListSerializer = ListSerializer(SerializedShippingMethod.serializer())

internal fun Product.toEntity(
    sortOrder: Int,
    syncedAtMs: Long = System.currentTimeMillis()
): ProductEntity = ProductEntity(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    categoryName = categoryName,
    price = price,
    imageUrl = imageUrl,
    videoUrl = videoUrl,
    hasVideo = hasVideo,
    inStock = inStock,
    allowCashOnDelivery = allowCashOnDelivery,
    mediaFormat = mediaFormat,
    selectedShippingId = selectedShippingId,
    reviewCount = reviewCount,
    likesCount = likesCount,
    commentsCount = commentsCount,
    savesCount = savesCount,
    sharesCount = sharesCount,
    imagesJson = productRoomJson.encodeToString(stringListSerializer, images),
    galleryVideoUrlsJson = productRoomJson.encodeToString(stringListSerializer, galleryVideoUrls),
    sizesJson = productRoomJson.encodeToString(stringListSerializer, sizes),
    colorsJson = productRoomJson.encodeToString(stringListSerializer, colors),
    shippingMethodsJson = productRoomJson.encodeToString(
        shippingListSerializer,
        shippingMethods.map {
            SerializedShippingMethod(
                id = it.id,
                name = it.name,
                price = it.price,
                deliveryTime = it.deliveryTime,
                origin = it.origin
            )
        }
    ),
    sortOrder = sortOrder,
    syncedAtMs = syncedAtMs
)

internal fun ProductEntity.toDomain(): Product = Product(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    categoryName = categoryName,
    price = price,
    imageUrl = imageUrl,
    images = decodeStringList(imagesJson),
    videoUrl = videoUrl,
    galleryVideoUrls = decodeStringList(galleryVideoUrlsJson),
    hasVideo = hasVideo,
    inStock = inStock,
    allowCashOnDelivery = allowCashOnDelivery,
    mediaFormat = mediaFormat,
    sizes = decodeStringList(sizesJson),
    colors = decodeStringList(colorsJson),
    shippingMethods = decodeShippingMethods(shippingMethodsJson),
    selectedShippingId = selectedShippingId,
    reviewCount = reviewCount,
    likesCount = likesCount,
    commentsCount = commentsCount,
    savesCount = savesCount,
    sharesCount = sharesCount
)

private fun decodeStringList(raw: String): List<String> {
    if (raw.isBlank() || raw == "[]") return emptyList()
    return runCatching {
        productRoomJson.decodeFromString(stringListSerializer, raw)
    }.getOrDefault(emptyList())
}

private fun decodeShippingMethods(raw: String): List<ProductShippingMethod> {
    if (raw.isBlank() || raw == "[]") return emptyList()
    return runCatching {
        productRoomJson.decodeFromString(shippingListSerializer, raw).map {
            ProductShippingMethod(
                id = it.id,
                name = it.name,
                price = it.price,
                deliveryTime = it.deliveryTime,
                origin = it.origin
            )
        }
    }.getOrDefault(emptyList())
}
