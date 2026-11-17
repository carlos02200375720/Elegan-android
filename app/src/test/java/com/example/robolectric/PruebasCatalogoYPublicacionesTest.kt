package com.example

import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.model.productMediaFormat
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productVideoUrl
import com.example.data.repository.EleganRepository
import com.example.data.repository.extractJsonItems
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasCatalogoYPublicacionesTest {

    @Test
    fun `test catalog starts empty without local products and handles backend json directly`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val repo = EleganRepository(context)

        val initialProducts = repo.observeProducts().first()
        assertTrue("El catálogo no debe contener productos locales precargados", initialProducts.isEmpty())

        val backendPayload = """
            {
              "items": [
                {
                  "id": "backend_prod_1",
                  "name": "Blazer Sartorial Azul",
                  "description": "Lana fría italiana",
                  "category": "camisa",
                  "price": 295.5,
                  "imageUrl": "https://cdn.example.com/uploads/blazer.jpg",
                  "images": ["https://cdn.example.com/uploads/blazer.jpg", "https://cdn.example.com/blazer-2.jpg"],
                  "sizes": ["48R", "50R"],
                  "colors": ["Azul Marino"],
                  "inStock": true,
                  "mediaFormat": "3:4"
                },
                {
                  "id": "backend_prod_2",
                  "title": "Oxford Borgoña",
                  "category": "zapato",
                  "price": "240.00",
                  "video_url": "https://cdn.example.com/uploads/oxford.mp4",
                  "in_stock": true
                }
              ]
            }
        """.trimIndent()

        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        val adapter = moshi.adapter<Map<String, Any?>>(mapType)
        val rawResponse = adapter.fromJson(backendPayload).orEmpty()

        val items = extractJsonItems(rawResponse)
        assertEquals(2, items.size)
        assertEquals("backend_prod_1", items[0].productId())
        assertEquals("https://cdn.example.com/uploads/blazer.jpg", items[0].productImageUrl())
        assertEquals(2, items[0].productImages().size)
        assertEquals("3:4", items[0].productMediaFormat())

        assertEquals("backend_prod_2", items[1].productId())
        assertEquals("Oxford Borgoña", items[1].productName())
        assertEquals(240.0, items[1].productPrice(), 0.01)
        assertEquals("https://cdn.example.com/uploads/oxford.mp4", items[1].productVideoUrl())
        assertTrue(items[1].productHasVideo())
    }

    @Test
    fun `test saved products and publication save counter update in memory without local storage`() = runBlocking {
        val context = obtenerContextoPrueba()
        val repo = EleganRepository(context)
        repo.setProductSaved("prod_test_1", true)

        val saved1 = repo.observeSavedProductIds().first()
        val stats1 = repo.observePublicationStats().first()
        assertTrue(saved1.contains("prod_test_1"))
        assertEquals(1L, stats1["prod_test_1"]?.savesCount)

        repo.setProductSaved("prod_test_1", false)
        val saved2 = repo.observeSavedProductIds().first()
        val stats2 = repo.observePublicationStats().first()
        assertFalse(saved2.contains("prod_test_1"))
        assertEquals(0L, stats2["prod_test_1"]?.savesCount ?: 0L)

        // Verificar que una nueva instancia no lee caché local en SharedPreferences
        val repoFresh = EleganRepository(context)
        assertTrue(repoFresh.observeSavedProductIds().first().isEmpty())
    }

    @Test
    fun `test liked publications toggle in memory without local storage`() = runBlocking {
        val context = obtenerContextoPrueba()
        val repo = EleganRepository(context)
        val likedAfterFirstTap = repo.togglePublicationLike("prod_like_1")
        assertTrue(likedAfterFirstTap)

        val likedIds1 = repo.observeLikedPublicationIds().first()
        val stats1 = repo.observePublicationStats().first()
        assertTrue(likedIds1.contains("prod_like_1"))
        assertEquals(1L, stats1["prod_like_1"]?.likesCount)

        val likedAfterSecondTap = repo.togglePublicationLike("prod_like_1")
        assertFalse(likedAfterSecondTap)

        val likedIds2 = repo.observeLikedPublicationIds().first()
        val stats2 = repo.observePublicationStats().first()
        assertFalse(likedIds2.contains("prod_like_1"))
        assertEquals(0L, stats2["prod_like_1"]?.likesCount ?: 0L)
    }
}
