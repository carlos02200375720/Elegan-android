package com.example.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.data.observability.MonitorErrores

/**
 * Inicializador diferido (lazy) de observabilidad y Sentry mediante Jetpack App Startup.
 *
 * No está registrado en `AndroidManifest.xml` para evitar bloquear el arranque en frío (TTID);
 * se invoca manualmente tras el primer frame mediante `AppInitializer.initializeComponent(...)`.
 */
class SentryInitializer : Initializer<MonitorErrores> {

    override fun create(context: Context): MonitorErrores {
        MonitorErrores.inicializar(context.applicationContext)
        return MonitorErrores
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(ClienteHttpInitializer::class.java)
}
