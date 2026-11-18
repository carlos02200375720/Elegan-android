package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.productCategory
import com.example.data.model.productImageUrl

fun resolveCategoryCoverImage(category: CategoryItem, products: List<ProductJson>): String? {
    if (category.imageUrl.isNotBlank()) return category.imageUrl
    val match = products.firstOrNull {
        it.productCategory() == category.id && it.productImageUrl().isNotBlank()
    }
    return match?.productImageUrl()?.takeIf { it.isNotBlank() }
}

fun getCategoryStoryIcon(category: CategoryItem): ImageVector {
    val key = category.iconName.ifBlank { category.id }.trim().lowercase()
    return when (key) {
        "grid_view", "todo", "all" -> Icons.Default.GridView
        "checkroom", "gorra", "gorras", "boinas", "sombrero", "sombreros" -> Icons.Default.Checkroom
        "visibility", "gafa", "gafas", "lentes" -> Icons.Default.Visibility
        "dry_cleaning", "camisa", "camisas", "traje", "trajes", "blazer" -> Icons.Default.DryCleaning
        "straighten", "correa", "correas", "cinturon", "cinturones" -> Icons.Default.Straighten
        "styler", "accessibility", "pantalone", "pantalon", "pantalones" -> Icons.Default.Accessibility
        "ice_skating", "zapato", "zapatos", "calzado", "oxford" -> Icons.Default.IceSkating
        "layers", "interiore", "interior", "ropa_interior" -> Icons.Default.Layers
        "spa", "perfume", "perfumes", "fragancia", "fragancias" -> Icons.Default.Spa
        "watch", "reloj", "relojes" -> Icons.Default.Watch
        "shopping_bag", "bolso", "bolsos", "accesorios" -> Icons.Default.ShoppingBag
        else -> Icons.Default.Category
    }
}
