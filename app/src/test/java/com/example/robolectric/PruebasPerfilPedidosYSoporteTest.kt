package com.example

import android.net.Uri
import com.example.data.model.ProductJson
import com.example.data.model.productName
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import com.example.data.repository.EleganRepository
import com.example.data.repository.extractCapturedOrderIdJson
import com.example.data.repository.extractCartItemsJson
import com.example.data.repository.extractMessagesJson
import com.example.data.repository.extractOrdersJson
import com.example.data.repository.extractPayPalSessionJson
import com.example.data.repository.extractUploadedMediaUrl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
        val backendProduct: ProductJson = mapOf(
            "id" to "backend_prod_order_1",
            "name" to "Camisa Oxford Blanca",
            "category" to "camisa",
            "price" to 89.0,
            "imageUrl" to "https://cdn.example.com/camisa.jpg"
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
            itemsSummary = "2x ${backendProduct.productName()}"
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

    @Test
    fun `test cart paypal orders messages and media json extractors`() {
        val cartItems = extractCartItemsJson(
            mapOf(
                "items" to listOf(
                    mapOf(
                        "id" to "cart_1",
                        "product_id" to "prod_99",
                        "name" to "Mocasín Veneciano",
                        "price" to "185.50",
                        "quantity" to 2,
                        "selected_size" to "42",
                        "selected_color" to "Tabaco",
                        "image_url" to "/uploads/mocasin.jpg"
                    )
                )
            ),
            fallbackUserId = "u1"
        )
        assertEquals(1, cartItems.size)
        assertEquals("cart_1", cartItems.first().id)
        assertEquals("prod_99", cartItems.first().productId)
        assertEquals(185.5, cartItems.first().price, 0.01)
        assertEquals(2L, cartItems.first().quantity)
        assertEquals("42", cartItems.first().selectedSize)
        assertTrue(cartItems.first().imageUrl.endsWith("/uploads/mocasin.jpg"))

        val paypalSession = extractPayPalSessionJson(
            mapOf(
                "id" to "PAYPAL_ORD_123",
                "status" to "CREATED",
                "links" to listOf(
                    mapOf("rel" to "self", "href" to "https://api.paypal.com/self"),
                    mapOf("rel" to "approve", "href" to "https://www.paypal.com/checkoutnow?token=PAYPAL_ORD_123")
                )
            )
        )
        assertNotNull(paypalSession)
        assertEquals("PAYPAL_ORD_123", paypalSession?.paypalOrderId)
        assertEquals("https://www.paypal.com/checkoutnow?token=PAYPAL_ORD_123", paypalSession?.approveUrl)

        val capturedId = extractCapturedOrderIdJson(
            mapOf("order" to mapOf("order_id" to "ORD_FINAL_900"))
        )
        assertEquals("ORD_FINAL_900", capturedId)

        val orders = extractOrdersJson(
            mapOf(
                "orders" to listOf(
                    mapOf(
                        "id" to "ORD_FINAL_900",
                        "customer_name" to "Carlos Méndez",
                        "payment_method" to "PayPal",
                        "total" to 371.0,
                        "item_count" to 2,
                        "status" to "pagado"
                    )
                )
            ),
            fallbackUserId = "u1"
        )
        assertEquals(1, orders.size)
        assertEquals("ORD_FINAL_900", orders.first().id)
        assertEquals("PayPal", orders.first().paymentMethod)
        assertEquals(371.0, orders.first().total, 0.01)

        val messages = extractMessagesJson(
            mapOf(
                "messages" to listOf(
                    mapOf(
                        "id" to "m1",
                        "role" to "admin",
                        "message" to "Tu pedido ha sido confirmado",
                        "media_url" to "/uploads/guia.jpg",
                        "media_type" to "image"
                    )
                )
            ),
            fallbackUserId = "u1"
        )
        assertEquals(1, messages.size)
        assertEquals("support", messages.first().sender)
        assertTrue(messages.first().mediaUrl.endsWith("/uploads/guia.jpg"))

        val mediaUrl = extractUploadedMediaUrl(
            mapOf("media" to mapOf("url" to "/uploads/foto_admin.jpg"))
        )
        assertNotNull(mediaUrl)
        assertTrue(mediaUrl.orEmpty().endsWith("/uploads/foto_admin.jpg"))
    }

    @Test
    fun `test media upload permissions allow admin or superadmin and reject customer`(): Unit = runBlocking {
        assertFalse(AuthUser(role = "customer").canUploadMedia)
        assertTrue(AuthUser(role = "admin").canUploadMedia)
        assertTrue(AuthUser(role = "superadmin").canUploadMedia)
        assertTrue(AuthUser(role = "customer", isAdmin = true).canUploadMedia)

        val context = obtenerContextoPrueba()
        reiniciarSesionClienteDemo(context)

        val repo = EleganRepository(context)
        val result = runCatching {
            repo.uploadMedia(Uri.parse("content://media/external/images/1"))
        }
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)

        MobileSession.clear(context)
    }
}
