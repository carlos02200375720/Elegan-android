package com.example.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.data.work.ProgramadorSincronizacionProductos

data class EstadoSincronizacionCatalogo(
    val periodicaProgramada: Boolean,
    val inmediataEncolada: Boolean
)

/**
 * Inicializador diferido (lazy) de WorkManager para sincronización de productos en segundo plano.
 *
 * Depende de [BaseDatosLocalInitializer] para asegurar el orden determinista:
 * `ClienteHttpInitializer -> SentryInitializer -> BaseDatosLocalInitializer -> SincronizacionCatalogoInitializer`
 * ejecutándose fuera de `Application.onCreate()` y del hilo principal.
 */
class SincronizacionCatalogoInitializer : Initializer<EstadoSincronizacionCatalogo> {

    override fun create(context: Context): EstadoSincronizacionCatalogo {
        val appContext = context.applicationContext
        val periodica = ProgramadorSincronizacionProductos.programarSincronizacionPeriodica(appContext)
        val inmediata = ProgramadorSincronizacionProductos.solicitarSincronizacionInmediata(
            context = appContext,
            forceReplace = false
        )
        return EstadoSincronizacionCatalogo(
            periodicaProgramada = periodica,
            inmediataEncolada = inmediata
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(BaseDatosLocalInitializer::class.java)
}
