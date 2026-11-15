package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.data.model.Product
import com.example.ui.components.EleganVideoPlayer

@Composable
internal fun ElementoMultimediaPublicacion(
    product: Product,
    item: GalleryMediaItem?,
    page: Int,
    ratio: Float,
    isVideoFocused: Boolean,
    onProductClick: () -> Unit
) {
    when (item) {
        is GalleryMediaItem.Video -> EleganVideoPlayer(
            videoUrl = item.videoUrl,
            posterUrl = item.posterUrl,
            aspectRatio = ratio,
            autoPlay = isVideoFocused,
            isFocused = isVideoFocused,
            showControls = true,
            testTagPrefix = "inicio_video_${product.id}_$page",
            modifier = Modifier.fillMaxSize()
        )
        is GalleryMediaItem.Image -> ImagenPublicacionProducto(
            imageUrl = item.imageUrl,
            contentDescription = "${product.name} - ${page + 1}",
            onClick = onProductClick
        )
        null -> {}
    }
}

@Composable
private fun ImagenPublicacionProducto(
    imageUrl: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(imageUrl, context) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .size(900, 900)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    )
}
