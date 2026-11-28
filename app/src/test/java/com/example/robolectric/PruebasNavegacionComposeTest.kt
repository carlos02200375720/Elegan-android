package com.example

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AlmacenBloqueoTransferencia
import com.example.data.remote.MobileSession
import com.example.ui.theme.EleganTheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasNavegacionComposeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        val context = obtenerContextoPrueba()
        instalarImageLoaderOfflineParaPruebas(context)
        AlmacenBloqueoTransferencia.limpiar(context)
        reiniciarSesionClienteDemo(context)
    }

    @After
    fun tearDown() {
        val context = obtenerContextoPrueba()
        AlmacenBloqueoTransferencia.limpiar(context)
        MobileSession.clear(context)
        restaurarImageLoaderPruebas()
    }

    @Test
    fun `typed routes preserve serialization and bottom bar rules`() {
        val producto = RutaApp.Producto(productId = "prod_42")
        val encoded = Json.encodeToString(producto)
        val decoded = Json.decodeFromString<RutaApp.Producto>(encoded)
        assertEquals(producto, decoded)

        assertTrue(RutaApp.Inicio.tieneBarraInferior())
        assertTrue(RutaApp.Catalogo.tieneBarraInferior())
        assertTrue(RutaApp.Perfil.tieneBarraInferior())
        assertFalse(RutaApp.Categoria.tieneBarraInferior())
        assertFalse(producto.tieneBarraInferior())
        assertFalse(RutaApp.Carrito.tieneBarraInferior())
        assertFalse(RutaApp.Verificacion.tieneBarraInferior())
        assertFalse(RutaApp.Gracia.tieneBarraInferior())
        assertFalse(RutaApp.Chats.tieneBarraInferior())

        assertEquals("PRODUCTO", producto.nombreAnalytics())
        assertEquals("VERIFICASION", RutaApp.Verificacion.nombreAnalytics())
    }

    @Test
    fun `AppContent navigates across typed Navigation Compose routes and pops real backstack`() {
        lateinit var navController: NavHostController

        composeTestRule.setContent {
            navController = rememberNavController()
            EleganTheme {
                AppContent(
                    navController = navController,
                    isAuthenticated = true,
                    onSignOut = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        assertEquals(RutaApp.Inicio, navController.currentDestination.aRutaApp())
        composeTestRule.onNodeWithTag("nav_inicio").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_catalogo").assertIsDisplayed()

        // 1. Navegar a pestaña Catálogo manteniendo la barra inferior
        composeTestRule.onNodeWithTag("nav_catalogo").performClick()
        composeTestRule.waitForIdle()
        assertEquals(RutaApp.Catalogo, navController.currentDestination.aRutaApp())
        composeTestRule.onNodeWithTag("nav_chats").assertIsDisplayed()

        // 2. Navegar a ruta secundaria Chats (oculta barra inferior)
        composeTestRule.onNodeWithTag("nav_chats").performClick()
        composeTestRule.waitForIdle()
        assertEquals(RutaApp.Chats, navController.currentDestination.aRutaApp())
        composeTestRule.onNodeWithTag("nav_inicio").assertDoesNotExist()

        // 3. Back del sistema saca Chats del backstack y regresa a Catálogo
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertEquals(RutaApp.Catalogo, navController.currentDestination.aRutaApp())
        composeTestRule.onNodeWithTag("nav_inicio").assertIsDisplayed()

        // 4. Back del sistema regresa de Catálogo a Inicio
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        assertEquals(RutaApp.Inicio, navController.currentDestination.aRutaApp())

        // 5. Navegar a Perfil desde Inicio
        composeTestRule.onNodeWithTag("nav_perfil").performClick()
        composeTestRule.waitForIdle()
        assertEquals(RutaApp.Perfil, navController.currentDestination.aRutaApp())
    }
}
