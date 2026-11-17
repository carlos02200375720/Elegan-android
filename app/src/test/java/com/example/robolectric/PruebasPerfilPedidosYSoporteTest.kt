package com.example

import com.example.data.model.Product
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
    fun `test profile update is not treated as saved when backend request fails`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo1 = EleganRepository(context)
        val saveResult = runCatching {
            repo1.saveUserProfile(
                name = "Cliente Demo",
                email = "cliente.demo@example.com",
                phone = "809-555-2026",
                address = "Av. Abraham Lincoln 450",
                city = "Santo Domingo"
            )
        }
        assertTrue("El guardado debe fallar si el backend no confirma la operación", saveResult.isFailure)

        MobileSession.clear(context)
    }

    @Test
    fun `test created order clears cart and appears in session order history without local cache`() = runBlocking {
        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo = EleganRepository(context)
        val backendProduct = Product(
            id = "backend_prod_order_1",
            name = "Camisa Oxford Blanca",
            category = "camisa",
            price = 89.0,
            imageUrl = "https://cdn.example.com/camisa.jpg"
        )
        repo.addToCart(backendProduct, selectedSize = "M", quantity = 2L, selectedColor = "Negro")

        val orderId = repo.createOrder(
            customerName = "Cliente Elegan",
            email = "cliente.elegan@gmail.com",
            phone = "809-555-0199",
            address = "Av. Winston Churchill 101",
            city = "Santo Domingo",
            paymentMethod = "Tarjeta",
            total = 110.0,
            itemCount = 2L,
            itemsSummary = "2x ${backendProduct.name}"
        )

        val orders = repo.observeOrders().first()
        val cart = repo.observeCart().first()
        assertEquals(1, orders.size)
        assertEquals(orderId, orders.first().id)
        assertEquals("Cliente Elegan", orders.first().customerName)
        assertEquals(110.0, orders.first().total, 0.01)
        assertTrue(cart.isEmpty())

        // Sin almacenamiento local persistente entre instancias
        val repoFresh = EleganRepository(context)
        assertTrue(repoFresh.observeOrders().first().isEmpty())

        MobileSession.clear(context)
    }

    @Test
    fun `test support chat sends image and video messages in memory`() {
        runBlocking {
            val context = obtenerContextoPrueba()
            reiniciarSesionClienteDemo(context)

            val repo = EleganRepository(context)
            repo.sendSupportMessage(
                text = "Adjunto foto del producto",
                mediaUri = "https://cdn.example.com/camisa.jpg",
                mediaType = "image",
                scope = this
            )
            repo.sendSupportMessage(
                text = "",
                mediaUri = "https://cdn.example.com/demo_video.mp4",
                mediaType = "video",
                scope = this
            )

            val msgs = repo.observeMessages().first()
            val imgMsg = msgs.first { it.mediaType == "image" }
            val vidMsg = msgs.first { it.mediaType == "video" }
            assertEquals("user", imgMsg.sender)
            assertEquals("https://cdn.example.com/camisa.jpg", imgMsg.mediaUrl)
            assertEquals("Adjunto foto del producto", imgMsg.text)
            assertEquals("user", vidMsg.sender)
            assertEquals("https://cdn.example.com/demo_video.mp4", vidMsg.mediaUrl)

            MobileSession.clear(context)
        }
    }
}
