package com.example

import com.example.ui.formatearDescuentoRd
import com.example.ui.formatearEnvioRd
import com.example.ui.formatearMonedaRD
import com.example.ui.formatearMontoDecimal
import com.example.ui.formatearPrecioRd
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun priceFormatting_isConsistentRegardlessOfDeviceLocale() {
    val previousLocale = Locale.getDefault()
    try {
      for (locale in listOf(Locale.US, Locale.forLanguageTag("es-ES"), Locale.GERMANY, Locale.FRANCE)) {
        Locale.setDefault(locale)
        assertEquals("1.500", formatearMontoDecimal(1500.0))
        assertEquals("100", formatearMontoDecimal(100.0))
        assertEquals("1.250,50", formatearMontoDecimal(1250.5))
        assertEquals("1.000.000", formatearMontoDecimal(1_000_000.0))
        assertEquals("0", formatearMontoDecimal(-0.0))
        assertEquals("0", formatearMontoDecimal(Double.NaN))
        assertEquals("RD$ 1.500", formatearMonedaRD(1500.0))
        assertEquals("RD$ 100", formatearPrecioRd(100.0))
        assertEquals("RD$ 55", formatearPrecioRd(55.0))
        assertEquals("-RD$ 12,75", formatearDescuentoRd(12.75))
        assertEquals("-RD$ 500", formatearDescuentoRd(500.0))
        assertEquals("GRATIS", formatearEnvioRd(0.0))
        assertEquals("RD$ 250", formatearEnvioRd(250.0))
        assertEquals("RD$ 1.500", formatearEnvioRd(1500.0))
      }
    } finally {
      Locale.setDefault(previousLocale)
    }
  }

  @Test
  fun mediaUrlAndGalleryClassification_handlesVideosSpacesAndInvalidMapNames() {
    assertTrue(com.example.data.model.esUrlDeVideo("https://cdn.elegan.com/clip.mp4?Expires=9999&Signature=abc"))
    assertTrue(com.example.data.model.esUrlDeVideo("https://cdn.elegan.com/clip.MOV?alt=media"))
    assertTrue(com.example.data.model.esUrlDeVideo("https://cdn.elegan.com/clip.webm#t=2"))
    assertFalse(com.example.data.model.esUrlDeVideo("https://cdn.elegan.com/camisa.jpg?v=2"))

    assertEquals(
      "https://cdn.elegan.com/uploads/mi%20camisa%201.jpg",
      com.example.data.model.resolveBackendMediaUrl("https://cdn.elegan.com/uploads/mi camisa 1.jpg")
    )
    assertEquals("", com.example.data.model.resolveBackendMediaUrl("http://cdn.elegan.com/img.jpg"))

    val productWithMixedGallery: com.example.data.model.ProductJson = mapOf(
      "id" to "prod_mixed_media",
      "name" to "Camisa Oxford",
      "imageUrl" to "https://cdn.elegan.com/cover.jpg",
      "images" to listOf(
        "https://cdn.elegan.com/cover.jpg",
        "https://cdn.elegan.com/slide 2.jpg",
        "https://cdn.elegan.com/runway.mp4?token=signed123",
        mapOf("name" to "Etiqueta sin URL"),
        mapOf("url" to "https://cdn.elegan.com/clip.mov?alt=media", "type" to "video")
      )
    )

    val galleryImages = com.example.ui.getProductGalleryImages(productWithMixedGallery)
    assertEquals(
      listOf(
        "https://cdn.elegan.com/cover.jpg",
        "https://cdn.elegan.com/slide%202.jpg"
      ),
      galleryImages
    )

    val mediaItems = com.example.ui.getProductMediaItems(productWithMixedGallery)
    val imageItems = mediaItems.filterIsInstance<com.example.ui.GalleryMediaItem.Image>()
    val videoItems = mediaItems.filterIsInstance<com.example.ui.GalleryMediaItem.Video>()
    assertEquals(2, imageItems.size)
    assertEquals(2, videoItems.size)
    assertEquals("https://cdn.elegan.com/runway.mp4?token=signed123", videoItems[0].videoUrl)
    assertEquals("https://cdn.elegan.com/clip.mov?alt=media", videoItems[1].videoUrl)
  }
}
