package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.model.Product

@Immutable
sealed interface GalleryMediaItem {
    @Immutable
    data class Video(val videoUrl: String, val posterUrl: String) : GalleryMediaItem

    @Immutable
    data class Image(val imageUrl: String) : GalleryMediaItem
}

/**
 * Retorna la lista unificada de elementos multimedia (video y fotos) obtenidos del backend.
 */
fun getProductMediaItems(product: Product): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val basePoster = if (product.imageUrl.isNotBlank() && !product.imageUrl.endsWith(".mp4", ignoreCase = true)) {
        product.imageUrl
    } else {
        product.images.firstOrNull { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
    }

    if (product.hasVideo && product.videoUrl.isNotBlank()) {
        items.add(GalleryMediaItem.Video(videoUrl = product.videoUrl, posterUrl = basePoster))
        if (basePoster.isNotBlank()) {
            items.add(GalleryMediaItem.Image(imageUrl = basePoster))
        }
    }

    val images = getProductGalleryImages(product)
    images.forEach { imgUrl ->
        if (imgUrl.isNotBlank() && !imgUrl.endsWith(".mp4", ignoreCase = true)) {
            if (items.none { it is GalleryMediaItem.Image && it.imageUrl == imgUrl }) {
                items.add(GalleryMediaItem.Image(imageUrl = imgUrl))
            }
        }
    }
    return items
}

/**
 * Retorna únicamente las imágenes reales del producto provistas por el backend.
 */
fun getProductGalleryImages(product: Product): List<String> {
    return (listOfNotNull(
        product.imageUrl.takeIf { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }
    ) + product.images.filter { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }).distinct()
}
