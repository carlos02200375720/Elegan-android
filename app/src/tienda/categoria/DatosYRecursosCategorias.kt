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
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.model.productCategory
import com.example.data.model.productImageUrl

data class CategoriaItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val imageUrl: String? = null,
    val icon: ImageVector = Icons.Default.GridView
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
            icon = Icons.Default.GridView,
            imageUrl = item["imageUrl"]?.toString()?.takeIf { it.isNotBlank() }
        )
    }.orEmpty()
    return remoteItems
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
