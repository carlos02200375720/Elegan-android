package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.model.ProductJson
import com.example.data.model.productHasVideo
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.model.productVideoUrl

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
fun getProductMediaItems(product: ProductJson): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val imageUrl = product.productImageUrl()
    val productImgs = product.productImages()
    val videoUrl = product.productVideoUrl()
    val basePoster = if (imageUrl.isNotBlank() && !imageUrl.endsWith(".mp4", ignoreCase = true)) {
        imageUrl
    } else {
        productImgs.firstOrNull { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
    }

    if (product.productHasVideo() && videoUrl.isNotBlank()) {
        items.add(GalleryMediaItem.Video(videoUrl = videoUrl, posterUrl = basePoster))
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
fun getProductGalleryImages(product: ProductJson): List<String> {
    val imageUrl = product.productImageUrl()
    return (listOfNotNull(
        imageUrl.takeIf { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }
    ) + product.productImages().filter { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }).distinct()
}
