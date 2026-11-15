package com.example

import com.example.data.remote.MobileSession
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasPerfilPedidosYSoporteTest {

    @Test
    fun `test local user delivery profile persists across repository refresh`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        limpiarPreferenciasPrueba(context, "elegan_profile_cache")
        reiniciarSesionClienteDemo(context)

        val repo1 = EleganRepository(context)
        repo1.saveUserProfile(
            phone = "809-555-2026",
            address = "Av. Abraham Lincoln 450",
            city = "Santo Domingo"
        )

        val profile1 = repo1.observeUserProfile().first()
        assertEquals("809-555-2026", profile1?.phone)
        assertEquals("Av. Abraham Lincoln 450", profile1?.address)
        assertEquals("Santo Domingo", profile1?.city)

        val repo2 = EleganRepository(context)
        val profile2 = repo2.observeUserProfile().first()
        assertEquals("809-555-2026", profile2?.phone)
        assertEquals("Av. Abraham Lincoln 450", profile2?.address)
        assertEquals("Santo Domingo", profile2?.city)

        limpiarPreferenciasPrueba(context, "elegan_profile_cache")
        MobileSession.clear(context)
    }

    @Test
    fun `test created order is saved and persists in order history across repository refresh`() = runBlocking {
        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo1 = EleganRepository(context)
        val sampleProduct = repo1.getInitialSampleProducts().first()
        repo1.addToCart(sampleProduct, selectedSize = "M", quantity = 2L, selectedColor = "Negro")

        val orderId = repo1.createOrder(
            customerName = "Cliente Elegan",
            email = "cliente.elegan@gmail.com",
            phone = "809-555-0199",
            address = "Av. Winston Churchill 101",
            city = "Santo Domingo",
            paymentMethod = "Tarjeta",
            total = 110.0,
            itemCount = 2L,
            itemsSummary = "2x ${sampleProduct.name}"
        )

        val orders1 = repo1.observeOrders().first()
        val cart1 = repo1.observeCart().first()
        assertEquals(1, orders1.size)
        assertEquals(orderId, orders1.first().id)
        assertEquals("Cliente Elegan", orders1.first().customerName)
        assertEquals(110.0, orders1.first().total, 0.01)
        assertTrue(cart1.isEmpty())

        // Simular reinicio / recarga y comprobar que el historial de pedidos conserva el pedido
        val repo2 = EleganRepository(context)
        val orders2 = repo2.observeOrders().first()
        assertEquals(1, orders2.size)
        assertEquals(orderId, orders2.first().id)
        assertEquals("Av. Winston Churchill 101", orders2.first().address)

        MobileSession.clear(context)
    }

    @Test
    fun `test support chat sends and persists image and video messages`() {
        runBlocking {
            val context = obtenerContextoPrueba()
            limpiarPreferenciasPrueba(context, "elegan_support_messages")
            reiniciarSesionClienteDemo(context)

            val repo1 = EleganRepository(context)
            repo1.sendSupportMessage(
                text = "Adjunto foto del producto",
                mediaUri = "android.resource://com.elegan/drawable/img_cat_camisa",
                mediaType = "image",
                scope = this
            )
            repo1.sendSupportMessage(
                text = "",
                mediaUri = "android.resource://com.elegan/raw/demo_video",
                mediaType = "video",
                scope = this
            )

            val msgs1 = repo1.observeMessages().first()
            val imgMsg = msgs1.first { it.mediaType == "image" }
            val vidMsg = msgs1.first { it.mediaType == "video" }
            assertEquals("user", imgMsg.sender)
            assertEquals("android.resource://com.elegan/drawable/img_cat_camisa", imgMsg.mediaUrl)
            assertEquals("Adjunto foto del producto", imgMsg.text)
            assertEquals("user", vidMsg.sender)
            assertEquals("android.resource://com.elegan/raw/demo_video", vidMsg.mediaUrl)

            // Simular recarga con una nueva instancia del repositorio
            val repo2 = EleganRepository(context)
            val msgs2 = repo2.observeMessages().first()
            assertTrue(msgs2.any { it.id == imgMsg.id && it.mediaType == "image" })
            assertTrue(msgs2.any { it.id == vidMsg.id && it.mediaType == "video" })

            limpiarPreferenciasPrueba(context, "elegan_support_messages")
            MobileSession.clear(context)
        }
    }
}
