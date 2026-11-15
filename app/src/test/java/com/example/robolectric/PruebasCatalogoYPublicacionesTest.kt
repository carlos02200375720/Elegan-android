package com.example

import com.example.data.model.Product
import com.example.data.remote.MobileSession
import com.example.data.repository.EleganRepository
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
    fun `test local publish and delete product persist across repository refresh`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        limpiarPreferenciasPrueba(context, "elegan_catalog_store")

        val repo1 = EleganRepository(context)
        val publishedId = repo1.addProduct(
            Product(
                name = "Traje Local Exclusivo",
                description = "Traje confeccionado localmente",
                category = "camisa",
                price = 320.0
            )
        )

        val products1 = repo1.observeProducts().first()
        assertTrue(products1.any { it.id == publishedId && it.name == "Traje Local Exclusivo" })

        val repo2 = EleganRepository(context)
        val products2 = repo2.observeProducts().first()
        assertTrue(products2.any { it.id == publishedId && it.name == "Traje Local Exclusivo" })

        repo2.deleteProduct(publishedId)
        val repo3 = EleganRepository(context)
        val products3 = repo3.observeProducts().first()
        assertFalse(products3.any { it.id == publishedId })

        limpiarPreferenciasPrueba(context, "elegan_catalog_store")
    }

    @Test
    fun `test saved products and publication save counter persist across repository refresh`() = runBlocking {
        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo1 = EleganRepository(context)
        repo1.setProductSaved("prod_test_1", true)

        val saved1 = repo1.observeSavedProductIds().first()
        val stats1 = repo1.observePublicationStats().first()
        assertTrue(saved1.contains("prod_test_1"))
        assertEquals(1L, stats1["prod_test_1"]?.savesCount)

        // Simular reinicio / recarga con una nueva instancia del repositorio
        val repo2 = EleganRepository(context)
        val saved2 = repo2.observeSavedProductIds().first()
        val stats2 = repo2.observePublicationStats().first()
        assertTrue(saved2.contains("prod_test_1"))
        assertEquals(1L, stats2["prod_test_1"]?.savesCount)

        // Quitar guardado y verificar persistencia del decremento en otra instancia nueva
        repo2.setProductSaved("prod_test_1", false)
        val repo3 = EleganRepository(context)
        val saved3 = repo3.observeSavedProductIds().first()
        val stats3 = repo3.observePublicationStats().first()
        assertFalse(saved3.contains("prod_test_1"))
        assertEquals(0L, stats3["prod_test_1"]?.savesCount ?: 0L)

        MobileSession.clear(context)
    }

    @Test
    fun `test liked publications toggle and persist across repository refresh`() = runBlocking {
        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo1 = EleganRepository(context)
        val likedAfterFirstTap = repo1.togglePublicationLike("prod_like_1")
        assertTrue(likedAfterFirstTap)

        val likedIds1 = repo1.observeLikedPublicationIds().first()
        val stats1 = repo1.observePublicationStats().first()
        assertTrue(likedIds1.contains("prod_like_1"))
        assertEquals(1L, stats1["prod_like_1"]?.likesCount)

        // Simular reinicio / recarga con nueva instancia
        val repo2 = EleganRepository(context)
        val likedIds2 = repo2.observeLikedPublicationIds().first()
        val stats2 = repo2.observePublicationStats().first()
        assertTrue(likedIds2.contains("prod_like_1"))
        assertEquals(1L, stats2["prod_like_1"]?.likesCount)

        // Quitar like y verificar persistencia del estado y decremento
        val likedAfterSecondTap = repo2.togglePublicationLike("prod_like_1")
        assertFalse(likedAfterSecondTap)

        val repo3 = EleganRepository(context)
        val likedIds3 = repo3.observeLikedPublicationIds().first()
        val stats3 = repo3.observePublicationStats().first()
        assertFalse(likedIds3.contains("prod_like_1"))
        assertEquals(0L, stats3["prod_like_1"]?.likesCount ?: 0L)

        MobileSession.clear(context)
    }

    @Test
    fun `test toggle product stock and announcement active persist across repository refresh`() {
        runBlocking {
            val context = obtenerContextoPrueba()
            limpiarPreferenciasPrueba(context, "elegan_publication_states")
            MobileSession.clear(context)

            val repo1 = EleganRepository(context)
            val firstProduct = repo1.getInitialSampleProducts().first()
            val firstAnnouncement = repo1.getInitialSampleAnnouncements().first()

            repo1.toggleProductStock(firstProduct.id, false)
            repo1.toggleAnnouncementActive(firstAnnouncement.id, false)

            val products1 = repo1.observeProducts().first()
            val announcements1 = repo1.observeAnnouncements().first()
            assertFalse(products1.first { it.id == firstProduct.id }.inStock)
            assertFalse(announcements1.first { it.id == firstAnnouncement.id }.isActive)

            // Simular recarga con una nueva instancia del repositorio
            val repo2 = EleganRepository(context)
            val products2 = repo2.observeProducts().first()
            val announcements2 = repo2.observeAnnouncements().first()
            assertFalse(products2.first { it.id == firstProduct.id }.inStock)
            assertFalse(announcements2.first { it.id == firstAnnouncement.id }.isActive)

            // Volver a encender y verificar actualización
            repo2.toggleProductStock(firstProduct.id, true)
            repo2.toggleAnnouncementActive(firstAnnouncement.id, true)
            val repo3 = EleganRepository(context)
            assertTrue(repo3.observeProducts().first().first { it.id == firstProduct.id }.inStock)
            assertTrue(repo3.observeAnnouncements().first().first { it.id == firstAnnouncement.id }.isActive)

            limpiarPreferenciasPrueba(context, "elegan_publication_states")
        }
    }
}
