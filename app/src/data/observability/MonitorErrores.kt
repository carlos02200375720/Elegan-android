package com.example.data.observability

import android.app.Application
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import io.sentry.Breadcrumb
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException

/**
 * Capa centralizada de observabilidad y monitoreo de crashes para producción.
 *
 * - Captura crashes fatales, ANRs y errores no fatales en Sentry cuando `SENTRY_DSN` está configurado.
 * - Mantiene trazabilidad local mediante `Log.e` / `Log.w` incluso sin DSN configurado.
 * - Evita exponer PII por defecto y descarta cancelaciones esperadas de corrutinas.
 */
object MonitorErrores {
    private const val TAG_INTERNO = "MonitorErrores"
    private const val PLACEHOLDER_DSN = "SENTRY_DSN_DEFAULT_VALUE"

    private val inicializado = AtomicBoolean(false)

    @Volatile
    private var monitoreoRemotoActivo: Boolean = false

    fun inicializar(application: Application) {
        if (!inicializado.compareAndSet(false, true)) return

        if ("robolectric".equals(Build.FINGERPRINT, ignoreCase = true)) {
            monitoreoRemotoActivo = false
            return
        }

        val dsn = BuildConfig.SENTRY_DSN.trim()
        if (!esDsnValido(dsn)) {
            monitoreoRemotoActivo = false
            if (BuildConfig.DEBUG) {
                Log.i(
                    TAG_INTERNO,
                    "Monitoreo remoto en pausa: configura SENTRY_DSN para activar el envío de crashes."
                )
            }
            return
        }

        try {
            SentryAndroid.init(application) { options ->
                options.dsn = dsn
                options.environment = if (BuildConfig.DEBUG) "debug" else "production"
                options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
                options.isAnrEnabled = true
                options.isAttachStacktrace = true
                options.isEnableAutoSessionTracking = true
                options.isDebug = false
                options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                    if (event.throwable is CancellationException) {
                        null
                    } else {
                        event
                    }
                }
            }
            monitoreoRemotoActivo = true
        } catch (error: Throwable) {
            monitoreoRemotoActivo = false
            Log.e(TAG_INTERNO, "No se pudo inicializar el monitoreo remoto de crashes.", error)
        }
    }

    fun estaActivo(): Boolean = monitoreoRemotoActivo

    fun registrarError(
        tag: String,
        mensaje: String,
        error: Throwable? = null,
        extras: Map<String, String> = emptyMap()
    ) {
        if (error is CancellationException) return

        if (error != null) {
            Log.e(tag, mensaje, error)
        } else {
            Log.e(tag, mensaje)
        }

        if (!monitoreoRemotoActivo) return

        runCatching {
            Sentry.withScope { scope ->
                scope.level = SentryLevel.ERROR
                scope.setTag("module", tag)
                scope.setExtra("summary", mensaje)
                extras.forEach { (clave, valor) ->
                    if (clave.isNotBlank() && valor.isNotBlank()) {
                        scope.setExtra(clave, valor)
                    }
                }
                if (error != null) {
                    Sentry.captureException(error)
                } else {
                    Sentry.captureMessage("[$tag] $mensaje", SentryLevel.ERROR)
                }
            }
        }
    }

    fun registrarAdvertencia(
        tag: String,
        mensaje: String,
        error: Throwable? = null,
        extras: Map<String, String> = emptyMap()
    ) {
        if (error is CancellationException) return

        if (error != null) {
            Log.w(tag, mensaje, error)
        } else {
            Log.w(tag, mensaje)
        }

        if (!monitoreoRemotoActivo) return

        runCatching {
            val breadcrumb = Breadcrumb().apply {
                category = tag
                message = mensaje
                level = SentryLevel.WARNING
                error?.javaClass?.simpleName?.let { setData("exception", it) }
                error?.message?.takeIf { it.isNotBlank() }?.let { setData("detail", it.take(180)) }
                extras.forEach { (clave, valor) ->
                    if (clave.isNotBlank() && valor.isNotBlank()) {
                        setData(clave, valor)
                    }
                }
            }
            Sentry.addBreadcrumb(breadcrumb)
        }
    }

    fun registrarNavegacion(pantalla: String) {
        if (!monitoreoRemotoActivo || pantalla.isBlank()) return
        runCatching {
            val breadcrumb = Breadcrumb().apply {
                type = "navigation"
                category = "ui.navigation"
                message = "Pantalla: $pantalla"
                level = SentryLevel.INFO
                setData("screen", pantalla)
            }
            Sentry.addBreadcrumb(breadcrumb)
        }
    }

    fun registrarEvento(
        categoria: String,
        mensaje: String,
        datos: Map<String, String> = emptyMap()
    ) {
        if (!monitoreoRemotoActivo) return
        runCatching {
            val breadcrumb = Breadcrumb().apply {
                category = categoria
                message = mensaje
                level = SentryLevel.INFO
                datos.forEach { (clave, valor) ->
                    if (clave.isNotBlank() && valor.isNotBlank()) {
                        setData(clave, valor)
                    }
                }
            }
            Sentry.addBreadcrumb(breadcrumb)
        }
    }

    fun actualizarUsuario(userId: String?, rol: String? = null) {
        if (!monitoreoRemotoActivo) return
        runCatching {
            val cleanId = userId?.trim().orEmpty()
            if (cleanId.isEmpty()) {
                Sentry.setUser(null)
            } else {
                val usuario = User().apply {
                    id = cleanId
                    rol?.trim()?.takeIf { it.isNotBlank() }?.let {
                        data = mapOf("role" to it)
                    }
                }
                Sentry.setUser(usuario)
            }
        }
    }

    internal fun esDsnValido(dsn: String): Boolean {
        val trimmed = dsn.trim()
        if (trimmed.isEmpty() || trimmed.equals(PLACEHOLDER_DSN, ignoreCase = true)) return false
        return trimmed.startsWith("https://", ignoreCase = true) && trimmed.contains("@")
    }
}
