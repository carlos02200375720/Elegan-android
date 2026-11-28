package com.example

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import androidx.paging.testing.asSnapshot
import com.example.data.model.Product
import com.example.data.repository.EleganRepository
import com.example.data.repository.PaginaProductos
import com.example.data.repository.ProductosPagingSource
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
import com.example.data.repository.extractPaginaProductos
import com.example.ui.viewmodel.CatalogoViewModel
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasPagingProductosTest {

    private fun productMap(
        index: Int,
        categoryId: String = "trajes",
        inStock: Boolean = true
    ): Map<String, Any?> = mapOf(
        "id" to "prod_$index",
        "name" to "Traje Elegante $index",
        "description" to "Confección italiana número $index",
        "categoryId" to categoryId,
        "price" to (4500.0 + index),
        "inStock" to inStock
    )

    @Test
    fun `extractPaginaProductos interpreta metadatos de paginacion y detecta respuesta legada completa`() {
        val paginatedResponse = mapOf(
            "items" to (1..20).map { productMap(it) },
            "pagination" to mapOf(
                "page" to 1,
                "limit" to 20,
                "total" to 500,
                "totalPages" to 25,
                "hasMore" to true
            )
        )
        val page1 = extractPaginaProductos(paginatedResponse, requestedPage = 1, requestedLimit = 20)
        assertEquals(20, page1.items.size)
        assertEquals(1, page1.page)
        assertEquals(2, page1.nextPage)
        assertEquals(500, page1.totalItems)
        assertTrue(page1.serverPaginated)

        val lastPageResponse = mapOf(
            "items" to (481..500).map { productMap(it) },
            "pagination" to mapOf(
                "page" to 25,
                "limit" to 20,
                "totalPages" to 25,
                "hasMore" to false
            )
        )
        val page25 = extractPaginaProductos(lastPageResponse, requestedPage = 25, requestedLimit = 20)
        assertNull(page25.nextPage)

        // Si un backend legado ignora `limit=20` y devuelve 60 elementos sin metadatos,
        // debe marcar serverPaginated=false y cerrar nextPage=null para evitar bucles.
        val legacyFullDump = mapOf(
            "items" to (1..60).map { productMap(it) }
        )
        val legacyPage = extractPaginaProductos(legacyFullDump, requestedPage = 1, requestedLimit = 20)
        assertEquals(60, legacyPage.items.size)
        assertFalse(legacyPage.serverPaginated)
        assertNull(legacyPage.nextPage)
    }

    @Test
    fun `ProductosPagingSource carga multiples paginas deduplicando IDs solapados y maneja errores`() = runBlocking {
        val source = ProductosPagingSource(
            fetchPage = { page, _, _, _, _ ->
                when (page) {
                    1 -> PaginaProductos(
                        items = listOf(
                            Product(id = "p_1", name = "A", inStock = true),
                            Product(id = "p_2", name = "B", inStock = true),
                            Product(id = "p_out", name = "Agotado", inStock = false)
                        ),
                        page = 1,
                        nextPage = 2,
                        serverPaginated = true
                    )
                    2 -> PaginaProductos(
                        items = listOf(
                            // p_2 repetido por desplazamiento de offset en backend -> debe deduplicarse
                            Product(id = "p_2", name = "B", inStock = true),
                            Product(id = "p_3", name = "C", inStock = true)
                        ),
                        page = 2,
                        nextPage = null,
                        serverPaginated = true
                    )
                    else -> PaginaProductos(emptyList(), page, null)
                }
            },
            inStockOnly = true
        )

        val pager = TestPager(
            config = PagingConfig(pageSize = 2, initialLoadSize = 2, enablePlaceholders = false),
            pagingSource = source
        )

        val page1Result = pager.refresh() as PagingSource.LoadResult.Page
        assertEquals(listOf("p_1", "p_2"), page1Result.data.map { it.id })
        assertNull(page1Result.prevKey)
        assertEquals(2, page1Result.nextKey)

        val page2Result = pager.append() as PagingSource.LoadResult.Page
        assertEquals(listOf("p_3"), page2Result.data.map { it.id })
        assertEquals(1, page2Result.prevKey)
        assertNull(page2Result.nextKey)

        // Error de red debe devolverse como LoadResult.Error sin crashear
        val failingSource = ProductosPagingSource(
            fetchPage = { _, _, _, _, _ -> throw IOException("Sin conexión") }
        )
        val failingPager = TestPager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSource = failingSource
        )
        val errorResult = failingPager.refresh()
        assertTrue(errorResult is PagingSource.LoadResult.Error)
    }

    @Test
    fun `RepositorioCatalogo y CatalogoViewModel paginan mil productos bajo demanda sin descargar todo en RAM`() = runBlocking {
        val context = obtenerContextoPrueba()
        val totalCatalogSize = 1_000
        val requestsRecorded = CopyOnWriteArrayList<Triple<Int, Int, Pair<String?, String?>>>()

        val fakeApi = object : BaseFakeEleganAuthApi() {
            override suspend fun productsPage(
                page: Int,
                limit: Int,
                category: String?,
                query: String?,
                inStock: Boolean?
            ): Map<String, Any?> {
                requestsRecorded.add(Triple(page, limit, category to query))
                val matching = (1..totalCatalogSize)
                    .asSequence()
                    .map { idx ->
                        val cat = if (idx % 2 == 0) "camisas" else "trajes"
                        productMap(idx, categoryId = cat, inStock = true)
                    }
                    .filter { map ->
                        val catOk = category == null || map["categoryId"] == category
                        val qOk = query == null || (map["name"] as String).contains(query, ignoreCase = true)
                        catOk && qOk
                    }
                    .toList()

                val start = ((page - 1) * limit).coerceAtLeast(0)
                val end = (start + limit).coerceAtMost(matching.size)
                val slice = if (start < end) matching.subList(start, end) else emptyList()
                val hasMore = end < matching.size
                return mapOf(
                    "items" to slice,
                    "pagination" to mapOf(
                        "page" to page,
                        "limit" to limit,
                        "total" to matching.size,
                        "hasMore" to hasMore
                    )
                )
            }
        }

        val catalogoRepo = RepositorioCatalogo(apiProvider = { fakeApi })

        // Simular scroll hasta el elemento 35: solo debe solicitar páginas iniciales, jamás los 1,000 items
        val feedSnapshot = catalogoRepo.observePagedProducts(
            pageSize = 20,
            prefetchDistance = 5,
            maxSize = 60
        ).asSnapshot {
            scrollTo(index = 35)
        }

        assertTrue("Debe cargar al menos 40 productos al hacer scroll al índice 35", feedSnapshot.size >= 40)
        assertTrue("Nunca debe descargar los 1,000 productos en memoria", feedSnapshot.size < 120)
        assertEquals("prod_1", feedSnapshot.first().id)
        assertTrue(requestsRecorded.all { (_, limit, _) -> limit <= 20 })

        // La caché acotada en memoria del repositorio se alimenta con las páginas cargadas
        val cachedProducts = catalogoRepo.observeProducts().first()
        assertTrue(cachedProducts.isNotEmpty())
        assertTrue(cachedProducts.size <= 250)

        // Integración con CatalogoViewModel filtrando por categoría y búsqueda
        val statsRepo = RepositorioEstadisticasPublicaciones()
        val carritoRepo = RepositorioCarrito(context)
        val guardadosRepo = RepositorioGuardadosYLikes(
            appContext = context,
            statsRepo = statsRepo,
            apiProvider = { fakeApi },
            tokenProvider = { null }
        )
        val pedidosRepo = RepositorioPedidos(context, carritoRepo)
        val mensajesRepo = RepositorioMensajesSoporte(context)
        val repo = EleganRepository(
            appContext = context,
            catalogoRepo = catalogoRepo,
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

        val vm = CatalogoViewModel(
            repository = repo,
            autoLoad = false,
            enablePaging = true,
            computationDispatcher = Dispatchers.Unconfined,
            searchDebounceMs = 0L
        )
        vm.setCategory("camisas")
        vm.setSearchQuery("100")

        assertEquals("camisas", vm.selectedCategory.value)
        assertEquals("100", vm.searchQuery.value)

        val filteredSnapshot = withContext(Dispatchers.IO) {
            repo.observePagedProducts(
                category = vm.selectedCategory.value,
                query = vm.searchQuery.value,
                inStockOnly = true,
                pageSize = 20,
                prefetchDistance = 5,
                maxSize = 60
            ).asSnapshot()
        }
        assertTrue(filteredSnapshot.isNotEmpty())
        assertTrue(filteredSnapshot.all { it.categoryId == "camisas" && it.name.contains("100") })
        assertTrue(
            requestsRecorded.any { (_, _, filters) ->
                filters.first == "camisas" && filters.second == "100"
            }
        )
    }
}
