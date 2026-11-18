package com.example

import android.content.Intent
import android.media.MediaPlayer
import com.example.data.model.ProductJson
import com.example.data.model.announcementBadge
import com.example.data.model.announcementCategory
import com.example.data.model.announcementId
import com.example.data.model.announcementImageUrl
import com.example.data.model.announcementMessage
import com.example.data.model.announcementTitle
import com.example.data.model.productCategory
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.model.productMediaFormat
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productVideoUrl
import com.example.data.remote.AuthResponse
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.GoogleAuthRequest
import com.example.data.remote.InicioSesionRequest
import com.example.data.remote.PerfilActualizadoRespuesta
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilRespuesta
import com.example.data.remote.RegistroRequest
import com.example.data.remote.UsuarioRespuesta
import com.example.data.repository.EleganRepository
import com.example.data.repository.RepositorioCategorias
import com.example.data.repository.RepositorioEstadisticasPublicaciones
import com.example.data.repository.RepositorioGuardadosYLikes
import com.example.data.repository.extractAnnouncementsJson
import com.example.data.repository.extractCategoriesJson
import com.example.data.repository.extractJsonItems
import com.example.data.repository.extractSavedProductIdsJson
import com.example.ui.DespachadorCompartidoPublicacion
import com.example.ui.ReceptorCompartidoPublicacion
import com.example.ui.components.EstadoReproductorVideo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MultipartBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer

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
    fun `test announcements and saved products json extractors handle backend formats`() {
        val announcementsPayload = mapOf(
            "announcements" to listOf(
                mapOf(
                    "id" to "ann_1",
                    "title" to "Colección Otoño",
                    "message" to "20% en sastrería italiana",
                    "image_url" to "/uploads/otono.jpg",
                    "category" to "camisa",
                    "badge" to "NUEVO",
                    "active" to true
                ),
                mapOf(
                    "id" to "ann_inactive",
                    "title" to "Oculto",
                    "message" to "No debe mostrarse",
                    "active" to false
                )
            )
        )

        val announcements = extractAnnouncementsJson(announcementsPayload)
        assertEquals(1, announcements.size)
        assertEquals("ann_1", announcements.first().announcementId())
        assertEquals("Colección Otoño", announcements.first().announcementTitle())
        assertEquals("20% en sastrería italiana", announcements.first().announcementMessage())
        assertEquals("camisa", announcements.first().announcementCategory())
        assertEquals("NUEVO", announcements.first().announcementBadge())
        assertTrue(announcements.first().announcementImageUrl().endsWith("/uploads/otono.jpg"))

        val savedFromIds = extractSavedProductIdsJson(
            mapOf("product_ids" to listOf("p1", "p2"))
        )
        assertEquals(setOf("p1", "p2"), savedFromIds)

        val savedFromItems = extractSavedProductIdsJson(
            mapOf("items" to listOf(mapOf("product_id" to "p3"), mapOf("id" to "p4")))
        )
        assertEquals(setOf("p3", "p4"), savedFromItems)
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

    @Test
    fun `test video player state avoids pause or start calls when not prepared or disposed`() {
        val mp = MediaPlayer()
        val shadow = shadowOf(mp)
        val state = EstadoReproductorVideo(initialAutoPlay = false, initialFocused = false)
        state.mediaPlayerRef = mp

        // En estado IDLE/PREPARING (isPrepared = false, hasStarted = false) no debe llamar pause/start/seekTo
        state.pausarReproduccion()
        state.iniciarReproduccion()
        state.repetirVideo()
        assertEquals(ShadowMediaPlayer.State.IDLE, shadow.state)

        // Incluso marcado como preparado pero sin haber iniciado, pausar no debe invocar mp.pause()
        state.isPrepared = true
        state.hasStarted = false
        state.pausarSiEsPosible()
        assertEquals(ShadowMediaPlayer.State.IDLE, shadow.state)

        // Con error o dispuesto, ninguna operación debe invocar al MediaPlayer nativo
        state.hasError = true
        state.hasStarted = true
        state.pausarSiEsPosible()
        state.iniciarSiPreparado()
        assertEquals(ShadowMediaPlayer.State.IDLE, shadow.state)

        mp.release()
    }

    @Test
    fun `test categories repository starts empty without local fallback and loads once`(): Unit = runBlocking {
        var fetchCalls = 0
        val repo = RepositorioCategorias(
            fetchRemotePayload = {
                fetchCalls++
                mapOf(
                    "items" to listOf(
                        mapOf(
                            "id" to "camisa",
                            "title" to "Camisas & Trajes",
                            "subtitle" to "Sastrería italiana",
                            "iconName" to "dry_cleaning",
                            "active" to true
                        ),
                        mapOf(
                            "id" to "oculta",
                            "title" to "Inactiva",
                            "active" to false
                        )
                    )
                )
            }
        )

        assertTrue(
            "Las categorías deben iniciar vacías sin categorías locales ni de respaldo",
            repo.observeCategories().first().isEmpty()
        )

        val firstLoad = repo.loadCategoriesOnce()
        assertEquals(1, fetchCalls)
        assertEquals(1, firstLoad.size)
        assertEquals("camisa", firstLoad.first().id)
        assertEquals("Camisas & Trajes", firstLoad.first().title)

        // Navegar entre pantallas / llamadas repetidas no debe duplicar la consulta
        val secondLoad = repo.loadCategoriesOnce()
        assertEquals(1, fetchCalls)
        assertEquals(firstLoad, secondLoad)
    }

    @Test
    fun `test categories repository preserves valid list during failed or invalid refresh`(): Unit = runBlocking {
        var step = 0
        val repo = RepositorioCategorias(
            fetchRemotePayload = {
                step++
                when (step) {
                    1 -> mapOf(
                        "categories" to listOf(
                            mapOf("id" to "gorra", "name" to "Gorras & Boinas")
                        )
                    )
                    2 -> throw RuntimeException("Timeout de red")
                    3 -> mapOf("error" to "bad_gateway")
                    else -> listOf(
                        mapOf("id" to "gorra", "name" to "Gorras & Boinas"),
                        mapOf("id" to "zapato", "name" to "Zapatos Oxford")
                    )
                }
            }
        )

        repo.loadCategoriesOnce()
        assertEquals(listOf("gorra"), repo.observeCategories().first().map { it.id })

        // Error de red: conserva la lista compartida
        repo.refreshCategories()
        assertEquals(listOf("gorra"), repo.observeCategories().first().map { it.id })

        // Respuesta inválida sin lista de categorías: conserva la lista compartida
        repo.refreshCategories()
        assertEquals(listOf("gorra"), repo.observeCategories().first().map { it.id })

        // Respuesta válida del backend: actualiza la lista
        repo.refreshCategories()
        assertEquals(listOf("gorra", "zapato"), repo.observeCategories().first().map { it.id })
    }

    @Test
    fun `test categories json extractor handles backend formats and rejects invalid payloads`() {
        assertEquals(null, extractCategoriesJson(null))
        assertEquals(null, extractCategoriesJson(mapOf("status" to "ok")))

        val fromData = extractCategoriesJson(
            mapOf(
                "data" to listOf(
                    mapOf(
                        "slug" to "perfume",
                        "label" to "Perfumes",
                        "description" to "Fragancias",
                        "image_url" to "/uploads/perfume.jpg",
                        "sortOrder" to 2
                    ),
                    mapOf(
                        "id" to "gafa",
                        "title" to "Gafas",
                        "sortOrder" to 1
                    )
                )
            )
        )
        assertEquals(listOf("gafa", "perfume"), fromData?.map { it.id })
        assertFalse("No debe inventar la categoría 'todo' si el backend no la envía", fromData.orEmpty().any { it.id.equals("todo", ignoreCase = true) })
        assertTrue(fromData?.last()?.imageUrl?.endsWith("/uploads/perfume.jpg") == true)
    }

    @Test
    fun `test refreshCategories forces backend query after initial load without inventing todo`(): Unit = runBlocking {
        var calls = 0
        val repo = RepositorioCategorias(
            fetchRemotePayload = {
                calls++
                when (calls) {
                    1 -> mapOf("items" to listOf(mapOf("id" to "camisa", "name" to "Camisas")))
                    else -> mapOf(
                        "items" to listOf(
                            mapOf("id" to "camisa", "name" to "Camisas"),
                            mapOf("id" to "correa", "name" to "Correas")
                        )
                    )
                }
            }
        )

        repo.loadCategoriesOnce()
        repo.loadCategoriesOnce()
        assertEquals(1, calls)

        val refreshed = repo.refreshCategories()
        assertEquals(2, calls)
        assertEquals(listOf("camisa", "correa"), refreshed.map { it.id })
        assertFalse(refreshed.any { it.id.equals("todo", ignoreCase = true) })
    }

    @Test
    fun `test categories preserve exact backend ids omit missing ids and keep case distinctness`() {
        val extracted = extractCategoriesJson(
            mapOf(
                "items" to listOf(
                    mapOf("title" to "Sin Identificador", "subtitle" to "Debe omitirse"),
                    mapOf("id" to "Camisa", "title" to "Camisas Mayúscula"),
                    mapOf("id" to "camisa", "title" to "Camisas Minúscula"),
                    mapOf("id" to "Camisa", "title" to "Duplicado Exacto")
                )
            )
        )

        assertEquals(listOf("Camisa", "camisa"), extracted?.map { it.id })
        assertEquals(listOf("Camisas Mayúscula", "Camisas Minúscula"), extracted?.map { it.title })

        val productWithoutCategory: ProductJson = mapOf("id" to "p_no_cat", "name" to "Traje")
        assertEquals("", productWithoutCategory.productCategory())
    }

    @Test
    fun `test setProductSaved reverts state and throws when backend request fails and syncs global count on success`() = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        var shouldFailSave = true
        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun setSaved(
                authorization: String,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                if (shouldFailSave) throw RuntimeException("HTTP 500")
                return mapOf("saved" to true, "savesCount" to 12)
            }
        }
        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        var failureThrown = false
        try {
            repo.setProductSaved("prod_1", true)
        } catch (_: RuntimeException) {
            failureThrown = true
        }
        assertTrue("El error del servidor debe propagarse al controlador", failureThrown)
        assertFalse("Si falla el backend, no debe quedar guardado localmente", "prod_1" in repo.observeSavedProductIds().first())
        assertEquals(0L, repo.observePublicationStats().first()["prod_1"]?.savesCount ?: 0L)

        shouldFailSave = false
        val confirmed = repo.setProductSaved("prod_1", true)
        assertTrue(confirmed)
        assertTrue("prod_1" in repo.observeSavedProductIds().first())
        assertEquals(12L, repo.observePublicationStats().first()["prod_1"]?.savesCount)
    }

    @Test
    fun `test recordPublicationShare calls backend syncs global count and preserves confirmed count on error`() = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        var shareCalls = 0
        var shouldFailShare = true
        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun recordShare(
                authorization: String?,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                shareCalls++
                if (shouldFailShare) throw RuntimeException("HTTP 503")
                return mapOf("sharesCount" to 9)
            }
        }
        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        val fallbackCount = repo.recordPublicationShare("prod_share_1")
        assertEquals(0L, fallbackCount)
        assertEquals(1, shareCalls)
        assertEquals(0L, repo.observePublicationStats().first()["prod_share_1"]?.sharesCount ?: 0L)

        shouldFailShare = false
        val updatedCount = repo.recordPublicationShare("prod_share_1")
        assertEquals(2, shareCalls)
        assertEquals(9L, updatedCount)
        assertEquals(9L, repo.observePublicationStats().first()["prod_share_1"]?.sharesCount)
    }

    @Test
    fun `test visitor share sends visitorId without auth fetches public counts and flushes after login when 401`() = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        var activeToken: String? = null
        var requireAuthForShares = false
        var globalShares = 4L
        val recordedCalls = mutableListOf<Pair<String?, Map<String, Any?>>>()

        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun getShareCounts(
                authorization: String?,
                productIds: List<String>
            ): Map<String, Any?> = mapOf("counts" to mapOf("prod_visitor" to globalShares))

            override suspend fun recordShare(
                authorization: String?,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                recordedCalls.add(authorization to request)
                if (requireAuthForShares && authorization.isNullOrBlank()) {
                    throw IllegalStateException("HTTP 401 Unauthorized")
                }
                globalShares++
                return mapOf("sharesCount" to globalShares)
            }
        }

        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { activeToken }
        )

        // 1. Visitante consulta conteos públicos y comparte cuando el backend permite visitorId sin sesión.
        repo.refreshPublicationShareCounts(listOf("prod_visitor"))
        assertEquals(4L, repo.observePublicationStats().first()["prod_visitor"]?.sharesCount)

        val visitorCount = repo.recordPublicationShare("prod_visitor")
        assertEquals(5L, visitorCount)
        assertEquals(null, recordedCalls.first().first)
        assertEquals(true, recordedCalls.first().second["isGuest"])
        assertFalse((recordedCalls.first().second["visitorId"] as? String).isNullOrBlank())

        // 2. Si el backend exige autenticación (HTTP 401), compartir sigue sin error y se sincroniza al iniciar sesión.
        requireAuthForShares = true
        val keptCount = repo.recordPublicationShare("prod_visitor")
        assertEquals(5L, keptCount)

        activeToken = "logged_in_user_token"
        repo.refreshSavedProducts()
        assertEquals(6L, repo.observePublicationStats().first()["prod_visitor"]?.sharesCount)
        assertEquals("Bearer logged_in_user_token", recordedCalls.last().first)
    }

    @Test
    fun `test refreshPublicationCounts and product json sync global likes saves shares and comments`() = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun getLikeCounts(authorization: String?, productIds: List<String>): Map<String, Any?> =
                mapOf("counts" to mapOf("prod_global" to 15))

            override suspend fun getSavedCounts(authorization: String?, productIds: List<String>): Map<String, Any?> =
                mapOf("counts" to mapOf("prod_global" to 7))

            override suspend fun getShareCounts(authorization: String?, productIds: List<String>): Map<String, Any?> =
                mapOf("counts" to mapOf("prod_global" to 4))
        }
        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        repo.syncCountsFromProducts(
            listOf(
                mapOf(
                    "id" to "prod_global",
                    "name" to "Abrigo Cachemira",
                    "commentsCount" to 3
                )
            )
        )
        repo.refreshPublicationCounts(listOf("prod_global"))

        val stats = repo.observePublicationStats().first()["prod_global"]
        assertEquals(15L, stats?.likesCount)
        assertEquals(7L, stats?.savesCount)
        assertEquals(4L, stats?.sharesCount)
        assertEquals(3L, stats?.commentsCount)
    }

    @Test
    fun `test visitor without session fetches global likes saves and shares counts`() = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        val authHeadersSeen = mutableListOf<String?>()
        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun getLikeCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                authHeadersSeen.add(authorization)
                return mapOf("counts" to mapOf("prod_pub_1" to 22))
            }

            override suspend fun getSavedCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                authHeadersSeen.add(authorization)
                return mapOf("counts" to mapOf("prod_pub_1" to 11))
            }

            override suspend fun getShareCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                authHeadersSeen.add(authorization)
                return mapOf("counts" to mapOf("prod_pub_1" to 6))
            }
        }
        val visitorRepo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { null }
        )

        visitorRepo.refreshPublicationCounts(listOf("prod_pub_1"))

        val stats = visitorRepo.observePublicationStats().first()["prod_pub_1"]
        assertEquals(listOf<String?>(null, null, null), authHeadersSeen)
        assertEquals(22L, stats?.likesCount)
        assertEquals(11L, stats?.savesCount)
        assertEquals(6L, stats?.sharesCount)
    }

    @Test
    fun `test share confirmation broadcast dispatches event only when chooser target is selected`() = runBlocking {
        val context = obtenerContextoPrueba()
        val receiver = ReceptorCompartidoPublicacion()
        val deferred = async {
            withTimeout(2000L) {
                DespachadorCompartidoPublicacion.confirmaciones.first()
            }
        }
        kotlinx.coroutines.yield()

        receiver.onReceive(
            context,
            Intent(context, ReceptorCompartidoPublicacion::class.java).apply {
                putExtra(ReceptorCompartidoPublicacion.EXTRA_PRODUCT_ID, "prod_shared_target")
                putExtra(ReceptorCompartidoPublicacion.EXTRA_SHARE_REQUEST_ID, "req_123")
            }
        )

        val event = deferred.await()
        assertEquals("prod_shared_target", event.productId)
        assertEquals("req_123", event.requestId)
    }
}

private open class StubEleganAuthApi : EleganAuthApi {
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
    override suspend fun getSavedCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun getLikes(authorization: String): Map<String, Any?> = emptyMap()
    override suspend fun setLike(authorization: String, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getLikeCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
    override suspend fun recordShare(authorization: String?, productId: String, request: Map<String, Any?>): Map<String, Any?> = emptyMap()
    override suspend fun getShareCounts(authorization: String?, productIds: List<String>): Map<String, Any?> = emptyMap()
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
