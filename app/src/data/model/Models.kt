package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.BuildConfig

/**
 * Representación directa de cada producto tal como llega en JSON desde el backend
 * (`GET api/movil/products` -> `{ items: [ ... ] }`), sin conversión a clase intermedia.
 */
typealias ProductJson = Map<String, Any?>

fun ProductJson.productId(): String =
    firstNonBlankString("id", "_id")

fun ProductJson.productName(): String =
    firstNonBlankString("name", "title")

fun ProductJson.productDescription(): String =
    firstNonBlankString("description")

fun ProductJson.productCategory(): String =
    firstNonBlankString("category").ifBlank { "todo" }

fun ProductJson.productPrice(): Double {
    val raw = this["price"] ?: return 0.0
    return when (raw) {
        is Number -> raw.toDouble()
        is String -> raw.trim().toDoubleOrNull() ?: 0.0
        else -> 0.0
    }
}

fun ProductJson.productReviewCount(): Long {
    val raw = this["reviewCount"] ?: this["review_count"] ?: return 0L
    return when (raw) {
        is Number -> raw.toLong().coerceAtLeast(0L)
        is String -> raw.trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        else -> 0L
    }
}

/** Conteo persistente de comentarios enviado por el catálogo móvil del backend. */
fun ProductJson.productCommentCount(): Long {
    val raw = this["commentsCount"] ?: this["comments_count"] ?: return 0L
    return when (raw) {
        is Number -> raw.toLong().coerceAtLeast(0L)
        is String -> raw.trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        else -> 0L
    }
}

