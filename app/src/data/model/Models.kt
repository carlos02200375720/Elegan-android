package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.BuildConfig
import com.example.R

object LocalBoutiqueMedia {
    fun drawableUri(@DrawableRes resId: Int): String =
        "android.resource://${BuildConfig.APPLICATION_ID}/$resId"

    val camisaCover: String get() = drawableUri(R.drawable.img_cat_camisa)
    val gorraCover: String get() = drawableUri(R.drawable.img_cat_gorra)
    val gafaCover: String get() = drawableUri(R.drawable.img_cat_gafa)
    val zapatoCover: String get() = drawableUri(R.drawable.img_cat_zapato)
    val perfumeCover: String get() = drawableUri(R.drawable.img_cat_perfume)

    fun fallbackImageForCategory(category: String): String = when (category.lowercase().trim()) {
        "gorra" -> gorraCover
        "gafa" -> gafaCover
        "camisa", "pantalone", "interiore" -> camisaCover
        "correa", "zapato" -> zapatoCover
        "perfume" -> perfumeCover
        else -> camisaCover
    }

    fun gallerySupplementsForCategory(category: String): List<String> = when (category.lowercase().trim()) {
        "gorra" -> listOf(gorraCover, camisaCover, gafaCover)
        "gafa" -> listOf(gafaCover, camisaCover, zapatoCover)
        "camisa", "pantalone", "interiore" -> listOf(camisaCover, zapatoCover, perfumeCover)
        "correa", "zapato" -> listOf(zapatoCover, camisaCover, gorraCover)
        "perfume" -> listOf(perfumeCover, camisaCover, gafaCover)
        else -> listOf(camisaCover, zapatoCover, perfumeCover)
    }
}

@Immutable
data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "todo", // gorra, gafa, camisa, correa, pantalone, zapato, interiore, perfume
    val price: Double = 0.0,
    val imageUrl: String = "",
    val images: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val rating: Double = 5.0,
    val reviewCount: Long = 0L,
    val sizes: List<String> = emptyList(),
    val inStock: Boolean = true,
    val isFeatured: Boolean = false,
    val mediaFormat: String = "1:1", // "1:1", "3:4", "4:3", "9:16", "16:9"
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    val createdAt: String? = null
) {
    val hasVideo: Boolean
        get() = isVideo || videoUrl.isNotBlank()
}

val Product.aspectRatioValue: Float
    get() = when (mediaFormat.trim()) {
        "3:4" -> 3f / 4f
        "4:3" -> 4f / 3f
        "9:16" -> 9f / 16f
        "16:9" -> 16f / 9f
        else -> 1f // default 1:1
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

@Immutable
data class Announcement(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "banner", // "banner", "historia_9_16", "descuento", "coleccion"
    val imageUrl: String = "",
    val discountCode: String = "",
    val discountPercent: Double = 0.0,
    val ctaText: String = "Ver Más",
    val targetCategory: String = "todo",
    val isActive: Boolean = true,
    val videoUrl: String = "",
    val isVideo: Boolean = false,
    val createdAt: String? = null
) {
    val hasVideo: Boolean
        get() = isVideo || videoUrl.isNotBlank()
}

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
