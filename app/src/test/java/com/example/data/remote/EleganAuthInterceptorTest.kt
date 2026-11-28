package com.example.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EleganAuthInterceptorTest {

    private fun ejecutarPeticion(
        url: String,
        token: String?,
        existingAuthHeader: String? = null,
        responseCode: Int = 200,
        expectedHost: String = "api.elegan.test"
    ): Pair<Request, Int> {
        var capturedRequest: Request? = null
        var unauthorizedCount = 0

        val interceptor = EleganAuthInterceptor(
            tokenProvider = { token },
            expectedBackendHost = expectedHost,
            onUnauthorizedSession = { unauthorizedCount += 1 }
        )

        val terminalInterceptor = Interceptor { chain ->
            capturedRequest = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(responseCode)
                .message(if (responseCode == 401) "Unauthorized" else "OK")
                .body("{}".toResponseBody("application/json".toMediaType()))
                .build()
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(terminalInterceptor)
            .build()

        val requestBuilder = Request.Builder().url(url)
        if (existingAuthHeader != null) {
            requestBuilder.header("Authorization", existingAuthHeader)
        }

        client.newCall(requestBuilder.build()).execute().use { /* close */ }
        return requireNotNull(capturedRequest) to unauthorizedCount
    }

    @Test
    fun inyectaBearerEnPeticionesHttpsAlBackendOficial() {
        val (request, unauthorizedCount) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/cart",
            token = "token_seguro_123"
        )

        assertEquals("Bearer token_seguro_123", request.header("Authorization"))
        assertEquals(0, unauthorizedCount)
    }

    @Test
    fun noFiltraTokenAHospedadoresExternosNiHttpSinCifrar() {
        val (cdnRequest, _) = ejecutarPeticion(
            url = "https://cdn.externo.test/imagen.jpg",
            token = "token_seguro_123"
        )
        val (httpRequest, _) = ejecutarPeticion(
            url = "http://api.elegan.test/api/movil/cart",
            token = "token_seguro_123"
        )

        assertNull(cdnRequest.header("Authorization"))
        assertNull(httpRequest.header("Authorization"))
    }

    @Test
    fun ignoraTokensLocalesYRespetaEncabezadoExplicito() {
        val (localTokenReq, _) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/orders",
            token = "local_session_antigua"
        )
        val (explicitHeaderReq, _) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/orders",
            token = "token_del_almacen",
            existingAuthHeader = "Bearer token_explicito"
        )

        assertNull(localTokenReq.header("Authorization"))
        assertEquals("Bearer token_explicito", explicitHeaderReq.header("Authorization"))
    }

    @Test
    fun noInyectaTokenEnRutasPublicasDeAutenticacion() {
        val (loginReq, _) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/auth/login",
            token = "token_seguro_123"
        )
        val (resetReq, _) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/auth/password/request-reset",
            token = "token_seguro_123"
        )

        assertNull(loginReq.header("Authorization"))
        assertNull(resetReq.header("Authorization"))
    }

    @Test
    fun notificaExpiracionEn401DeRutaProtegidaYLaIgnoraEnLogin() {
        val (_, protectedUnauthorizedCount) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/orders",
            token = "token_expirado",
            responseCode = 401
        )
        val (_, loginUnauthorizedCount) = ejecutarPeticion(
            url = "https://api.elegan.test/api/movil/auth/login",
            token = "token_expirado",
            responseCode = 401
        )
        val (_, externalUnauthorizedCount) = ejecutarPeticion(
            url = "https://cdn.externo.test/recurso",
            token = "token_expirado",
            existingAuthHeader = "Bearer externo",
            responseCode = 401
        )

        assertEquals(1, protectedUnauthorizedCount)
        assertEquals(0, loginUnauthorizedCount)
        assertEquals(0, externalUnauthorizedCount)
    }
}
