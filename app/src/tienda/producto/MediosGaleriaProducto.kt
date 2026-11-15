package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Product

@Immutable
sealed interface GalleryMediaItem {
    @Immutable
    data class Video(val videoUrl: String, val posterUrl: String) : GalleryMediaItem

    @Immutable
    data class Image(val imageUrl: String) : GalleryMediaItem
}

/**
 * Retorna la lista unificada de elementos multimedia (video y fotos) para la galería.
 * Si el producto tiene video, la portada del video también acompaña como imagen la galería.
 */
fun getProductMediaItems(product: Product): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val basePoster = if (product.imageUrl.isNotBlank() && !product.imageUrl.endsWith(".mp4", ignoreCase = true)) {
        product.imageUrl
    } else {
        product.images.firstOrNull { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
    }

    if (product.hasVideo && product.videoUrl.isNotBlank()) {
        val videoPoster = basePoster.ifBlank {
            getDefaultCategoryFallback(product.category)
        }
        items.add(GalleryMediaItem.Video(videoUrl = product.videoUrl, posterUrl = videoPoster))
        if (videoPoster.isNotBlank()) {
            items.add(GalleryMediaItem.Image(imageUrl = videoPoster))
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
    return if (items.isNotEmpty()) items else listOf(GalleryMediaItem.Image(basePoster))
}

fun getDefaultCategoryFallback(category: String): String {
    return LocalBoutiqueMedia.fallbackImageForCategory(category)
}

/**
 * Retorna la lista de imágenes para la galería multimedia del producto.
 * Si el producto ya cuenta con su lista de imágenes, se utiliza dicha lista.
 * Si no, se proveen perspectivas locales acordes a su categoría.
 */
fun getProductGalleryImages(product: Product): List<String> {
    val userImages = (listOfNotNull(
        product.imageUrl.takeIf { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }
    ) + product.images.filter { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }).distinct()
    if (userImages.isNotEmpty()) return userImages

    val base = if (product.imageUrl.isNotBlank() && !product.imageUrl.endsWith(".mp4", ignoreCase = true)) {
        product.imageUrl
    } else {
        getDefaultCategoryFallback(product.category)
    }

    val supplements = LocalBoutiqueMedia.gallerySupplementsForCategory(product.category)
    return (listOf(base) + supplements).distinct()
}