fun ProductJson.productImages(): List<String> {
    val rawList = (this["images"] as? List<*>) ?: (this["gallery"] as? List<*>) ?: emptyList<Any?>()
    val directUrl = firstNonBlankString("imageUrl", "image_url", "image", "coverImage")
        .let(::resolveBackendMediaUrl)
    val parsed = rawList.mapNotNull { entry ->
        when (entry) {
            is String -> entry.trim().takeIf { it.isNotBlank() }?.let(::resolveBackendMediaUrl)
            is Map<*, *> -> {
                val url = listOf("url", "imageUrl", "image_url", "name")
                    .firstNotNullOfOrNull { key -> (entry[key] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                url?.let(::resolveBackendMediaUrl)
            }
            else -> null
        }
    }
    return if (directUrl.isNotBlank()) (listOf(directUrl) + parsed).distinct() else parsed.distinct()
}

fun ProductJson.productImageUrl(): String {
    val direct = firstNonBlankString("imageUrl", "image_url", "image", "coverImage")
        .let(::resolveBackendMediaUrl)
    if (direct.isNotBlank()) return direct
    return productImages().firstOrNull { !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
}

fun ProductJson.productVideoUrl(): String =
    firstNonBlankString("videoUrl", "video_url", "video").let(::resolveBackendMediaUrl)

fun ProductJson.productHasVideo(): Boolean =
    readBoolean("isVideo", "is_video", defaultValue = false) || productVideoUrl().isNotBlank()

fun ProductJson.productInStock(): Boolean =
    readBoolean("inStock", "in_stock", defaultValue = true)

fun ProductJson.productMediaFormat(): String =
    firstNonBlankString("mediaFormat", "media_format").ifBlank { "1:1" }

fun ProductJson.productSizes(): List<String> =
    readStringArray("sizes")

fun ProductJson.productColors(): List<String> =
    readStringArray("colors")

private fun ProductJson.firstNonBlankString(vararg keys: String): String {
    for (key in keys) {
        val value = (this[key] as? String)?.trim()
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

private fun ProductJson.readBoolean(vararg keys: String, defaultValue: Boolean): Boolean {
    for (key in keys) {
        if (!containsKey(key) || this[key] == null) continue
        return when (val raw = this[key]) {
            is Boolean -> raw
            is Number -> raw.toInt() != 0
            is String -> raw.equals("true", ignoreCase = true) || raw == "1"
            else -> defaultValue
        }
    }
    return defaultValue
}

private fun ProductJson.readStringArray(key: String): List<String> {
    val list = this[key] as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        when (item) {
            is String -> item.trim().takeIf { it.isNotBlank() }
            is Map<*, *> -> (item["name"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}

internal fun resolveBackendMediaUrl(rawUrl: String): String {
    val clean = rawUrl.trim()
    if (clean.isBlank()) return ""
    if (clean.startsWith("http://", ignoreCase = true) || clean.startsWith("https://", ignoreCase = true)) {
        return clean
    }
    if (clean.startsWith("//")) return "https:$clean"
    val cleanBase = BuildConfig.ELEGAN_API_BASE_URL.trimEnd('/')
    return if (clean.startsWith("/")) "$cleanBase$clean" else "$cleanBase/$clean"
}

typealias AnnouncementJson = Map<String, Any?>

fun AnnouncementJson.announcementId(): String =
    firstNonBlankString("id", "_id")

fun AnnouncementJson.announcementTitle(): String =
    firstNonBlankString("title", "name", "headline")

fun AnnouncementJson.announcementMessage(): String =
    firstNonBlankString("message", "description", "body", "subtitle", "content")

fun AnnouncementJson.announcementImageUrl(): String =
    firstNonBlankString("imageUrl", "image_url", "image", "bannerUrl", "banner_url")
        .let(::resolveBackendMediaUrl)

fun AnnouncementJson.announcementCategory(): String =
    firstNonBlankString("category", "targetCategory", "target_category")

fun AnnouncementJson.announcementBadge(): String =
    firstNonBlankString("badge", "tag", "label")

fun AnnouncementJson.announcementActive(): Boolean =
    readBoolean("active", "isActive", "is_active", "published", defaultValue = true)

@Immutable
data class PayPalOrderSession(
    val paypalOrderId: String,
    val approveUrl: String,
    val status: String = "CREATED"
)

@Immutable
data class PublicationStats(
    val id: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
    val savesCount: Long = 0L,
    val lastCommentId: String = ""
)

@Immutable
data class PublicationComment(
    val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val displayName: String = "",
    val text: String = "",
    val createdAt: String? = null
)

@Immutable
data class CartItem(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val selectedSize: String = "",
    val selectedColor: String = "",
    val quantity: Long = 1L,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Immutable
data class Order(
    val id: String = "",
    val userId: String = "",
    val customerName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val paymentMethod: String = "Tarjeta",
    val paymentProofUrl: String = "",
    val total: Double = 0.0,
    val itemCount: Long = 1L,
    val status: String = "confirmado",
    val itemsSummary: String = "",
    val createdAt: String? = null
)

@Immutable
data class BankAccountInfo(
    val id: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val accountType: String = "",
    val holderName: String = "",
    val holderDocument: String = ""
)

fun defaultBankAccounts(): List<BankAccountInfo> = listOf(
    BankAccountInfo(
        id = "bank_1",
        bankName = "Banco Popular",
        accountNumber = "804-291847-3",
        accountType = "Cuenta Corriente",
        holderName = "Carlos Alberto García",
        holderDocument = "001-1849203-7"
    ),
    BankAccountInfo(
        id = "bank_2",
        bankName = "Banreservas",
        accountNumber = "960-448201-5",
        accountType = "Cuenta de Ahorros",
        holderName = "Elegan Sartorial S.R.L.",
        holderDocument = "1-32-84910-4"
    ),
    BankAccountInfo(
        id = "bank_3",
        bankName = "Banco BHD",
        accountNumber = "215-009384-1",
        accountType = "Cuenta Corriente",
        holderName = "Carlos Alberto García",
        holderDocument = "001-1849203-7"
    )
)

@Immutable
data class SupportMessage(
    val id: String = "",
    val userId: String = "",
    val sender: String = "user", // "user" or "support"
    val senderName: String = "",
    val text: String = "",
    val mediaUrl: String = "",
    val mediaType: String = "", // "image", "video", or ""
    val createdAt: String? = null
)

@Immutable
data class UserProfile(
    val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val city: String = "",
    val updatedAt: String? = null
)

enum class StoreCategory(val id: String, val title: String, val iconName: String) {
    TODO("todo", "Todo", "grid_view"),
    GORRA("gorra", "Gorras & Boinas", "checkroom"),
    GAFA("gafa", "Gafas de Sol", "visibility"),
    CAMISA("camisa", "Camisas & Trajes", "dry_cleaning"),
    CORREA("correa", "Correas de Cuero", "straighten"),
    PANTALONE("pantalone", "Pantalones de Vestir", "styler"),
    ZAPATO("zapato", "Zapatos Oxford", "ice_skating"),
    INTERIORE("interiore", "Ropa Interior", "layers"),
    PERFUME("perfume", "Perfumes & Fragancias", "spa")
}
