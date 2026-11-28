package com.example

import android.os.Looper
import androidx.lifecycle.ViewModel
import com.example.data.di.ProveedorContenedorElegan
import com.example.data.di.obtenerContenedorElegan
import com.example.data.model.Product
import com.example.data.remote.MobileSession
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
import com.example.ui.viewmodel.CarritoYPedidosViewModel
import com.example.ui.viewmodel.CatalogoViewModel
import com.example.ui.viewmodel.FabricaViewModelsElegan
import com.example.ui.viewmodel.InteraccionesViewModel
import com.example.ui.viewmodel.PerfilYSoporteViewModel
import com.example.ui.viewmodel.ResumenCheckout
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasContenedorYViewModelsBloqueTresTest {

    private class ViewModelDesconocido : ViewModel()

    @Before
    fun setUp() {
        reiniciarSesionClienteDemo(obtenerContextoPrueba())
    }

    @After
    fun tearDown() {
        MobileSession.clear(obtenerContextoPrueba())
    }

    @Test
    fun `EleganApplication exposes singleton AppContainer with shared repositories`() {
        val context = obtenerContextoPrueba()
        assertTrue(context.applicationContext is ProveedorContenedorElegan)

        val c1 = context.obtenerContenedorElegan()
        val c2 = context.obtenerContenedorElegan()

        assertSame(c1, c2)
        assertSame(c1.eleganRepository, c2.eleganRepository)
        assertSame(c1.authRepository, c2.authRepository)
        assertSame(c1.legalDocumentsRepository, c2.legalDocumentsRepository)
    }

    @Test
    fun `FabricaViewModelsElegan creates all four domain ViewModels and rejects unknown types`() {
        val repo = EleganRepository(obtenerContextoPrueba())
        val factory = FabricaViewModelsElegan(repository = repo, autoLoad = false)

        val catalogoVm = factory.create(CatalogoViewModel::class.java)
        val interaccionesVm = factory.create(InteraccionesViewModel::class.java)
        val carritoVm = factory.create(CarritoYPedidosViewModel::class.java)
        val perfilVm = factory.create(PerfilYSoporteViewModel::class.java)

        assertNotNull(catalogoVm)
        assertNotNull(interaccionesVm)
        assertNotNull(carritoVm)
        assertNotNull(perfilVm)

        try {
            factory.create(ViewModelDesconocido::class.java)
            fail("Debió lanzar IllegalArgumentException para un ViewModel no registrado")
        } catch (expected: IllegalArgumentException) {
            assertTrue(expected.message.orEmpty().contains("ViewModelDesconocido"))
        }
    }

    @Test
    fun `split ViewModels manage independent domain state cleanly without AndroidViewModel`() = runBlocking {
        val context = obtenerContextoPrueba()
        val fakeApi = object : BaseFakeEleganAuthApi() {
            override suspend fun setSaved(
                authorization: String,
                productId: String,
                request: Map<String, Any?>
            ): Map<String, Any?> = mapOf("saved" to true, "savesCount" to 1)
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

        val catalogoVm = CatalogoViewModel(repository = repo, autoLoad = false)
        val interaccionesVm = InteraccionesViewModel(repository = repo, autoSync = false)
        val carritoVm = CarritoYPedidosViewModel(repository = repo, autoLoad = false)
        val perfilVm = PerfilYSoporteViewModel(repository = repo, autoLoad = false)

        // 1. Catálogo
        val blazer = Product(id = "prod_b3_1", name = "Blazer Italiano", categoryId = "trajes", price = 9500.0)
        catalogoVm.setCategory("trajes")
        catalogoVm.setSearchQuery("blazer")
        catalogoVm.selectProduct(blazer)
        assertEquals("trajes", catalogoVm.selectedCategory.value)
        assertEquals("blazer", catalogoVm.searchQuery.value)
        assertEquals("prod_b3_1", catalogoVm.selectedProduct.value?.id)
        catalogoVm.clearSelection()
        assertNull(catalogoVm.selectedProduct.value)

        // 2. Carrito y resumen de checkout
        carritoVm.setCouponCode("  vip20 ")
        assertEquals("VIP20", carritoVm.currentCouponCode())
        carritoVm.setSelectedCartItemIds(listOf("item_1", "item_2"))
        assertEquals(setOf("item_1", "item_2"), carritoVm.selectedCartItemIds.value)
        carritoVm.updateCheckoutTotals(subtotal = 5000.0, discount = 500.0, shipping = 200.0, total = 4700.0)
        assertEquals(
            ResumenCheckout(subtotal = 5000.0, discount = 500.0, shipping = 200.0, total = 4700.0),
            carritoVm.checkoutSummary.value
        )
        carritoVm.clearOrderState()
        assertEquals("", carritoVm.currentCouponCode())
        assertTrue(carritoVm.selectedCartItemIds.value.isEmpty())
        assertEquals(ResumenCheckout(), carritoVm.checkoutSummary.value)

        // 3. Interacciones reactivas sobre repositorio compartido inyectado sin red real
        repo.setProductSaved("prod_b3_1", true)
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertTrue("prod_b3_1" in interaccionesVm.savedProductIds.value)

        // 4. Perfil y sesión
        assertEquals("user-demo-001", perfilVm.sessionUser.value?.id)
        perfilVm.clearSessionSnapshot()
        assertNull(perfilVm.sessionUser.value)
    }
}
