package com.example.ui

import androidx.compose.runtime.Immutable
import com.example.data.model.Product
import com.example.data.model.esUrlDeVideo
import com.example.data.model.productGalleryVideoUrls
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
fun getProductMediaItems(product: Product): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val imageUrl = product.productImageUrl()
    val productImgs = product.productImages()
    val videoUrl = product.productVideoUrl()
    val galleryVideos = product.productGalleryVideoUrls()
    val videoUrlSet = galleryVideos.toSet()

    val basePoster = if (imageUrl.isNotBlank() && !esUrlDeVideo(imageUrl) && imageUrl !in videoUrlSet) {
        imageUrl
    } else {
        productImgs.firstOrNull { it.isNotBlank() && !esUrlDeVideo(it) && it !in videoUrlSet }.orEmpty()
    }

    if (product.productHasVideo() && videoUrl.isNotBlank()) {
        items.add(GalleryMediaItem.Video(videoUrl = videoUrl, posterUrl = basePoster))
        if (basePoster.isNotBlank()) {
            items.add(GalleryMediaItem.Image(imageUrl = basePoster))
        }
    }

    val images = getProductGalleryImages(product)
    images.forEach { imgUrl ->
        if (imgUrl.isNotBlank() && !esUrlDeVideo(imgUrl) && imgUrl !in videoUrlSet) {
            if (items.none { it is GalleryMediaItem.Image && it.imageUrl == imgUrl }) {
                items.add(GalleryMediaItem.Image(imageUrl = imgUrl))
            }
        }
    }

    galleryVideos.forEach { extraVideo ->
        if (extraVideo.isNotBlank() && items.none { it is GalleryMediaItem.Video && it.videoUrl == extraVideo }) {
            items.add(GalleryMediaItem.Video(videoUrl = extraVideo, posterUrl = basePoster))
        }
    }
    return items
}

/**
 * Retorna únicamente las imágenes reales del producto provistas por el backend.
 */
fun getProductGalleryImages(product: Product): List<String> {
    val videoUrlSet = product.productGalleryVideoUrls().toSet()
    val imageUrl = product.productImageUrl()
    return (listOfNotNull(
        imageUrl.takeIf { it.isNotBlank() && !esUrlDeVideo(it) && it !in videoUrlSet }
    ) + product.productImages().filter { it.isNotBlank() && !esUrlDeVideo(it) && it !in videoUrlSet }).distinct()
}
