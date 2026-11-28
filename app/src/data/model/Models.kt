package com.example.data.model

import androidx.compose.runtime.Immutable
import com.example.BuildConfig

/** Lee únicamente importes enteros; los valores fraccionarios se rechazan. */
fun parseWholeMoney(raw: Any?): Double? {
    val integer = when (raw) {
        is Byte, is Short, is Int, is Long -> (raw as Number).toLong()
        is Number -> {
            val number = raw.toDouble()
            if (!number.isFinite() || number < 0.0 || number % 1.0 != 0.0 || number >= Long.MAX_VALUE.toDouble()) return null
            number.toLong()
        }
        is String -> raw.trim().takeIf { it.matches(Regex("^\\d+$")) }?.toLongOrNull()
        else -> null
    } ?: return null
    if (integer < 0L) return null
    return integer.toDouble()
}

private val uuidCategoryPattern =
    Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
private val objectIdCategoryPattern = Regex("^[0-9a-fA-F]{24}$")

private fun looksLikeOpaqueCategoryId(value: String): Boolean =
    uuidCategoryPattern.matches(value) || objectIdCategoryPattern.matches(value)

@Immutable
data class ProductShippingMethod(
    val id: String,
    val name: String,
    val price: Double,
    val deliveryTime: String,
    val origin: String
)

/**
 * Modelo tipado e inmutable de producto para Compose y dominio.
 * Se construye una sola vez en la capa de datos mediante [toProduct] a partir del JSON del backend.
 */
@Immutable
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val videoUrl: String = "",
    val galleryVideoUrls: List<String> = emptyList(),
    val hasVideo: Boolean = false,
    val inStock: Boolean = true,
    val allowCashOnDelivery: Boolean = true,
    val mediaFormat: String = "1:1",
    val sizes: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val shippingMethods: List<ProductShippingMethod> = emptyList(),
    val selectedShippingId: String = "",
    val reviewCount: Long = 0L,
    val likesCount: Long? = null,
    val commentsCount: Long? = null,
    val savesCount: Long? = null,
    val sharesCount: Long? = null
)

fun Product.productId(): String = id

fun Product.productName(): String = name

fun Product.productDescription(): String = description

fun Product.productCategory(): String = categoryId

fun Product.productCategoryDisplayName(categories: List<CategoryItem> = emptyList()): String {
    if (categoryName.isNotBlank()) return categoryName
    if (categoryId.isBlank()) return ""
    val matchedTitle = categories.firstOrNull { it.id == categoryId }?.title?.trim()
    if (!matchedTitle.isNullOrBlank()) return matchedTitle
    return if (looksLikeOpaqueCategoryId(categoryId)) "" else categoryId
}

fun Product.productCategoryDisplayName(categoryTitlesById: Map<String, String>): String {
    if (categoryName.isNotBlank()) return categoryName
    if (categoryId.isBlank()) return ""
    val matchedTitle = categoryTitlesById[categoryId]?.trim()
    if (!matchedTitle.isNullOrBlank()) return matchedTitle
    return if (looksLikeOpaqueCategoryId(categoryId)) "" else categoryId
}

fun Product.productShippingMethods(): List<ProductShippingMethod> = shippingMethods

fun Product.productPrice(): Double = price

fun Product.productReviewCount(): Long = reviewCount

/** Conteo persistente de comentarios enviado por el catálogo móvil del backend. */
fun Product.productCommentCount(): Long = commentsCount ?: 0L

/** Conteo global de likes enviado por el catálogo móvil del backend. */
fun Product.productLikesCount(): Long = likesCount ?: 0L

/** Conteo global de guardados enviado por el catálogo móvil del backend. */
fun Product.productSavesCount(): Long = savesCount ?: 0L

/** Conteo global de compartidos enviado por el catálogo móvil del backend. */
fun Product.productSharesCount(): Long = sharesCount ?: 0L

fun Product.productImages(): List<String> = images

fun Product.productGalleryVideoUrls(): List<String> = galleryVideoUrls

fun Product.productImageUrl(): String = imageUrl

fun Product.productVideoUrl(): String = videoUrl

fun Product.productHasVideo(): Boolean =
    hasVideo || videoUrl.isNotBlank() || galleryVideoUrls.isNotEmpty()

