package com.example.data.remote

import android.content.Context
import com.example.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/**
 * Interceptor centralizado de autenticación para las peticiones dirigidas al backend de Elegan.
 *
 * - Inyecta `Authorization: Bearer <token>` solo cuando el destino coincide con el host HTTPS oficial
 *   y la petición no incluye ya un encabezado explícito.
 * - Nunca reenvía tokens locales (`local_session_`) ni tokens hacia hosts externos/CDN.
 * - Detecta respuestas HTTP 401 en rutas protegidas y notifica expiración de sesión una sola vez.
 */
internal class EleganAuthInterceptor(
    private val tokenProvider: () -> String?,
    private val expectedBackendHost: String? =
        BuildConfig.ELEGAN_API_BASE_URL.toHttpUrlOrNull()?.host,
    private val onUnauthorizedSession: () -> Unit = {}
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val isBackendHttps = original.url.isHttps &&
            !expectedBackendHost.isNullOrBlank() &&
            original.url.host.equals(expectedBackendHost, ignoreCase = true)

        val requestToProceed = if (
            isBackendHttps &&
            original.header(HEADER_AUTHORIZATION).isNullOrBlank() &&
            !esRutaAutenticacionPublica(original.url.encodedPath)
        ) {
            val token = tokenProvider()?.trim()?.takeIf {
                it.isNotBlank() && !it.startsWith(LOCAL_SESSION_PREFIX)
            }
            if (token != null) {
                original.newBuilder()
                    .header(HEADER_AUTHORIZATION, "Bearer $token")
                    .build()
            } else {
                original
            }
        } else {
            original
        }

        val response = chain.proceed(requestToProceed)
        if (
            response.code == 401 &&
            isBackendHttps &&
            !requestToProceed.header(HEADER_AUTHORIZATION).isNullOrBlank() &&
            !esRutaAutenticacionPublica(requestToProceed.url.encodedPath)
        ) {
            onUnauthorizedSession()
        }
        return response
    }

    private fun esRutaAutenticacionPublica(encodedPath: String): Boolean {
        val normalized = encodedPath.lowercase().trimEnd('/')
        return normalized.endsWith("/auth/login") ||
            normalized.endsWith("/auth/register") ||
            normalized.endsWith("/auth/google") ||
            normalized.contains("/auth/password/")
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"
        private const val LOCAL_SESSION_PREFIX = "local_session_"
    }
}

/**
 * Proveedor unificado de clientes [OkHttpClient] derivados de una sola instancia base
 * para compartir el [ConnectionPool] y el [Dispatcher] en toda la app (API, uploads, Coil y WebSockets).
 */
object ClienteHttpElegan {
    @Volatile
    private var appContext: Context? = null

    private val _eventosSesionExpirada = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val eventosSesionExpirada: SharedFlow<Unit> = _eventosSesionExpirada.asSharedFlow()

    fun inicializar(context: Context) {
        appContext = context.applicationContext
    }

    private fun tokenActivoActual(): String? {
        val ctx = appContext ?: return null
        return MobileSession.token(ctx)
    }

    private fun notificarSesionExpirada() {
        appContext?.let { MobileSession.clear(it) }
        _eventosSesionExpirada.tryEmit(Unit)
    }

    private val baseClient: OkHttpClient by lazy {
        val sharedDispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 12
        }
        OkHttpClient.Builder()
            .dispatcher(sharedDispatcher)
            .connectionPool(ConnectionPool(12, 5, TimeUnit.MINUTES))
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val authInterceptor: EleganAuthInterceptor by lazy {
        EleganAuthInterceptor(
            tokenProvider = ::tokenActivoActual,
            onUnauthorizedSession = ::notificarSesionExpirada
        )
    }

    internal val apiClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .addInterceptor(authInterceptor)
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .build()
    }

    internal val uploadClient: OkHttpClient by lazy {
        apiClient.newBuilder()
            .readTimeout(180, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .build()
    }

    /** Cliente derivado sin AuthInterceptor para medios/CDN (Coil), compartiendo pool de conexiones. */
    val mediaClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Cliente derivado para WebSockets en tiempo real compartiendo pool e hilos base. */
    internal val realtimeClient: OkHttpClient by lazy {
        baseClient.newBuilder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
