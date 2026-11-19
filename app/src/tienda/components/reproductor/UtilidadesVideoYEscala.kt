package com.example.ui.components

import android.content.Context
import android.graphics.Matrix
import android.view.TextureView
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.request.ImageRequest
import coil.size.Precision
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal val videoReleaseScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private object CacheMultimediaVideo {
    private const val TAMANO_MAXIMO_CACHE = 200L * 1024L * 1024L

    @Volatile
    private var instancia: SimpleCache? = null

    fun obtener(context: Context): SimpleCache {
        return instancia ?: synchronized(this) {
            instancia ?: SimpleCache(
                File(context.cacheDir, "elegan_video_cache"),
                LeastRecentlyUsedCacheEvictor(TAMANO_MAXIMO_CACHE),
                StandaloneDatabaseProvider(context.applicationContext)
            ).also { instancia = it }
        }
    }
}

internal fun crearMediaSourceConCacheVideo(context: Context): DefaultMediaSourceFactory {
    val servidor = DefaultHttpDataSource.Factory()
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(30_000)

    val fuente = CacheDataSource.Factory()
        .setCache(CacheMultimediaVideo.obtener(context))
        .setUpstreamDataSourceFactory(servidor)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    return DefaultMediaSourceFactory(fuente)
}

/**
 * No extrae ni guarda fotogramas locales; la caché se limita a los datos
 * de reproducción y se administra por tamaño máximo.
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