fun Product.productInStock(): Boolean = inStock

fun Product.productMediaFormat(): String = mediaFormat.ifBlank { "1:1" }

fun Product.productSizes(): List<String> = sizes

fun Product.productColors(): List<String> = colors

private val videoFileExtensions = setOf("mp4", "mov", "webm", "m4v", "mkv", "3gp", "m3u8")

fun esUrlDeVideo(url: String): Boolean {
    val cleanPath = url.trim().substringBefore('?').substringBefore('#')
    val ext = cleanPath.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return ext in videoFileExtensions
}

private fun looksLikeMediaPathOrUrl(value: String): Boolean =
    value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("//") ||
        value.contains('/') ||
        value.substringAfterLast('.', missingDelimiterValue = "").length in 2..5

internal fun resolveBackendMediaUrl(rawUrl: String): String {
    val clean = rawUrl.trim()
    if (clean.isBlank()) return ""
    val encoded = if (clean.contains(' ')) clean.replace(" ", "%20") else clean
    if (encoded.startsWith("https://", ignoreCase = true)) {
        return encoded
    }
    if (encoded.startsWith("//")) {
        val candidate = "https:$encoded"
        return if (candidate.length > "https://".length) candidate else ""
    }
    // Bloquea esquemas en texto plano o no permitidos (http://, javascript:, data:, file:, etc.)
    if (encoded.contains("://") || encoded.substringBefore('/').contains(':')) {
        return ""
    }
    val cleanBase = BuildConfig.ELEGAN_API_BASE_URL.trimEnd('/')
    if (!cleanBase.startsWith("https://", ignoreCase = true)) return ""
    return if (encoded.startsWith("/")) "$cleanBase$encoded" else "$cleanBase/$encoded"
}

