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
import com.example.data.remote.ApiAutenticacionMovil
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

suspend fun cargarCategoriasPublicadas(): List<CategoriaItem> {
    val response = ApiAutenticacionMovil.service.categories()
    val remoteItems = (response["items"] as? List<*>)?.mapNotNull { raw ->
        val item = raw as? Map<*, *> ?: return@mapNotNull null
        val id = item["id"]?.toString()?.trim().orEmpty()
        val name = item["name"]?.toString()?.trim().orEmpty()
        if (id.isBlank() || name.isBlank()) return@mapNotNull null
        val known = storeCategoriesList.firstOrNull { it.id.equals(id, true) }
        CategoriaItem(
            id = id,
            title = name,
            subtitle = item["description"]?.toString()?.ifBlank { "Explora esta categoría" } ?: "Explora esta categoría",
            icon = known?.icon ?: Icons.Default.GridView,
            imageUrl = item["imageUrl"]?.toString()?.takeIf { it.isNotBlank() }
        )
    }.orEmpty()
    return listOf(CategoriaItem(
        id = "todo",
        title = "Todo el Catálogo",
        subtitle = "Explora todas las piezas y colecciones",
        icon = Icons.Default.GridView
    )) + remoteItems.filterNot { it.id.equals("todo", true) }
}

fun resolveCategoryCoverImage(category: CategoriaItem, products: List<ProductJson>): String? {
    category.imageUrl?.takeIf { it.isNotBlank() }?.let { return it }
    val match = if (category.id.equals("todo", true)) {
        products.firstOrNull { it.productImageUrl().isNotBlank() }
    } else {
        products.firstOrNull { it.productCategory().equals(category.id, true) && it.productImageUrl().isNotBlank() }
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
fun getCategoryStoryIcon(category: CategoriaItem): ImageVector = category.icon
