package com.example.startup

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.startup.AppInitializer
import com.example.data.observability.MonitorErrores
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Orquestador de inicialización diferida con Jetpack App Startup (`androidx.startup`).
 *
 * Mantiene `Application.onCreate()` y el primer frame de `MainActivity` libres de trabajo
 * pesado (inicialización de Sentry SDK y preparación de Room), ejecutando la cadena de
 * dependencias en un hilo en segundo plano justo después de renderizar el primer frame.
 */
object CoordinadorInicioDiferido {
    private val programado = AtomicBoolean(false)
    private val completado = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun programarTrasPrimerFrame(
        decorView: View,
        context: Context,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) {
        if (!programado.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val tareaDiferida = Runnable {
            scope.launch(dispatcher) {
                runCatching {
                    inicializarComponentesDiferidosAhora(appContext)
                }.onFailure { error ->
                    MonitorErrores.registrarAdvertencia(
                        tag = "InicioDiferido",
                        mensaje = "Error durante la inicialización diferida de componentes.",
                        error = error
                    )
                }
            }
        }
        if (decorView.isAttachedToWindow) {
            decorView.post(tareaDiferida)
        } else {
            Handler(Looper.getMainLooper()).post(tareaDiferida)
        }
    }

    /**
     * Ejecuta de forma segura e idempotente la cadena diferida en App Startup:
     * `ClienteHttpInitializer -> SentryInitializer -> BaseDatosLocalInitializer -> SincronizacionCatalogoInitializer`.
     */
    fun inicializarComponentesDiferidosAhora(context: Context): EstadoBaseDatosLocal {
        val appContext = context.applicationContext
        val appInitializer = AppInitializer.getInstance(appContext)
        val resultado = appInitializer.initializeComponent(BaseDatosLocalInitializer::class.java)
        appInitializer.initializeComponent(SincronizacionCatalogoInitializer::class.java)
        completado.set(true)
        return resultado
    }

    fun estaCompletado(): Boolean = completado.get()

    internal fun reiniciarParaPruebas() {
        programado.set(false)
        completado.set(false)
    }
}