/** Convierte un objeto JSON de producto del backend en un modelo [Product] inmutable. */
fun Map<*, *>.toProduct(): Product {
    val rawGallery = (this["images"] as? List<*>) ?: (this["gallery"] as? List<*>) ?: emptyList<Any?>()
    val directImageUrl = firstNonBlankString("imageUrl", "image_url", "image", "coverImage")
        .let(::resolveBackendMediaUrl)
    val parsedImages = rawGallery.mapNotNull { entry ->
        val rawUrl = when (entry) {
            is String -> entry.trim().takeIf { it.isNotBlank() }
            is Map<*, *> -> {
                val primary = listOf("url", "imageUrl", "image_url", "mediaUrl", "media_url", "src", "path")
                    .firstNotNullOfOrNull { key -> (entry[key] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                primary ?: (entry["name"] as? String)?.trim()?.takeIf { it.isNotBlank() && looksLikeMediaPathOrUrl(it) }
            }
            else -> null
        }
        rawUrl?.let(::resolveBackendMediaUrl)?.takeIf { it.isNotBlank() }
    }
    val resolvedImages = if (directImageUrl.isNotBlank()) {
        (listOf(directImageUrl) + parsedImages).distinct()
    } else {
        parsedImages.distinct()
    }

    val resolvedVideoUrl = firstNonBlankString("videoUrl", "video_url", "video")
        .let(::resolveBackendMediaUrl)
    val resolvedGalleryVideos = buildList {
        if (resolvedVideoUrl.isNotBlank()) add(resolvedVideoUrl)
        rawGallery.forEach { entry ->
            when (entry) {
                is String -> {
                    val resolved = resolveBackendMediaUrl(entry)
                    if (resolved.isNotBlank() && esUrlDeVideo(resolved)) add(resolved)
                }
                is Map<*, *> -> {
                    val typeHint = listOf("type", "mediaType", "media_type", "mimeType", "mime_type", "kind")
                        .firstNotNullOfOrNull { key -> (entry[key] as? String)?.trim()?.lowercase() }
                        .orEmpty()
                    val rawUrl = listOf("videoUrl", "video_url", "url", "mediaUrl", "media_url", "src", "path")
                        .firstNotNullOfOrNull { key -> (entry[key] as? String)?.trim()?.takeIf { it.isNotBlank() } }
                        .orEmpty()
                    val resolved = resolveBackendMediaUrl(rawUrl)
                    if (resolved.isNotBlank() && (typeHint.contains("video") || esUrlDeVideo(resolved))) {
                        add(resolved)
                    }
                }
            }
        }
    }.distinct()

    val coverImage = if (directImageUrl.isNotBlank() && !esUrlDeVideo(directImageUrl)) {
        directImageUrl
    } else {
        resolvedImages.firstOrNull { !esUrlDeVideo(it) }.orEmpty()
    }

    val shippingMethods = (this["shippingMethods"] as? List<*>)?.mapNotNull { entry ->
        val method = entry as? Map<*, *> ?: return@mapNotNull null
        val id = method["id"]?.toString()?.trim().orEmpty()
        val name = method["name"]?.toString()?.trim().orEmpty()
        val price = parseWholeMoney(method["price"]) ?: return@mapNotNull null
        if (id.isBlank() || name.isBlank()) return@mapNotNull null
        ProductShippingMethod(
            id = id,
            name = name,
            price = price,
            deliveryTime = method["deliveryTime"]?.toString().orEmpty(),
            origin = method["origin"]?.toString().orEmpty()
        )
    } ?: emptyList()

    val isExplicitVideo = readBoolean("isVideo", "is_video", defaultValue = false)

    return Product(
        id = firstNonBlankString("id", "_id"),
        name = firstNonBlankString("name", "title"),
        description = firstNonBlankString("description"),
        categoryId = firstNonBlankString("category", "categoryId", "category_id"),
        categoryName = firstNonBlankString(
            "categoryName",
            "category_name",
            "categoryTitle",
            "category_title",
            "categoryLabel",
            "category_label"
        ),
        price = parseWholeMoney(this["price"]) ?: 0.0,
        imageUrl = coverImage,
        images = resolvedImages,
        videoUrl = resolvedVideoUrl,
        galleryVideoUrls = resolvedGalleryVideos,
        hasVideo = isExplicitVideo || resolvedVideoUrl.isNotBlank() || resolvedGalleryVideos.isNotEmpty(),
        inStock = readBoolean("inStock", "in_stock", defaultValue = true),
        allowCashOnDelivery = readBoolean("allowCashOnDelivery", "allow_cash_on_delivery", defaultValue = true),
        mediaFormat = firstNonBlankString("mediaFormat", "media_format").ifBlank { "1:1" },
        sizes = readStringArray("sizes"),
        colors = readStringArray("colors"),
        shippingMethods = shippingMethods,
        selectedShippingId = firstNonBlankString("selectedShippingId", "shippingMethodId"),
        reviewCount = readOptionalLongMetric("reviewCount", "review_count") ?: 0L,
        likesCount = readOptionalLongMetric("likesCount", "likes_count"),
        commentsCount = readOptionalLongMetric("commentsCount", "comments_count"),
        savesCount = readOptionalLongMetric("savesCount", "saves_count"),
        sharesCount = readOptionalLongMetric("sharesCount", "shares_count")
    )
}

@Immutable
sealed interface AnnouncementDestination {
    @Immutable
    data class Category(val categoryId: String) : AnnouncementDestination

    @Immutable
    data class Product(val productId: String) : AnnouncementDestination

    @Immutable
    data object None : AnnouncementDestination
}

/**
 * Modelo tipado e inmutable de anuncio promocional de inicio.
 */
@Immutable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val imageUrl: String = "",
    val badge: String = "",
    val destinationType: String = "",
    val destinationId: String = "",
    val destination: AnnouncementDestination = AnnouncementDestination.None,
    val category: String = "",
    val active: Boolean = true
)

fun Announcement.announcementId(): String = id

fun Announcement.announcementTitle(): String = title

fun Announcement.announcementMessage(): String = message

fun Announcement.announcementImageUrl(): String = imageUrl

fun Announcement.announcementDestinationType(): String = destinationType

fun Announcement.announcementDestinationId(): String = destinationId

fun Announcement.announcementDestination(): AnnouncementDestination = destination

fun Announcement.announcementCategory(): String = when (val dest = destination) {
    is AnnouncementDestination.Category -> dest.categoryId
    else -> category
}

fun Announcement.announcementBadge(): String = badge

fun Announcement.announcementActive(): Boolean = active

/** Convierte un objeto JSON de anuncio del backend en un modelo [Announcement] inmutable. */
fun Map<*, *>.toAnnouncement(): Announcement {
    val destType = firstNonBlankString("destinationType", "destination_type").lowercase()
    val destId = firstNonBlankString("destinationId", "destination_id")
    val legacyProductId = firstNonBlankString(
        "productId",
        "product_id",
        "targetProductId",
        "target_product_id"
    )
    val legacyCategory = firstNonBlankString("category", "targetCategory", "target_category")

    val resolvedDestination = when {
        destId.isNotBlank() && (destType == "category" || destType == "categoria") ->
            AnnouncementDestination.Category(destId)
        destId.isNotBlank() && (destType == "product" || destType == "producto") ->
            AnnouncementDestination.Product(destId)
        legacyProductId.isNotBlank() ->
            AnnouncementDestination.Product(legacyProductId)
        legacyCategory.isNotBlank() ->
            AnnouncementDestination.Category(legacyCategory)
        else -> AnnouncementDestination.None
    }

    val resolvedCategory = when (resolvedDestination) {
        is AnnouncementDestination.Category -> resolvedDestination.categoryId
        else -> legacyCategory
    }

    return Announcement(
        id = firstNonBlankString("id", "_id"),
        title = firstNonBlankString("title", "name", "headline"),
        message = firstNonBlankString("message", "description", "body", "subtitle", "content"),
        imageUrl = firstNonBlankString("imageUrl", "image_url", "image", "bannerUrl", "banner_url")
            .let(::resolveBackendMediaUrl),
        badge = firstNonBlankString("badge", "tag", "label"),
        destinationType = destType,
        destinationId = destId,
        destination = resolvedDestination,
        category = resolvedCategory,
        active = readBoolean("active", "isActive", "is_active", "published", defaultValue = true)
    )
}

@Immutable
data class CashOnDeliveryConfig(
    val enabled: Boolean = false,
    val message: String = ""
)

fun Map<*, *>.toCashOnDeliveryConfig(): CashOnDeliveryConfig = CashOnDeliveryConfig(
    enabled = readBoolean("enabled", defaultValue = false),
    message = firstNonBlankString("message")
)

private fun Map<*, *>.firstNonBlankString(vararg keys: String): String {
    for (key in keys) {
        val value = (this[key] as? String)?.trim()
        if (!value.isNullOrBlank()) return value
    }
    return ""
}

private fun Map<*, *>.readBoolean(vararg keys: String, defaultValue: Boolean): Boolean {
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

private fun Map<*, *>.readOptionalLongMetric(vararg keys: String): Long? {
    for (key in keys) {
        if (!containsKey(key)) continue
        val raw = this[key] ?: return 0L
        return when (raw) {
            is Number -> raw.toLong().coerceAtLeast(0L)
            is String -> raw.trim().toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            else -> 0L
        }
    }
    return null
}

private fun Map<*, *>.readStringArray(key: String): List<String> {
    val list = this[key] as? List<*> ?: return emptyList()
    return list.mapNotNull { item ->
        when (item) {
            is String -> item.trim().takeIf { it.isNotBlank() }
            is Map<*, *> -> (item["name"] as? String)?.trim()?.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}

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
    val shippingMethodId: String = "",
    val shippingMethodName: String = "",
    val shippingPrice: Double = 0.0,
    val shippingDeliveryTime: String = "",
    val shippingOrigin: String = "",
    val allowCashOnDelivery: Boolean = true,
    val quantity: Long = 1L,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Immutable
data class OrderItem(
    val productId: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val quantity: Long = 1L,
    val selectedSize: String = "",
    val selectedColor: String = "",
    val shippingMethodName: String = "",
    val shippingPrice: Double = 0.0,
    val shippingDeliveryTime: String = "",
    val shippingOrigin: String = ""
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
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val shippingCost: Double = 0.0,
    val shippingMethodName: String = "",
    val shippingDeliveryTime: String = "",
    val shippingOrigin: String = "",
    val total: Double = 0.0,
    val itemCount: Long = 1L,
    val status: String = "confirmado",
    val itemsSummary: String = "",
    val items: List<OrderItem> = emptyList(),
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

@Immutable
data class CategoryItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val imageUrl: String = "",
    val iconName: String = "",
    val sortOrder: Int = Int.MAX_VALUE
)

@Immutable
data class LegalDocument(
    val title: String,
    val subtitle: String,
    val content: String
)
