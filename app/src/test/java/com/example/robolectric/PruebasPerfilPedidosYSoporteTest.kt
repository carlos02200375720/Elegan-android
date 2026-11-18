package com.example

import android.net.Uri
import com.example.data.model.ProductJson
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.GoogleAuthRequest
import com.example.data.remote.InicioSesionRequest
import com.example.data.remote.MobileSession
import com.example.data.remote.PerfilActualizadoRespuesta
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilRespuesta
import com.example.data.remote.RegistroRequest
import com.example.data.remote.UsuarioRespuesta
import com.example.data.repository.EleganRepository
import com.example.data.repository.RepositorioCarrito
import com.example.data.repository.RepositorioMedia
import com.example.data.repository.RepositorioPedidos
import com.example.data.repository.extractCapturedOrderIdJson
import com.example.data.repository.extractCartItemsJson
import com.example.data.repository.extractMessagesJson
import com.example.data.repository.extractOrdersJson
import com.example.data.repository.extractPayPalSessionJson
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
    fun `test paypal order capture persists backend order clears cart and does not confirm locally on failure`() = runBlocking {
        val context = obtenerContextoPrueba()
        val carritoRepo = RepositorioCarrito(context)
        val backendProduct: ProductJson = mapOf(
            "id" to "backend_prod_order_1",
            "name" to "Camisa Oxford Blanca",
            "category" to "camisa",
            "price" to 89.0,
            "imageUrl" to "https://cdn.example.com/camisa.jpg"
        )
        carritoRepo.addToCart(backendProduct, selectedSize = "M", quantity = 2L, selectedColor = "Negro")

        var shouldFailCapture = true
        val remoteOrders = mutableListOf<Map<String, Any?>>()
        val fakeApi = object : OrdersFakeEleganAuthApi() {
            override suspend fun createPayPalOrder(
                authorization: String,
                request: Map<String, Any?>
            ): Map<String, Any?> = mapOf(
                "paypalOrderId" to "PAYPAL_TEST_100",
                "approveUrl" to "https://www.paypal.com/checkoutnow?token=PAYPAL_TEST_100",
                "status" to "CREATED"
            )

            override suspend fun capturePayPalOrder(
                authorization: String,
                paypalOrderId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                if (shouldFailCapture) {
                    throw IllegalStateException("HTTP 410 Gone")
                }
                val orderMap = mapOf(
                    "id" to "ORD_MONGO_100",
                    "customerName" to "Cliente Elegan",
                    "paymentMethod" to "PayPal",
                    "total" to 178.0,
                    "itemCount" to 2L,
                    "status" to "pagado"
                )
                remoteOrders.add(orderMap)
                return mapOf("order" to orderMap)
            }

            override suspend fun getOrders(authorization: String): Map<String, Any?> =
                mapOf("orders" to remoteOrders.toList())
        }

        val pedidosRepo = RepositorioPedidos(
            appContext = context,
            carritoRepo = carritoRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        val session = pedidosRepo.createPayPalOrder(
            customerName = "Cliente Elegan",
            email = "cliente.elegan@gmail.com",
            phone = "809-555-0199",
            address = "Av. Winston Churchill 101",
            city = "Santo Domingo",
            total = 178.0,
            itemCount = 2L,
            itemsSummary = "2x Camisa Oxford Blanca",
            cartItems = carritoRepo.observeCart().first()
        )
        assertEquals("PAYPAL_TEST_100", session.paypalOrderId)

        val failedCapture = runCatching {
            pedidosRepo.capturePayPalOrder(
                paypalOrderId = session.paypalOrderId,
                customerName = "Cliente Elegan",
                email = "cliente.elegan@gmail.com",
                phone = "809-555-0199",
                address = "Av. Winston Churchill 101",
                city = "Santo Domingo",
                total = 178.0,
                itemCount = 2L,
                itemsSummary = "2x Camisa Oxford Blanca"
            )
        }
        assertTrue("Si falla el backend no debe confirmarse ninguna orden local", failedCapture.isFailure)
        assertTrue("No debe haber órdenes en memoria tras error del servidor", pedidosRepo.observeOrders().first().isEmpty())
        assertFalse("El carrito no debe vaciarse si el pago falló", carritoRepo.observeCart().first().isEmpty())

        shouldFailCapture = false
        val confirmedOrderId = pedidosRepo.capturePayPalOrder(
            paypalOrderId = session.paypalOrderId,
            customerName = "Cliente Elegan",
            email = "cliente.elegan@gmail.com",
            phone = "809-555-0199",
            address = "Av. Winston Churchill 101",
            city = "Santo Domingo",
            total = 178.0,
            itemCount = 2L,
            itemsSummary = "2x Camisa Oxford Blanca"
        )

        val orders = pedidosRepo.observeOrders().first()
        assertEquals("ORD_MONGO_100", confirmedOrderId)
        assertEquals(1, orders.size)
        assertEquals("ORD_MONGO_100", orders.first().id)
        assertEquals("PayPal", orders.first().paymentMethod)
        assertEquals(178.0, orders.first().total, 0.01)
        assertTrue(carritoRepo.observeCart().first().isEmpty())
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
    fun `test paypal order creation syncs cart from mongodb and rejects stale local cart on CART_EMPTY`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val serverCart = mutableListOf(
            mapOf<String, Any?>(
                "id" to "mongo_cart_1",
                "productId" to "prod_paypal_1",
                "name" to "Corbata de Seda",
                "price" to 65.0,
                "quantity" to 1L,
                "selectedSize" to "Única",
                "selectedColor" to "Burdeos"
            )
        )
        var payPalCalls = 0
        val fakeApi = object : OrdersFakeEleganAuthApi() {
            override suspend fun getCart(authorization: String): Map<String, Any?> =
                mapOf("items" to serverCart.toList())

            override suspend fun createPayPalOrder(
                authorization: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                payPalCalls++
                if (serverCart.isEmpty()) {
                    return mapOf("success" to false, "code" to "CART_EMPTY", "message" to "Carrito vacío")
                }
                return mapOf(
                    "paypalOrderId" to "PP_SYNCED_1",
                    "approveUrl" to "https://www.paypal.com/checkoutnow?token=PP_SYNCED_1"
                )
            }
        }

        val carritoRepo = RepositorioCarrito(
            appContext = context,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )
        val pedidosRepo = RepositorioPedidos(
            appContext = context,
            carritoRepo = carritoRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        carritoRepo.refreshCart()
        assertEquals(1, carritoRepo.observeCart().first().size)

        // Simulamos que MongoDB fue vaciado en el backend mientras Android aún mostraba 1 artículo.
        serverCart.clear()
        val staleCartBeforeCheckout = carritoRepo.observeCart().first()
        val payPalAttempt = runCatching {
            pedidosRepo.createPayPalOrder(
                customerName = "Cliente Elegan",
                email = "cliente@elegan.com",
                phone = "809-555-0100",
                address = "Av. Lincoln",
                city = "Santo Domingo",
                total = 65.0,
                itemCount = 1L,
                itemsSummary = "1x Corbata de Seda",
                cartItems = staleCartBeforeCheckout
            )
        }

        assertTrue("Debe fallar si MongoDB tiene el carrito vacío", payPalAttempt.isFailure)
        assertEquals("No debe invocar createPayPalOrder con carrito vacío en servidor", 0, payPalCalls)
        assertTrue("El carrito en Android debe quedar sincronizado como vacío", carritoRepo.observeCart().first().isEmpty())
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

private open class OrdersFakeEleganAuthApi : EleganAuthApi {
    override suspend fun register(request: RegistroRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun login(request: InicioSesionRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun loginGoogle(request: GoogleAuthRequest): AuthResponse = throw UnsupportedOperationException()
    override suspend fun me(authorization: String): UsuarioRespuesta = throw UnsupportedOperationException()
    override suspend fun logout(authorization: String) = Unit
    override suspend fun products(): Map<String, Any?> = emptyMap()
    override suspend fun productById(productId: String): Map<String, Any?> = emptyMap()
    override suspend fun categories(): Map<String, Any?> = emptyMap()
    override suspend fun announcements(): Map<String, Any?> = emptyMap()
    override suspend fun getCart(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun addToCart(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun updateCartItem(authorization: String, itemId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun removeCartItem(authorization: String, itemId: String): Map<String, Any?> = emptyMap()
    override suspend fun clearCart(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun getSaved(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun setSaved(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getSavedCounts(authorization: String, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun getLikes(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun setLike(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getLikeCounts(authorization: String, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun recordShare(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getShareCounts(authorization: String, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun getPublicationComments(authorization: String, productId: String): Map<String, Any?> = emptyMap()
    override suspend fun addPublicationComment(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun profile(authorization: String): PerfilRespuesta = throw UnsupportedOperationException()
    override suspend fun updateProfile(authorization: String, request: PerfilActualizarRequest): PerfilActualizadoRespuesta = throw UnsupportedOperationException()
    override suspend fun getOrders(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun createPayPalOrder(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun capturePayPalOrder(authorization: String, paypalOrderId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getMessages(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun sendMessage(authorization: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun uploadMedia(authorization: String, file: MultipartBody.Part): Map<String, Any?> = emptyMap()
}
