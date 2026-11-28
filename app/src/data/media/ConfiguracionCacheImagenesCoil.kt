package com.example.data.media

import android.app.ActivityManager
import android.content.ComponentCallbacks2
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import coil.ImageLoader
import coil.disk.DiskCache
import coil.key.Keyer
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.Options
import coil.size.Precision
import coil.size.Scale
import com.example.data.remote.ClienteHttpElegan
import java.io.File
import kotlinx.coroutines.Dispatchers
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

/**
 * Configuración centralizada de Coil para caché persistente en disco y gestión optimizada de memoria.
 *
 * - Disco persistente dedicado (`elegan_coil_disk_cache`) con presupuesto adaptativo (64 MB – 250 MB)
 *   y limpieza asíncrona en [Dispatchers.IO].
 * - Memoria optimizada según capacidad del dispositivo (22% en dispositivos estándar, 14% en low-RAM),
 *   con referencias fuertes + débiles y recorte proactivo en `onTrimMemory`/`onLowMemory`.
 * - Claves de caché canónicas que ignoran parámetros volátiles de firma/expiración de CDN
 *   para reutilizar miniaturas persistidas al navegar por listas paginadas o reiniciar la app.
 * - Transición instantánea desde miniatura en RAM hacia vista detalle mediante `placeholderMemoryCacheKey`.
 */
object ConfiguracionCacheImagenesCoil {

    const val DISK_CACHE_DIR_NAME: String = "elegan_coil_disk_cache"
    const val DISK_CACHE_MAX_PERCENT: Double = 0.04
    const val DISK_CACHE_MIN_BYTES: Long = 64L * 1024L * 1024L
    const val DISK_CACHE_MAX_BYTES: Long = 250L * 1024L * 1024L

    const val MEMORY_CACHE_PERCENT_STANDARD: Double = 0.22
    const val MEMORY_CACHE_PERCENT_LOW_RAM: Double = 0.14

    const val THUMBNAIL_COMPACT_SIZE_PX: Int = 240
    const val THUMBNAIL_GRID_SIZE_PX: Int = 480
    const val FEED_MEDIA_SIZE_PX: Int = 900

    private val VOLATILE_QUERY_PARAMS = setOf(
        "x-goog-signature",
        "x-goog-date",
        "x-goog-expires",
        "x-goog-credential",
        "x-goog-algorithm",
        "x-goog-signedheaders",
        "x-amz-signature",
        "x-amz-date",
        "x-amz-expires",
        "x-amz-credential",
        "x-amz-security-token",
        "x-amz-algorithm",
        "x-amz-signedheaders",
        "signature",
        "expires",
        "policy",
        "key-pair-id",
        "token"
    )

    fun directorioCacheDisco(context: Context): File =
        File(context.applicationContext.cacheDir, DISK_CACHE_DIR_NAME)

    fun porcentajeCacheMemoria(context: Context, forceLowRam: Boolean? = null): Double {
        val isLowRam = forceLowRam ?: esDispositivoBajaMemoria(context)
        return if (isLowRam) MEMORY_CACHE_PERCENT_LOW_RAM else MEMORY_CACHE_PERCENT_STANDARD
    }

    fun crearMemoryCache(context: Context, forceLowRam: Boolean? = null): MemoryCache {
        return MemoryCache.Builder(context.applicationContext)
            .maxSizePercent(porcentajeCacheMemoria(context, forceLowRam))
            .strongReferencesEnabled(true)
            .weakReferencesEnabled(true)
            .build()
    }

    fun crearDiskCache(
        context: Context,
        directory: File = directorioCacheDisco(context)
    ): DiskCache {
        return DiskCache.Builder()
            .directory(directory)
            .maxSizePercent(DISK_CACHE_MAX_PERCENT)
            .minimumMaxSizeBytes(DISK_CACHE_MIN_BYTES)
            .maximumMaxSizeBytes(DISK_CACHE_MAX_BYTES)
            .cleanupDispatcher(Dispatchers.IO)
            .build()
    }

