package com.example

import android.os.Looper
import android.view.View
import androidx.startup.AppInitializer
import com.example.data.local.EleganDatabase
import com.example.data.observability.MonitorErrores
import com.example.startup.BaseDatosLocalInitializer
import com.example.startup.ClienteHttpInitializer
import com.example.startup.CoordinadorInicioDiferido
import com.example.startup.SentryInitializer
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasAppStartupTest {

    @Before
    fun setUp() {
        limpiarCacheAppInitializer()
        CoordinadorInicioDiferido.reiniciarParaPruebas()
        MonitorErrores.reiniciarParaPruebas()
    }

    @After
    fun tearDown() {
        limpiarCacheAppInitializer()
        CoordinadorInicioDiferido.reiniciarParaPruebas()
        MonitorErrores.reiniciarParaPruebas()
    }

    private fun limpiarCacheAppInitializer() {
        val context = obtenerContextoPrueba()
        val appInitializer = AppInitializer.getInstance(context)
        val field = AppInitializer::class.java.getDeclaredField("mInitialized")
        field.isAccessible = true
        val initializedMap = field.get(appInitializer) as? MutableMap<*, *>
        initializedMap?.clear()
    }

    @Test
    fun `Sentry y BaseDatosLocal no se autodescubren en arranque en frio y declaran su grafo de dependencias`() {
        val context = obtenerContextoPrueba()
        val appInitializer = AppInitializer.getInstance(context)

        // Solo ClienteHttpInitializer está en InitializationProvider; Sentry y Room permanecen diferidos
        assertFalse(
            "SentryInitializer no debe autodescubrirse en el arranque en frío",
            appInitializer.isEagerlyInitialized(SentryInitializer::class.java)
        )
        assertFalse(
            "BaseDatosLocalInitializer no debe autodescubrirse en el arranque en frío",
            appInitializer.isEagerlyInitialized(BaseDatosLocalInitializer::class.java)
        )
        assertFalse(
            "MonitorErrores no debe inicializarse durante Application.onCreate()",
            MonitorErrores.estaInicializado()
        )

        assertEquals(emptyList<Class<*>>(), ClienteHttpInitializer().dependencies())
        assertEquals(listOf(ClienteHttpInitializer::class.java), SentryInitializer().dependencies())
        assertEquals(listOf(SentryInitializer::class.java), BaseDatosLocalInitializer().dependencies())
    }

    @Test
    fun `CoordinadorInicioDiferido inicializa Room y transitivamente Sentry mediante AppInitializer`() {
        val context = obtenerContextoPrueba()

        assertFalse(MonitorErrores.estaInicializado())
        assertFalse(CoordinadorInicioDiferido.estaCompletado())

        val estado = CoordinadorInicioDiferido.inicializarComponentesDiferidosAhora(context)

        assertTrue(estado.instanciaLista)
        assertEquals(EleganDatabase.DATABASE_NAME, estado.nombreBaseDatos)
        assertTrue(
            "BaseDatosLocalInitializer debe inicializar transitivamente SentryInitializer",
            MonitorErrores.estaInicializado()
        )
        assertTrue(CoordinadorInicioDiferido.estaCompletado())
    }

    @Test
    fun `programarTrasPrimerFrame difiere la inicializacion hasta despues del mensaje de vista`() {
        val context = obtenerContextoPrueba()
        val view = View(context)

        CoordinadorInicioDiferido.programarTrasPrimerFrame(
            decorView = view,
            context = context,
            dispatcher = Dispatchers.Unconfined
        )

        Shadows.shadowOf(Looper.getMainLooper()).idle()

        assertTrue(CoordinadorInicioDiferido.estaCompletado())
        assertTrue(MonitorErrores.estaInicializado())
    }
}
