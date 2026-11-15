package com.example.ui.components

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import coil.request.ImageRequest
import coil.size.Precision
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val videoReleaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Almacenamiento local de contenido multimedia erradicado definitivamente.
 * No guarda ningún archivo ni fotograma en disco local.
 */
fun extractVideoPosterLocally(context: Context, videoUriStr: String): String? {
    return null
}

fun purgeLocalMediaStorage(context: Context) {
    try {
        val mediaDir = File(context.filesDir, "product_media")
        if (mediaDir.exists()) {
            mediaDir.deleteRecursively()
        }
    } catch (_: Exception) {}
}

internal fun ajustarTransformacionVideo(
    textureView: TextureView?,
    vWidth: Int,
    vHeight: Int
) {
    if (textureView == null) return
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || vWidth <= 0 || vHeight <= 0) return

    val viewRatio = viewWidth / viewHeight
    val videoRatio = vWidth.toFloat() / vHeight.toFloat()

    val scaleX: Float
    val scaleY: Float

    // Escala completa (Center Crop): ocupa todo el ancho y alto del contenedor
    if (videoRatio > viewRatio) {
        scaleX = videoRatio / viewRatio
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewRatio / videoRatio
    }

    val matrix = Matrix()
    matrix.setScale(scaleX, scaleY, viewWidth / 2f, viewHeight / 2f)
    textureView.setTransform(matrix)
}

internal fun construirPeticionPortadaVideo(
    context: Context,
    effectivePoster: String?
): ImageRequest? {
    return effectivePoster?.let { url ->
        ImageRequest.Builder(context)
            .data(url)
            .size(900, 900)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .crossfade(false)
            .build()
    }
}
