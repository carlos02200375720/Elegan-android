package com.example

import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.size.Precision
import com.example.data.media.ConfiguracionCacheImagenesCoil
import com.example.data.model.Product
import com.example.ui.calcularUrlsMiniaturasAPrecargar
import java.io.File
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasCacheImagenesCoilTest {

    @Test
    fun `ImageLoader configura cache de disco persistente acotada y gestion de memoria adaptativa`() {
        val context = obtenerContextoPrueba()
        val isolatedDiskDir = File(context.cacheDir, "test_coil_disk_${UUID.randomUUID()}")
        val loader = ConfiguracionCacheImagenesCoil.crearImageLoader(
            context = context,
            diskCacheDirectory = isolatedDiskDir,
            forceLowRam = false
        )
        val lowRamLoader = ConfiguracionCacheImagenesCoil.crearImageLoader(
            context = context,
            diskCacheDirectory = File(context.cacheDir, "test_coil_disk_low_${UUID.randomUUID()}"),
            forceLowRam = true
        )
        try {
            val diskCache = loader.diskCache
            assertNotNull("Debe inicializar DiskCache persistente", diskCache)
            assertEquals(isolatedDiskDir.canonicalPath, diskCache!!.directory.toFile().canonicalPath)
            assertTrue(
                "El límite de caché de disco debe estar dentro de [64 MB, 250 MB], actual=${diskCache.maxSize}",
                diskCache.maxSize in ConfiguracionCacheImagenesCoil.DISK_CACHE_MIN_BYTES..ConfiguracionCacheImagenesCoil.DISK_CACHE_MAX_BYTES
            )

            val stdMemoryCache = loader.memoryCache
            val lowRamMemoryCache = lowRamLoader.memoryCache
            assertNotNull(stdMemoryCache)
            assertNotNull(lowRamMemoryCache)
            assertTrue(
                "Los dispositivos low-RAM deben reservar menos memoria que los estándar",
                lowRamMemoryCache!!.maxSize < stdMemoryCache!!.maxSize
            )

            assertEquals(CachePolicy.ENABLED, loader.defaults.diskCachePolicy)
            assertEquals(CachePolicy.ENABLED, loader.defaults.memoryCachePolicy)
            assertEquals(CachePolicy.ENABLED, loader.defaults.networkCachePolicy)
            assertTrue(loader.defaults.allowRgb565)
            assertTrue(loader.defaults.allowHardware)
            assertTrue(
                "Debe registrar los normalizadores de clave canónica en Coil",
                loader.components.keyers.size >= 2
            )
        } finally {
            loader.shutdown()
            lowRamLoader.shutdown()
            isolatedDiskDir.deleteRecursively()
        }
    }

    @Test
    fun `normalizarClaveCacheUrl ignora parametros volatiles de firma y conserva identidad de imagen`() {
        val signedToday = "https://CDN.Elegan.com/products/traje-01.jpg?w=600&X-Goog-Signature=abc123&X-Goog-Expires=3600&token=t1"
        val signedTomorrow = "https://cdn.elegan.com/products/traje-01.jpg?X-Goog-Signature=xyz999&Expires=999999&w=600&token=t2"
        val differentVariant = "https://cdn.elegan.com/products/traje-01.jpg?w=1200&X-Goog-Signature=xyz999"

        val keyToday = ConfiguracionCacheImagenesCoil.normalizarClaveCacheUrl(signedToday)
        val keyTomorrow = ConfiguracionCacheImagenesCoil.normalizarClaveCacheUrl(signedTomorrow)
        val keyVariant = ConfiguracionCacheImagenesCoil.normalizarClaveCacheUrl(differentVariant)

        assertEquals("https://cdn.elegan.com/products/traje-01.jpg?w=600", keyToday)
        assertEquals(keyToday, keyTomorrow)
        assertNotEquals(keyToday, keyVariant)
    }

    @Test
    fun `peticiones de miniatura y detalle comparten clave de disco y enlazan placeholder en RAM`() {
        val context = obtenerContextoPrueba()
        val urlSignedV1 = "https://cdn.elegan.com/media/saco.jpg?X-Amz-Signature=sig1&X-Amz-Expires=120"
        val urlSignedV2 = "https://cdn.elegan.com/media/saco.jpg?X-Amz-Signature=sig2&X-Amz-Expires=900"

        val gridThumb = ConfiguracionCacheImagenesCoil.peticionMiniaturaProducto(context, urlSignedV1)
        val compactThumb = ConfiguracionCacheImagenesCoil.peticionMiniaturaCompacta(context, urlSignedV2)
        val detailImage = ConfiguracionCacheImagenesCoil.peticionImagenPublicacion(context, urlSignedV2)

        assertEquals(gridThumb.diskCacheKey, compactThumb.diskCacheKey)
        assertEquals(gridThumb.diskCacheKey, detailImage.diskCacheKey)
        assertEquals(gridThumb.memoryCacheKey, detailImage.placeholderMemoryCacheKey)
        assertEquals(gridThumb.memoryCacheKey, compactThumb.placeholderMemoryCacheKey)

        assertEquals(Bitmap.Config.RGB_565, gridThumb.bitmapConfig)
        assertEquals(Precision.INEXACT, gridThumb.precision)
        assertEquals(CachePolicy.ENABLED, gridThumb.diskCachePolicy)
        assertEquals(CachePolicy.ENABLED, gridThumb.memoryCachePolicy)
    }

    @Test
    fun `recortarMemoria libera RAM bajo presion manteniendo intacta la cache persistente en disco`() {
        val context = obtenerContextoPrueba()
        val isolatedDiskDir = File(context.cacheDir, "test_coil_trim_${UUID.randomUUID()}")
        val loader = ConfiguracionCacheImagenesCoil.crearImageLoader(
            context = context,
            diskCacheDirectory = isolatedDiskDir
        )
        try {
            val memoryCache = loader.memoryCache!!
            val diskCache = loader.diskCache!!
            val key = MemoryCache.Key("https://cdn.elegan.com/p1.jpg#s=480")
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.RGB_565)
            memoryCache[key] = MemoryCache.Value(bitmap)

            val editor = diskCache.openEditor("https://cdn.elegan.com/p1.jpg")!!
            editor.data.toFile().writeBytes(byteArrayOf(1, 2, 3, 4))
            editor.commit()

            assertTrue(memoryCache.size > 0)
            assertNotNull(diskCache.openSnapshot("https://cdn.elegan.com/p1.jpg")?.also { it.close() })

            ConfiguracionCacheImagenesCoil.recortarMemoria(
                loader,
                ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL
            )

            assertEquals("La memoria RAM debe liberarse ante presión crítica", 0, memoryCache.size)
            val persisted = diskCache.openSnapshot("https://cdn.elegan.com/p1.jpg")
            assertNotNull("La caché persistente en disco debe permanecer intacta tras trimMemory", persisted)
            persisted?.close()
        } finally {
            loader.shutdown()
            isolatedDiskDir.deleteRecursively()
        }
    }

    @Test
    fun `calcularUrlsMiniaturasAPrecargar selecciona ventana acotada para cuadrícula paginada`() {
        val products = (1..50).map { index ->
            Product(
                id = "p_$index",
                name = "Prenda $index",
                imageUrl = if (index == 5) {
                    "https://cdn.elegan.com/video_$index.mp4"
                } else {
                    "https://cdn.elegan.com/img_$index.jpg"
                }
            )
        }

        val urls = calcularUrlsMiniaturasAPrecargar(
            products = products,
            firstVisibleItemIndex = 4, // 2 headers + producto índice 2 (p_3)
            visibleItemCount = 6,
            headerItemCount = 2,
            lookaheadCount = 8
        )

        // Rango de índices: [2, 16) -> 14 productos menos p_5 (que es video .mp4) = 13 miniaturas
        assertEquals(13, urls.size)
        assertEquals("https://cdn.elegan.com/img_3.jpg", urls.first())
        assertEquals("https://cdn.elegan.com/img_16.jpg", urls.last())
        assertTrue(urls.none { it.endsWith(".mp4") })
    }
}
