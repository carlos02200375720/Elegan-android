package com.example

import android.os.Looper
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.remote.AccionRefrescoTiempoReal
import com.example.data.remote.CoordinadorEventosTiempoReal
import com.example.data.remote.MobileSession
import com.example.data.remote.RealtimeEvent
import com.example.data.repository.EleganRepository
import com.example.data.repository.RepositorioAnuncios
import com.example.data.repository.RepositorioCarrito
import com.example.data.repository.RepositorioCatalogo
import com.example.data.repository.RepositorioCategorias
import com.example.data.repository.RepositorioComentarios
import com.example.data.repository.RepositorioEstadisticasPublicaciones
import com.example.data.repository.RepositorioGuardadosYLikes
import com.example.data.repository.RepositorioMedia
import com.example.data.repository.RepositorioMensajesSoporte
import com.example.data.repository.RepositorioPedidos
import com.example.data.repository.RepositorioPerfilYCuenta
import com.example.ui.calcularVentanaIdsVisibles
import com.example.ui.components.EstadoReproductorVideo
import com.example.ui.components.GestorVideoActivo
import com.example.ui.filtrarProductosCatalogo
import com.example.ui.resolveCategoryCoverImages
import com.example.ui.viewmodel.InteraccionesViewModel
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasEscalabilidadTest {

    @Before
    fun setUp() {
        GestorVideoActivo.reiniciarParaPruebas()
        reiniciarSesionClienteDemo(obtenerContextoPrueba())
    }

    @After
    fun tearDown() {
        GestorVideoActivo.reiniciarParaPruebas()
        MobileSession.clear(obtenerContextoPrueba())
    }

    @Test
    fun `GestorVideoActivo garantiza un unico reproductor activo y libera el anterior`() {
        val videoA = EstadoReproductorVideo(initialAutoPlay = false)
        val videoB = EstadoReproductorVideo(initialAutoPlay = false)

        videoA.iniciarReproduccion()
        assertTrue(videoA.shouldLoadPlayer)
        assertTrue(videoA.isPlaying)
        assertSame(videoA, GestorVideoActivo.estadoActivoActual())

        // Al iniciar el video B, el video A debe soltar inmediatamente su ExoPlayer/MediaCodec
        videoB.iniciarReproduccion()
        assertFalse(videoA.shouldLoadPlayer)
        assertFalse(videoA.isPlaying)
        assertTrue(videoB.shouldLoadPlayer)
        assertTrue(videoB.isPlaying)
        assertSame(videoB, GestorVideoActivo.estadoActivoActual())

        // Al liberar el reproductor activo, no queda ninguno retenido
        videoB.liberarReproductor()
        assertFalse(videoB.shouldLoadPlayer)
        assertFalse(videoB.isPlaying)
        assertNull(GestorVideoActivo.estadoActivoActual())
    }

    @Test
    fun `CoordinadorEventosTiempoReal agrupa rafagas sociales y nunca recarga productos por comentarios`() = runBlocking {
        val accionesEjecutadas = CopyOnWriteArrayList<AccionRefrescoTiempoReal>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinador = CoordinadorEventosTiempoReal(
                scope = scope,
                onEjecutar = { accionesEjecutadas.add(it) },
                delayPrivadoMs = 120L,
                delayGlobalBaseMs = 150L,
                delaySocialBaseMs = 150L,
                jitterMaximoMs = 20L,
                jitterProvider = { 10L }
            )

            // Ráfaga de 50 eventos sociales (comentarios, likes, guardados, compartidos)
            repeat(20) {
                coordinador.procesarEvento(RealtimeEvent(collection = "comments", operation = "create"))
                coordinador.procesarEvento(RealtimeEvent(collection = "likes", operation = "update"))
            }
            repeat(10) {
                coordinador.procesarEvento(RealtimeEvent(collection = "guardadas", operation = "update"))
                coordinador.procesarEvento(RealtimeEvent(collection = "publication_shares", operation = "create"))
            }

            delay(320L)

            assertEquals(
                "Los comentarios deben coalescerse en un único refresco de comentarios abiertos",
                1,
                accionesEjecutadas.count { it == AccionRefrescoTiempoReal.COMENTARIOS_ABIERTOS }
            )
            assertEquals(
                "Los likes/guardados/shares/comments deben coalescerse en un único refresco visible",
                1,
                accionesEjecutadas.count { it == AccionRefrescoTiempoReal.INTERACCIONES_VISIBLES }
            )
            assertEquals(
                "Los eventos de comentarios nunca deben disparar una recarga del catálogo completo",
                0,
                accionesEjecutadas.count { it == AccionRefrescoTiempoReal.PRODUCTOS }
            )

            // Ráfaga de 10 eventos de catálogo también debe agruparse en 1
            repeat(10) {
                coordinador.procesarEvento(RealtimeEvent(collection = "products", operation = "update"))
            }
            delay(320L)
            assertEquals(1, accionesEjecutadas.count { it == AccionRefrescoTiempoReal.PRODUCTOS })
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `CoordinadorEventosTiempoReal cancela trabajos pendientes al detenerse`() = runBlocking {
        val accionesEjecutadas = CopyOnWriteArrayList<AccionRefrescoTiempoReal>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val coordinador = CoordinadorEventosTiempoReal(
                scope = scope,
                onEjecutar = { accionesEjecutadas.add(it) },
                delayPrivadoMs = 80L,
                delayGlobalBaseMs = 80L,
                delaySocialBaseMs = 80L,
                jitterMaximoMs = 0L
            )
            coordinador.procesarEvento(RealtimeEvent(collection = "products", operation = "update"))
            coordinador.procesarEvento(RealtimeEvent(collection = "cart", operation = "update"))
            coordinador.cancelarTodo()
            delay(150L)
            assertTrue(accionesEjecutadas.isEmpty())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `InteraccionesViewModel solo consulta conteos para la ventana visible acotada y respeta TTL`() = runBlocking {
        val context = obtenerContextoPrueba()
        val likeCountBatches = CopyOnWriteArrayList<List<String>>()
        val saveCountBatches = CopyOnWriteArrayList<List<String>>()
        val shareCountBatches = CopyOnWriteArrayList<List<String>>()

        val fakeApi = object : BaseFakeEleganAuthApi() {
            override suspend fun getLikeCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                likeCountBatches.add(productIds)
                return mapOf("counts" to productIds.associateWith { 3 })
            }

            override suspend fun getSavedCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                saveCountBatches.add(productIds)
                return mapOf("counts" to productIds.associateWith { 2 })
            }

            override suspend fun getShareCounts(authorization: String?, productIds: List<String>): Map<String, Any?> {
                shareCountBatches.add(productIds)
                return mapOf("counts" to productIds.associateWith { 1 })
            }
        }

        val statsRepo = RepositorioEstadisticasPublicaciones()
        val carritoRepo = RepositorioCarrito(context)
        val guardadosRepo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { "backend_test_token" }
        )
        val pedidosRepo = RepositorioPedidos(context, carritoRepo)
        val mensajesRepo = RepositorioMensajesSoporte(context)
        val repo = EleganRepository(
            appContext = context,
            catalogoRepo = RepositorioCatalogo(),
            categoriasRepo = RepositorioCategorias(),
            anunciosRepo = RepositorioAnuncios(),
            mediaRepo = RepositorioMedia(context),
            carritoRepo = carritoRepo,
            estadisticasRepo = statsRepo,
            guardadosYLikesRepo = guardadosRepo,
            comentariosRepo = RepositorioComentarios(appContext = context, statsRepo = statsRepo),
            pedidosRepo = pedidosRepo,
            mensajesRepo = mensajesRepo,
            perfilYCuentaRepo = RepositorioPerfilYCuenta(
                appContext = context,
                carritoRepo = carritoRepo,
                guardadosYLikesRepo = guardadosRepo,
                pedidosRepo = pedidosRepo,
                mensajesRepo = mensajesRepo
            )
        )

        var currentTimeMs = 1_000L
        val vm = InteraccionesViewModel(
            repository = repo,
            autoSync = false,
            countsTtlMs = 30_000L,
            maxVisibleCountsWindow = 10,
            nowProvider = { currentTimeMs },
            computationDispatcher = Dispatchers.Unconfined
        )

        suspend fun esperarHasta(condicion: () -> Boolean) {
            repeat(50) {
                Shadows.shadowOf(Looper.getMainLooper()).idle()
                if (condicion()) return
                delay(20L)
            }
            Shadows.shadowOf(Looper.getMainLooper()).idle()
        }

        // Al reportar 30 IDs, debe limitar estrictamente a la ventana máxima (10)
        val requestedIds = (1..30).map { "prod_$it" }
        vm.onVisibleProductsChanged(requestedIds)
        esperarHasta {
            likeCountBatches.size >= 1 && saveCountBatches.size >= 1 && shareCountBatches.size >= 1
        }

        assertEquals(1, likeCountBatches.size)
        assertEquals(1, saveCountBatches.size)
        assertEquals(1, shareCountBatches.size)
        assertEquals(10, likeCountBatches.first().size)
        assertEquals("prod_1", likeCountBatches.first().first())
        assertEquals("prod_10", likeCountBatches.first().last())

        // Dentro de la ventana TTL, repetir los mismos IDs no genera nuevas peticiones HTTP
        currentTimeMs += 5_000L
        vm.onVisibleProductsChanged(requestedIds.take(10))
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        delay(60L)
        assertEquals(1, likeCountBatches.size)

        // Al expirar el TTL, vuelve a consultar únicamente la ventana visible actual
        currentTimeMs += 31_000L
        vm.onVisibleProductsChanged(listOf("prod_1", "prod_2"))
        esperarHasta { likeCountBatches.size >= 2 }

        assertEquals(2, likeCountBatches.size)
        assertEquals(listOf("prod_1", "prod_2"), likeCountBatches.last())
    }

    @Test
    fun `calcularVentanaIdsVisibles respeta cabeceras y tamano maximo de ventana`() {
        val products = (1..100).map { i ->
            Product(id = "p_$i", name = "Prenda $i", categoryId = "cat_1", price = 1000.0)
        }
        val window = calcularVentanaIdsVisibles(
            featuredProducts = products,
            firstVisibleItemIndex = 14,
            visibleItemCount = 3,
            headerItemCount = 4,
            lookaheadCount = 6,
            maxWindowSize = 20
        )
        // Índice 14 - 4 cabeceras = producto índice 10 (p_11), total 3 + 6 = 9 elementos
        assertEquals(9, window.size)
        assertEquals("p_11", window.first())
        assertEquals("p_19", window.last())
    }

    @Test
    fun `resolveCategoryCoverImages y filtrarProductosCatalogo escalan con miles de productos`() {
        val categories = (1..20).map { idx ->
            CategoryItem(
                id = "cat_$idx",
                title = "Colección $idx",
                imageUrl = if (idx == 1) "https://cdn.elegan.test/cat1_custom.jpg" else ""
            )
        }
        val products = (1..5_000).map { idx ->
            val catIndex = ((idx - 1) % 20) + 1
            Product(
                id = "prod_$idx",
                name = if (idx == 4_999) "Traje Exclusivo Milán" else "Prenda Clásica $idx",
                description = if (idx == 2_500) "Lana merino italiana premium" else "Corte sastre",
                categoryId = "cat_$catIndex",
                imageUrl = "https://cdn.elegan.test/p_$idx.jpg",
                price = 5000.0 + idx
            )
        }

        val covers = resolveCategoryCoverImages(categories, products)
        assertEquals(20, covers.size)
        assertEquals("https://cdn.elegan.test/cat1_custom.jpg", covers["cat_1"])
        assertEquals("https://cdn.elegan.test/p_2.jpg", covers["cat_2"])
        assertEquals("https://cdn.elegan.test/p_20.jpg", covers["cat_20"])

        val categoryTitleById = categories.associate { it.id to it.title }
        val byName = filtrarProductosCatalogo(products, categoryTitleById, selectedCategory = null, query = "Milán")
        assertEquals(listOf("prod_4999"), byName.map { it.id })

        val byDesc = filtrarProductosCatalogo(products, categoryTitleById, selectedCategory = null, query = "merino")
        assertEquals(listOf("prod_2500"), byDesc.map { it.id })

        val byCategoryAndTitle = filtrarProductosCatalogo(
            products = products,
            categoryTitleById = categoryTitleById,
            selectedCategory = "cat_5",
            query = "Colección 5"
        )
        assertEquals(250, byCategoryAndTitle.size)
        assertTrue(byCategoryAndTitle.all { it.categoryId == "cat_5" })
    }
}
