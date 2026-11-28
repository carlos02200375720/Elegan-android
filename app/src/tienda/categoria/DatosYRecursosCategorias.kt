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
import com.example.data.model.Product
import com.example.data.model.productCategory
import com.example.data.model.productImageUrl

fun resolveCategoryCoverImages(
    categories: List<CategoryItem>,
    products: List<Product>
): Map<String, String?> {
    if (categories.isEmpty()) return emptyMap()
    val result = HashMap<String, String?>(categories.size * 2)
    val missingCategoryIds = HashSet<String>(categories.size)
    for (category in categories) {
        if (category.imageUrl.isNotBlank()) {
            result[category.id] = category.imageUrl
        } else {
            result[category.id] = null
            missingCategoryIds.add(category.id)
        }
    }
    if (missingCategoryIds.isEmpty() || products.isEmpty()) return result

    for (product in products) {
        val catId = product.productCategory()
        if (catId in missingCategoryIds) {
            val imageUrl = product.productImageUrl()
            if (imageUrl.isNotBlank()) {
                result[catId] = imageUrl
                missingCategoryIds.remove(catId)
                if (missingCategoryIds.isEmpty()) break
            }
        }
    }
    return result
}

fun resolveCategoryCoverImage(category: CategoryItem, products: List<Product>): String? {
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
