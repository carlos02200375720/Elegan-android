package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IceSkating
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.ProductJson
import com.example.data.model.StoreCategory
import com.example.data.model.productCategory
import com.example.data.model.productImageUrl

data class CategoriaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val imageUrl: String? = null
)

val storeCategoriesList = listOf(
    CategoriaItem(
        id = "todo",
        title = "Todo el Catálogo",
        subtitle = "Explora todas las piezas y colecciones",
        icon = Icons.Default.GridView
    ),
    CategoriaItem(
        id = "gorra",
        title = "Gorras & Boinas",
        subtitle = "Boinas Peaky, newsboys y sombreros tweed",
        icon = Icons.Default.Checkroom
    ),
    CategoriaItem(
        id = "gafa",
        title = "Gafas",
        subtitle = "Gafas de sol polarizadas y monturas vintage",
        icon = Icons.Default.Visibility
    ),
    CategoriaItem(
        id = "camisa",
        title = "Camisas & Trajes",
        subtitle = "Camisas algodón egipcio, chalecos y blazers",
        icon = Icons.Default.DryCleaning
    ),
    CategoriaItem(
        id = "correa",
        title = "Correas",
        subtitle = "Cinturones de cuero vacuno y hebillas artesanales",
        icon = Icons.Default.Straighten
    ),
    CategoriaItem(
        id = "pantalone",
        title = "Pantalones",
        subtitle = "Pantalones de vestir sartoriales y chinos",
        icon = Icons.Default.Accessibility
    ),
    CategoriaItem(
        id = "zapato",
        title = "Zapatos",
        subtitle = "Oxfords cap-toe, brogues y mocasines",
        icon = Icons.Default.IceSkating
    ),
    CategoriaItem(
        id = "interiore",
        title = "Ropa Interior",
        subtitle = "Bóxers micro-modal y camisetas confort",
        icon = Icons.Default.Layers
    ),
    CategoriaItem(
        id = "perfume",
        title = "Perfumes",
        subtitle = "Fragancias de autor y extractos exclusivos",
        icon = Icons.Default.Spa
    )
)

fun resolveCategoryCoverImage(category: StoreCategory, products: List<ProductJson>): String? {
    val match = if (category == StoreCategory.TODO) {
        products.firstOrNull { it.productImageUrl().isNotBlank() }
    } else {
        products.firstOrNull {
            it.productCategory().equals(category.id, ignoreCase = true) && it.productImageUrl().isNotBlank()
        }
    }
    return match?.productImageUrl()?.takeIf { it.isNotBlank() }
}

fun getCategoryStoryIcon(category: StoreCategory): ImageVector = when (category) {
    StoreCategory.TODO -> Icons.Default.GridView
    StoreCategory.GORRA -> Icons.Default.Checkroom
    StoreCategory.GAFA -> Icons.Default.Visibility
    StoreCategory.CAMISA -> Icons.Default.DryCleaning
    StoreCategory.CORREA -> Icons.Default.Straighten
    StoreCategory.PANTALONE -> Icons.Default.Accessibility
    StoreCategory.ZAPATO -> Icons.Default.IceSkating
    StoreCategory.INTERIORE -> Icons.Default.Layers
    StoreCategory.PERFUME -> Icons.Default.Spa
}
