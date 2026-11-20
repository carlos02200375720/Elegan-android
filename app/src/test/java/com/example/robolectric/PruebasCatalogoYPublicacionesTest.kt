package com.example

import android.content.Intent
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.example.data.local.EleganDatabase
import com.example.data.local.PendingVisitorShareEntity
import com.example.data.model.AnnouncementDestination
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.announcementBadge
import com.example.data.model.announcementCategory
import com.example.data.model.announcementDestination
import com.example.data.model.announcementDestinationId
import com.example.data.model.announcementDestinationType
import com.example.data.model.announcementId
import com.example.data.model.announcementImageUrl
import com.example.data.model.announcementMessage
import com.example.data.model.announcementTitle
import com.example.data.model.productCategory
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.model.productMediaFormat
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productVideoUrl
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
import com.example.ui.components.sanitizarUrlVideoParaLog
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException
import java.util.UUID
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
    fun `test video player state safely tracks play pause and replay requests`() {
        val state = EstadoReproductorVideo(initialAutoPlay = false, initialFocused = false)

        state.pausarReproduccion()
        assertFalse(state.isPlaying)

        state.iniciarReproduccion()
        assertTrue(state.isPlaying)
        assertTrue(state.shouldLoadPlayer)

        state.pausarReproduccion()
        assertFalse(state.isPlaying)

        state.repetirVideo()
        assertTrue(state.isPlaying)
        assertFalse(state.isCompleted)
    }

    @Test
    fun `test video player state does not autoplay when initialFocused is true and autoPlay is false`() {
        val state = EstadoReproductorVideo(initialAutoPlay = false, initialFocused = true)
        assertFalse(state.isPlaying)
        assertFalse(state.shouldLoadPlayer)

        state.iniciarReproduccion()
        assertTrue(state.isPlaying)
        assertTrue(state.shouldLoadPlayer)
    }

    @Test
    fun `test video player pauses cleanly on background lifecycle and sanitizes log url`() {
        val state = EstadoReproductorVideo(initialAutoPlay = false, initialFocused = true)
        state.iniciarReproduccion()
        state.hasStarted = true
        assertTrue(state.isPlaying)
        assertFalse(state.showOverlayControls)

        state.pausarPorSegundoPlano()

        assertFalse(state.isPlaying)
        assertFalse(state.hasStarted)
        assertTrue(state.showOverlayControls)
        assertEquals(
            "https://cdn.elegan.com/videos/traje.mp4",
            sanitizarUrlVideoParaLog("https://cdn.elegan.com/videos/traje.mp4?token=secreto#t=5")
        )
    }

    @Test
    fun `test media url resolver allows only https and relative backend paths`() {
        assertEquals(
            "https://cdn.elegan.com/img/traje.jpg",
            com.example.data.model.resolveBackendMediaUrl("https://cdn.elegan.com/img/traje.jpg")
        )
        assertEquals(
            "https://cdn.elegan.com/img/traje.jpg",
            com.example.data.model.resolveBackendMediaUrl("//cdn.elegan.com/img/traje.jpg")
        )
        assertTrue(
            com.example.data.model.resolveBackendMediaUrl("/uploads/traje.jpg")
                .startsWith("https://")
        )
        assertEquals("", com.example.data.model.resolveBackendMediaUrl("http://cdn.elegan.com/img/traje.jpg"))
        assertEquals("", com.example.data.model.resolveBackendMediaUrl("javascript:alert(1)"))
        assertEquals("", com.example.data.model.resolveBackendMediaUrl("file:///etc/passwd"))
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
        assertEquals(true, recordedCalls.first().second["fromGuestSession"])
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

    @Test
    fun `test visitor pending share persists in Room across repository restart and retries without token reusing exact eventId`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val dbName = "elegan_test_shares_${UUID.randomUUID()}.db"
        var isOffline = true
        val requestsSent = mutableListOf<Triple<String?, String, Map<String, Any?>>>()

        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun recordShare(
                authorization: String?,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                requestsSent.add(Triple(authorization, productId, request))
                if (isOffline) throw IOException("Sin conexión")
                return mapOf("sharesCount" to 7)
            }
        }

        val db1 = Room.databaseBuilder(context, EleganDatabase::class.java, dbName).build()
        val repo1 = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = RepositorioEstadisticasPublicaciones(),
            apiProvider = { fakeApi },
            tokenProvider = { null },
            pendingShareDaoProvider = { db1.pendingVisitorShareDao() }
        )

        val countWhileOffline = repo1.recordPublicationShare("prod_offline_1")
        assertEquals(0L, countWhileOffline)

        val persistedAfterFailure = db1.pendingVisitorShareDao().getPendingShares()
        assertEquals(1, persistedAfterFailure.size)
        val storedEvent = persistedAfterFailure.single()
        assertEquals("prod_offline_1", storedEvent.productId)
        assertFalse(storedEvent.eventId.isBlank())
        assertFalse(storedEvent.visitorId.isBlank())
        assertEquals(null, requestsSent.single().first)
        assertEquals(storedEvent.eventId, requestsSent.single().third["eventId"])
        db1.close()

        // Simula reinicio de la app reabriendo la misma base Room y enviando al volver la conexión sin token
        isOffline = false
        val db2 = Room.databaseBuilder(context, EleganDatabase::class.java, dbName).build()
        val repo2 = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = RepositorioEstadisticasPublicaciones(),
            apiProvider = { fakeApi },
            tokenProvider = { null },
            pendingShareDaoProvider = { db2.pendingVisitorShareDao() }
        )

        val flushed = repo2.flushPendingVisitorShares()
        assertEquals(1, flushed)
        assertTrue(db2.pendingVisitorShareDao().getPendingShares().isEmpty())
        assertEquals(7L, repo2.observePublicationStats().first()["prod_offline_1"]?.sharesCount)

        val retryCall = requestsSent.last()
        assertEquals(null, retryCall.first)
        assertEquals("prod_offline_1", retryCall.second)
        assertEquals(storedEvent.eventId, retryCall.third["eventId"])
        assertEquals(storedEvent.visitorId, retryCall.third["visitorId"])
        assertEquals(true, retryCall.third["fromGuestSession"])

        db2.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `test persistent Room queue deletes only backend confirmed eventId and preserves failed events for idempotent retry`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val db = Room.inMemoryDatabaseBuilder(context, EleganDatabase::class.java).build()
        var failProdA = true
        var failProdB = true
        val sentEventIds = mutableListOf<Pair<String, String>>()

        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun recordShare(
                authorization: String?,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                val eventId = request["eventId"] as String
                sentEventIds.add(productId to eventId)
                return when (productId) {
                    "prod_a" -> {
                        if (failProdA) throw IOException("Sin conexión")
                        mapOf("success" to true, "sharesCount" to 3)
                    }
                    "prod_b" -> {
                        if (failProdB) mapOf("success" to false)
                        else mapOf("success" to true, "sharesCount" to 5)
                    }
                    else -> emptyMap()
                }
            }
        }

        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = RepositorioEstadisticasPublicaciones(),
            apiProvider = { fakeApi },
            tokenProvider = { null },
            pendingShareDaoProvider = { db.pendingVisitorShareDao() }
        )

        repo.recordPublicationShare("prod_a")
        repo.recordPublicationShare("prod_b")

        val queued = db.pendingVisitorShareDao().getPendingShares()
        assertEquals(2, queued.size)
        val eventA = queued.first { it.productId == "prod_a" }
        val eventB = queued.first { it.productId == "prod_b" }
        assertTrue(eventA.eventId != eventB.eventId)

        // Vuelve conexión pero backend solo confirma prod_a y rechaza (success=false) prod_b
        failProdA = false
        failProdB = true
        val partialFlushed = repo.flushPendingVisitorShares()
        assertEquals(1, partialFlushed)

        val remainingAfterPartial = db.pendingVisitorShareDao().getPendingShares()
        assertEquals(listOf(eventB.eventId), remainingAfterPartial.map { it.eventId })
        assertEquals(3L, repo.observePublicationStats().first()["prod_a"]?.sharesCount)

        // Segundo reintento: solo envía eventB con exactamente el mismo eventId y luego limpia la cola
        sentEventIds.clear()
        failProdB = false
        val finalFlushed = repo.flushPendingVisitorShares()
        assertEquals(1, finalFlushed)
        assertEquals(listOf("prod_b" to eventB.eventId), sentEventIds)
        assertTrue(db.pendingVisitorShareDao().getPendingShares().isEmpty())
        assertEquals(5L, repo.observePublicationStats().first()["prod_b"]?.sharesCount)

        db.close()
    }

    @Test
    fun `test elegan database disables destructive fallback and migration preserves pending visitor shares`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val dbName = "elegan_migration_test_${UUID.randomUUID()}.db"

        // 1. Crea la base v1 con EleganDatabase.createBuilder (sin fallbackToDestructiveMigration) e inserta eventos pendientes.
        val v1Db = EleganDatabase.createBuilder(context, dbName).build()
        val pendingRow = PendingVisitorShareEntity(
            eventId = "evt_migration_keep_1",
            productId = "prod_migration_1",
            visitorId = "visitor_migration_1",
            createdAt = 1_700_000_000_000L
        )
        v1Db.pendingVisitorShareDao().insertPendingShare(pendingRow)
        assertEquals(listOf(pendingRow), v1Db.pendingVisitorShareDao().getPendingShares())
        v1Db.close()

        // 2. Verifica que EleganDatabase.createBuilder NO borre tablas si hay cambio de versión sin migración,
        // sino que proteja los datos preservando la tabla intacta en SQLite.
        val sqliteHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) = Unit
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
                        // Simula una migración incremental segura agregando una columna nueva sin recrear la tabla.
                        val migration1To2 = object : Migration(1, 2) {
                            override fun migrate(db: SupportSQLiteDatabase) {
                                db.execSQL("ALTER TABLE pending_visitor_shares ADD COLUMN attemptCount INTEGER NOT NULL DEFAULT 0")
                            }
                        }
                        migration1To2.migrate(db)
                    }
                })
                .build()
        )
        val upgradedDb = sqliteHelper.writableDatabase
        upgradedDb.query("SELECT eventId, productId, visitorId, createdAt, attemptCount FROM pending_visitor_shares").use { cursor ->
            assertTrue("Los registros pendientes deben conservarse tras migrar la tabla", cursor.moveToFirst())
            assertEquals("evt_migration_keep_1", cursor.getString(0))
            assertEquals("prod_migration_1", cursor.getString(1))
            assertEquals("visitor_migration_1", cursor.getString(2))
            assertEquals(1_700_000_000_000L, cursor.getLong(3))
            assertEquals(0, cursor.getInt(4))
        }
        upgradedDb.close()
        sqliteHelper.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `test productCategoryDisplayName resolves category title from categories list and hides raw UUID`() {
        val categories = listOf(
            CategoryItem(id = "c8d8cbe6-afab-4cff-a036-9a404c461e25", title = "Camiza"),
            CategoryItem(id = "Camisa", title = "Camisas Mayúscula"),
            CategoryItem(id = "camisa", title = "Camisas Minúscula")
        )
        val uuidProduct: ProductJson = mapOf(
            "id" to "p1",
            "name" to "Camisa Oxford",
            "category" to "c8d8cbe6-afab-4cff-a036-9a404c461e25"
        )
        assertEquals("Camiza", uuidProduct.productCategoryDisplayName(categories))
        assertEquals("", uuidProduct.productCategoryDisplayName(emptyList()))
        assertEquals("c8d8cbe6-afab-4cff-a036-9a404c461e25", uuidProduct.productCategory())

        val upperCaseProduct: ProductJson = mapOf("id" to "p2", "category" to "Camisa")
        val lowerCaseProduct: ProductJson = mapOf("id" to "p3", "category" to "camisa")
        assertEquals("Camisas Mayúscula", upperCaseProduct.productCategoryDisplayName(categories))
        assertEquals("Camisas Minúscula", lowerCaseProduct.productCategoryDisplayName(categories))

        val explicitNameProduct: ProductJson = mapOf(
            "id" to "p4",
            "category" to "c8d8cbe6-afab-4cff-a036-9a404c461e25",
            "categoryName" to "Sastrería"
        )
        assertEquals("Sastrería", explicitNameProduct.productCategoryDisplayName(emptyList()))
    }

    @Test
    fun `test announcements parse camelCase destinationType and destinationId for category and product`() {
        val payload = mapOf(
            "items" to listOf(
                mapOf(
                    "id" to "ann_cat_1",
                    "title" to "Anuncio categoría",
                    "message" to "Explora la colección",
                    "destinationType" to "category",
                    "destinationId" to "c8d8cbe6-afab-4cff-a036-9a404c461e25",
                    "active" to true
                ),
                mapOf(
                    "id" to "ann_prod_1",
                    "title" to "oferta",
                    "message" to "Producto destacado",
                    "destinationType" to "product",
                    "destinationId" to "37eef6ac-148d-4024-b5ec-92a39ebdcd66",
                    "active" to true
                )
            )
        )

        val parsed = extractAnnouncementsJson(payload)
        assertEquals(2, parsed.size)

        val categoryAnnouncement = parsed[0]
        assertEquals("category", categoryAnnouncement.announcementDestinationType())
        assertEquals("c8d8cbe6-afab-4cff-a036-9a404c461e25", categoryAnnouncement.announcementDestinationId())
        assertEquals(
            AnnouncementDestination.Category("c8d8cbe6-afab-4cff-a036-9a404c461e25"),
            categoryAnnouncement.announcementDestination()
        )
        assertEquals("c8d8cbe6-afab-4cff-a036-9a404c461e25", categoryAnnouncement.announcementCategory())

        val productAnnouncement = parsed[1]
        assertEquals("product", productAnnouncement.announcementDestinationType())
        assertEquals("37eef6ac-148d-4024-b5ec-92a39ebdcd66", productAnnouncement.announcementDestinationId())
        assertEquals(
            AnnouncementDestination.Product("37eef6ac-148d-4024-b5ec-92a39ebdcd66"),
            productAnnouncement.announcementDestination()
        )
        assertEquals("", productAnnouncement.announcementCategory())
    }

    @Test
    fun `test setProductSaved and togglePublicationLike rethrow CancellationException and revert optimistic state`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val statsRepo = RepositorioEstadisticasPublicaciones()
        val fakeApi = object : StubEleganAuthApi() {
            override suspend fun getSaved(authorization: String): Map<String, Any?> =
                mapOf("productIds" to emptyList<String>())

            override suspend fun getLikes(authorization: String): Map<String, Any?> =
                mapOf("productIds" to emptyList<String>())

            override suspend fun setSaved(
                authorization: String,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                throw kotlinx.coroutines.CancellationException("Cancelado al salir de pantalla")
            }

            override suspend fun setLike(
                authorization: String,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> {
                throw kotlinx.coroutines.CancellationException("Cancelado al salir de pantalla")
            }
        }
        val repo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "valid_backend_token" }
        )

        var saveCancelled = false
        try {
            repo.setProductSaved("prod_cancel_1", true)
        } catch (_: kotlinx.coroutines.CancellationException) {
            saveCancelled = true
        }
        assertTrue("CancellationException debe propagarse en setProductSaved", saveCancelled)
        assertFalse("prod_cancel_1" in repo.observeSavedProductIds().first())

        var likeCancelled = false
        try {
            repo.togglePublicationLike("prod_cancel_1")
        } catch (_: kotlinx.coroutines.CancellationException) {
            likeCancelled = true
        }
        assertTrue("CancellationException debe propagarse en togglePublicationLike", likeCancelled)
        assertFalse("prod_cancel_1" in repo.observeLikedPublicationIds().first())
    }
}

private open class StubEleganAuthApi : BaseFakeEleganAuthApi()
