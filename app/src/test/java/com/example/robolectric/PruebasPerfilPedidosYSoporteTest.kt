package com.example

import android.net.Uri
import com.example.data.model.ProductJson
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import com.example.data.repository.EleganRepository
import com.example.data.repository.RepositorioCarrito
import com.example.data.repository.RepositorioMedia
import com.example.data.repository.RepositorioPedidos
import com.example.data.repository.extractCapturedOrderIdJson
import com.example.data.repository.extractCartItemsJson
import com.example.data.repository.extractMessagesJson
import com.example.data.repository.extractOrdersJson
import com.example.data.repository.extractUploadedMediaUrl
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.MultipartBody
import okio.Buffer
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
    fun `test cart repository does not mutate locally on backend failures and syncs mongodb truth`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val serverCart = mutableListOf<Map<String, Any?>>()
        var failNextAdd = true
        var failNextUpdate = false
        var failNextRemove = false
        var failNextClear = false

        val fakeApi = object : OrdersFakeEleganAuthApi() {
            override suspend fun getCart(authorization: String): Map<String, Any?> =
                mapOf("items" to serverCart.toList())

            override suspend fun addToCart(authorization: String, request: Map<String, Any?>): Map<String, Any?> {
                if (failNextAdd) throw IllegalStateException("HTTP 503 Service Unavailable")
                val entry = mapOf(
                    "id" to "mongo_item_1",
                    "productId" to request["productId"],
                    "name" to request["name"],
                    "price" to request["price"],
                    "quantity" to request["quantity"],
                    "selectedSize" to request["selectedSize"],
                    "selectedColor" to request["selectedColor"]
                )
                serverCart.add(entry)
                return mapOf("items" to serverCart.toList())
            }

            override suspend fun updateCartItem(
                authorization: String,
                itemId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                if (failNextUpdate) throw IllegalStateException("HTTP 500 Update Error")
                val newQty = (request["quantity"] as Number).toLong()
                val idx = serverCart.indexOfFirst { it["id"] == itemId }
                if (idx >= 0) {
                    serverCart[idx] = serverCart[idx].toMutableMap().apply { put("quantity", newQty) }
                }
                return mapOf("items" to serverCart.toList())
            }

            override suspend fun removeCartItem(authorization: String, itemId: String): Map<String, Any?> {
                if (failNextRemove) throw IllegalStateException("HTTP 500 Delete Error")
                serverCart.removeAll { it["id"] == itemId }
                return mapOf("items" to serverCart.toList())
            }

            override suspend fun clearCart(authorization: String): Map<String, Any?> {
                if (failNextClear) throw IllegalStateException("HTTP 500 Clear Error")
                serverCart.clear()
                return mapOf("items" to emptyList<Any>())
            }
        }

        // Sin token en sesión real, no debe crear artículos falsos en memoria.
        val unauthCarritoRepo = RepositorioCarrito(
            appContext = context,
            apiProvider = { fakeApi },
            tokenProvider = { null }
        )
        val product: ProductJson = mapOf(
            "id" to "prod_sync_1",
            "name" to "Mocasín Italiano",
            "price" to 210.0,
            "imageUrl" to "https://cdn.example.com/mocasin.jpg"
        )
        val unauthAdd = runCatching { unauthCarritoRepo.addToCart(product, "42", 1L, "Negro") }
        assertTrue(unauthAdd.isFailure)
        assertTrue(unauthCarritoRepo.observeCart().first().isEmpty())

        val carritoRepo = RepositorioCarrito(
            appContext = context,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        // 1. Si falla addToCart en backend, Android NO debe guardar el artículo localmente.
        val failedAdd = runCatching { carritoRepo.addToCart(product, "42", 1L, "Negro") }
        assertTrue(failedAdd.isFailure)
        assertTrue("No debe existir carrito local si POST /api/movil/cart falló", carritoRepo.observeCart().first().isEmpty())
        assertNotNull(carritoRepo.observeCartError().first())

        // 2. Cuando el backend confirma, Android refleja exactamente el carrito de MongoDB.
        failNextAdd = false
        carritoRepo.addToCart(product, "42", 1L, "Negro")
        assertEquals(1, carritoRepo.observeCart().first().size)
        assertEquals("mongo_item_1", carritoRepo.observeCart().first().first().id)
        assertEquals(null, carritoRepo.observeCartError().first())

        // 3. Si falla updateCartQuantity, no debe quedar la cantidad falsa en Android.
        failNextUpdate = true
        val failedUpdate = runCatching { carritoRepo.updateCartQuantity("mongo_item_1", 5L) }
        assertTrue(failedUpdate.isFailure)
        assertEquals(1L, carritoRepo.observeCart().first().first().quantity)

        // 4. Si falla removeFromCart o clearCart, no debe desaparecer el artículo en Android mientras siga en MongoDB.
        failNextRemove = true
        val failedRemove = runCatching { carritoRepo.removeFromCart("mongo_item_1") }
        assertTrue(failedRemove.isFailure)
        assertEquals(1, carritoRepo.observeCart().first().size)

        failNextClear = true
        val failedClear = runCatching { carritoRepo.clearCart(syncRemote = true) }
        assertTrue(failedClear.isFailure)
        assertEquals(1, carritoRepo.observeCart().first().size)

        // 5. Si MongoDB queda vacío externamente y refreshCart falla o luego sincroniza, se refleja la colección cart real.
        failNextClear = false
        carritoRepo.clearCart(syncRemote = true)
        assertTrue(carritoRepo.observeCart().first().isEmpty())
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
    fun `test cart orders messages and media json extractors`() {
        val cartItems = extractCartItemsJson(
            mapOf(
                "items" to listOf(
                    mapOf(
                        "id" to "cart_1",
                        "product_id" to "prod_99",
                        "name" to "Mocasín Veneciano",
                        "price" to "185",
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
        assertEquals(185.0, cartItems.first().price, 0.01)
        assertEquals(2L, cartItems.first().quantity)
        assertEquals("42", cartItems.first().selectedSize)
        assertTrue(cartItems.first().imageUrl.endsWith("/uploads/mocasin.jpg"))

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
                        "payment_method" to "Transferencia bancaria",
                        "subtotal" to 3000.0,
                        "discount" to 200.0,
                        "shipping_cost" to 500.0,
                        "total" to 3300.0,
                        "item_count" to 2,
                        "status" to "pendiente_confirmacion",
                        "items" to listOf(
                            mapOf(
                                "productId" to "prod_99",
                                "name" to "Camisa Oxford",
                                "imageUrl" to "/uploads/camisa.jpg",
                                "price" to 1500.0,
                                "quantity" to 2,
                                "selectedSize" to "M",
                                "selectedColor" to "Blanco",
                                "shippingMethodName" to "Internacional",
                                "shippingPrice" to 500.0,
                                "shippingDeliveryTime" to "5-15 días",
                                "shippingOrigin" to "Estados Unidos"
                            )
                        )
                    )
                )
            ),
            fallbackUserId = "u1"
        )
        assertEquals(1, orders.size)
        val parsedOrder = orders.first()
        assertEquals("ORD_FINAL_900", parsedOrder.id)
        assertEquals("Transferencia bancaria", parsedOrder.paymentMethod)
        assertEquals(3000.0, parsedOrder.subtotal, 0.01)
        assertEquals(200.0, parsedOrder.discount, 0.01)
        assertEquals(500.0, parsedOrder.shippingCost, 0.01)
        assertEquals(3300.0, parsedOrder.total, 0.01)
        assertEquals("Internacional", parsedOrder.shippingMethodName)
        assertEquals("5-15 días", parsedOrder.shippingDeliveryTime)
        assertEquals(1, parsedOrder.items.size)
        val parsedItem = parsedOrder.items.first()
        assertEquals("Camisa Oxford", parsedItem.name)
        assertTrue(parsedItem.imageUrl.endsWith("/uploads/camisa.jpg"))
        assertEquals(1500.0, parsedItem.price, 0.01)
        assertEquals(2L, parsedItem.quantity)
        assertEquals("M", parsedItem.selectedSize)
        assertEquals("Blanco", parsedItem.selectedColor)
        assertEquals("5-15 días", parsedItem.shippingDeliveryTime)
        assertEquals("Estados Unidos", parsedItem.shippingOrigin)

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

    @Test
    fun `test media repository streams file content across multiple chunks without buffering full payload`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        MobileSession.clear(context)
        MobileSession.save(
            context,
            AuthResponse(
                token = "admin_backend_token",
                user = AuthUser(
                    id = "admin-001",
                    email = "admin@elegan.com",
                    name = "Admin Elegan",
                    role = "admin",
                    isAdmin = true
                )
            )
        )

        val tempVideo = File.createTempFile("elegan_video_stream_", ".mp4", context.cacheDir)
        try {
            // ~200 KB > buffer de 64 KB para probar escritura por bloques en streaming
            val payloadBytes = ByteArray(200 * 1024) { idx -> (idx % 251).toByte() }
            tempVideo.writeBytes(payloadBytes)

            var capturedContentLength = -1L
            var capturedDisposition: String? = null
            var streamedBytes = ByteArray(0)

            val fakeApi = object : OrdersFakeEleganAuthApi() {
                override suspend fun uploadMedia(
                    authorization: String,
                    file: MultipartBody.Part
                ): Map<String, Any?> {
                    assertEquals("Bearer admin_backend_token", authorization)
                    capturedDisposition = file.headers?.get("Content-Disposition")
                    capturedContentLength = file.body.contentLength()
                    val sink = Buffer()
                    file.body.writeTo(sink)
                    streamedBytes = sink.readByteArray()
                    return mapOf("url" to "/uploads/${tempVideo.name}")
                }
            }

            val mediaRepo = RepositorioMedia(
                appContext = context,
                apiProvider = { fakeApi }
            )
            val uploadedUrl = mediaRepo.uploadMedia(Uri.fromFile(tempVideo))

            assertEquals(payloadBytes.size.toLong(), capturedContentLength)
            assertTrue(capturedDisposition.orEmpty().contains(tempVideo.name))
            assertTrue(payloadBytes.contentEquals(streamedBytes))
            assertTrue(uploadedUrl.endsWith("/uploads/${tempVideo.name}"))
        } finally {
            tempVideo.delete()
            MobileSession.clear(context)
        }
    }
}

private open class OrdersFakeEleganAuthApi : BaseFakeEleganAuthApi()