    fun crearImageLoader(
        context: Context,
        okHttpClientProvider: () -> OkHttpClient = { ClienteHttpElegan.mediaClient },
        diskCacheDirectory: File = directorioCacheDisco(context),
        forceLowRam: Boolean? = null
    ): ImageLoader {
        val appContext = context.applicationContext
        val decodeParallelism = Runtime.getRuntime()
            .availableProcessors()
            .coerceIn(2, 4)

        return ImageLoader.Builder(appContext)
            .okHttpClient { okHttpClientProvider() }
            .memoryCache { crearMemoryCache(appContext, forceLowRam) }
            .diskCache { crearDiskCache(appContext, diskCacheDirectory) }
            .components {
                add(NormalizadorClaveCacheString())
                add(NormalizadorClaveCacheHttpUrl())
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .respectCacheHeaders(false)
            .allowRgb565(true)
            .allowHardware(true)
            .bitmapFactoryMaxParallelism(decodeParallelism)
            .crossfade(false)
            .build()
    }

    /**
     * Recorta la caché de memoria de Coil ante presión del sistema manteniendo intacta la caché en disco.
     */
    @Suppress("DEPRECATION")
    fun recortarMemoria(imageLoader: ImageLoader?, level: Int) {
        val memoryCache = imageLoader?.memoryCache ?: return
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND ||
                level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> {
                memoryCache.clear()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN ||
                level == ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> {
                memoryCache.trimMemory(level)
            }
        }
    }

    /**
     * Normaliza URLs de medios eliminando parámetros volátiles de firma/expiración
     * para asegurar aciertos persistentes en disco y RAM al navegar o reabrir la app.
     */
    fun normalizarClaveCacheUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) return ""
        val parsed = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return trimmed
        val scheme = parsed.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return trimmed
        }
        val host = parsed.host?.lowercase() ?: return trimmed
        val portSuffix = if (parsed.port != -1 &&
            !((scheme == "https" && parsed.port == 443) || (scheme == "http" && parsed.port == 80))
        ) {
            ":${parsed.port}"
        } else {
            ""
        }
        val path = parsed.encodedPath.orEmpty()
        val stableQuery = parsed.queryParameterNames
            .asSequence()
            .filter { name -> name.lowercase() !in VOLATILE_QUERY_PARAMS }
            .sorted()
            .flatMap { name ->
                parsed.getQueryParameters(name).asSequence().map { value ->
                    "${Uri.encode(name)}=${Uri.encode(value)}"
                }
            }
            .joinToString("&")

        return buildString(trimmed.length) {
            append(scheme)
            append("://")
            append(host)
            append(portSuffix)
            append(path)
            if (stableQuery.isNotEmpty()) {
                append('?')
                append(stableQuery)
            }
        }
    }

    fun claveCacheDisco(imageUrl: String): String = normalizarClaveCacheUrl(imageUrl)

    fun claveCacheMemoria(imageUrl: String, maxSizePx: Int): String =
        "${normalizarClaveCacheUrl(imageUrl)}#s=$maxSizePx"

    /**
     * Petición estandarizada para miniaturas de productos en cuadrículas paginadas (Catálogo, Guardados).
     */
    fun peticionMiniaturaProducto(
        context: Context,
        imageUrl: String,
        maxSizePx: Int = THUMBNAIL_GRID_SIZE_PX
    ): ImageRequest {
        val diskKey = claveCacheDisco(imageUrl)
        val memKey = claveCacheMemoria(imageUrl, maxSizePx)
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCacheKey(diskKey)
            .memoryCacheKey(memKey)
            .size(maxSizePx, maxSizePx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    /**
     * Petición compacta de baja huella RAM para miniaturas pequeñas (carrito, variantes, checkout, categorías).
     * Reutiliza la misma clave en disco que la miniatura de catálogo evitando descargas duplicadas.
     */
    fun peticionMiniaturaCompacta(
        context: Context,
        imageUrl: String,
        maxSizePx: Int = THUMBNAIL_COMPACT_SIZE_PX
    ): ImageRequest {
        val diskKey = claveCacheDisco(imageUrl)
        val memKey = claveCacheMemoria(imageUrl, maxSizePx)
        return ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCacheKey(diskKey)
            .memoryCacheKey(memKey)
            .placeholderMemoryCacheKey(MemoryCache.Key(claveCacheMemoria(imageUrl, THUMBNAIL_GRID_SIZE_PX)))
            .size(maxSizePx, maxSizePx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .allowRgb565(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build()
    }

    /**
     * Petición para publicaciones del feed y visor de detalle.
     * Reutiliza la miniatura en RAM como placeholder instantáneo y la misma entrada en disco.
     */
    fun peticionImagenPublicacion(
        context: Context,
        imageUrl: String,
        maxSizePx: Int = FEED_MEDIA_SIZE_PX,
        retryAttempt: Int = 0
    ): ImageRequest {
        val diskKey = claveCacheDisco(imageUrl)
        val memKey = claveCacheMemoria(imageUrl, maxSizePx)
        val builder = ImageRequest.Builder(context)
            .data(imageUrl)
            .diskCacheKey(diskKey)
            .memoryCacheKey(memKey)
            .size(maxSizePx, maxSizePx)
            .scale(Scale.FILL)
            .precision(Precision.INEXACT)
            .allowRgb565(true)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)

        if (maxSizePx != THUMBNAIL_GRID_SIZE_PX) {
            builder.placeholderMemoryCacheKey(
                MemoryCache.Key(claveCacheMemoria(imageUrl, THUMBNAIL_GRID_SIZE_PX))
            )
        }

        if (retryAttempt > 0) {
            builder
                .memoryCachePolicy(CachePolicy.WRITE_ONLY)
                .setParameter("elegan_retry", retryAttempt, memoryCacheKey = null)
        } else {
            builder.memoryCachePolicy(CachePolicy.ENABLED)
        }
        return builder.build()
    }

    private fun esDispositivoBajaMemoria(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        return activityManager?.isLowRamDevice == true
    }

    private class NormalizadorClaveCacheString : Keyer<String> {
        override fun key(data: String, options: Options): String? {
            val trimmed = data.trim()
            if (!trimmed.startsWith("http://", ignoreCase = true) &&
                !trimmed.startsWith("https://", ignoreCase = true)
            ) {
                return null
            }
            return normalizarClaveCacheUrl(trimmed)
        }
    }

    private class NormalizadorClaveCacheHttpUrl : Keyer<HttpUrl> {
        override fun key(data: HttpUrl, options: Options): String =
            normalizarClaveCacheUrl(data.toString())
    }
}
