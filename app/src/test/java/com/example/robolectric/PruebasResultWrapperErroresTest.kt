package com.example

import com.example.data.error.ApiParsingException
import com.example.data.error.ApiResultException
import com.example.data.error.ErrorApi
import com.example.data.error.MapeadorErroresApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.repository.RepositorioAnuncios
import com.example.data.repository.RepositorioCarrito
import com.example.data.repository.RepositorioCatalogo
import com.example.data.repository.RepositorioCategorias
import com.example.data.repository.RepositorioPedidos
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasResultWrapperErroresTest {

    private fun httpError(code: Int, body: String): HttpException {
        val responseBody = body.toResponseBody("application/json".toMediaType())
        return HttpException(Response.error<Map<String, Any?>>(code, responseBody))
    }

    @Test
    fun `MapeadorErroresApi clasifica red, timeout, servidor, cliente, auth, parsing y desconocido`() {
        val netError = MapeadorErroresApi.mapear(IOException("Unable to resolve host"))
        assertTrue(netError is ErrorApi.NetworkError)
        assertFalse((netError as ErrorApi.NetworkError).isTimeout)
        assertTrue(netError.isRetryable)
        assertEquals("network", netError.errorCategory)

        val timeoutError = MapeadorErroresApi.mapear(SocketTimeoutException("Read timed out"))
        assertTrue(timeoutError is ErrorApi.NetworkError)
        assertTrue((timeoutError as ErrorApi.NetworkError).isTimeout)
        assertEquals(ErrorApi.DEFAULT_TIMEOUT_MESSAGE, timeoutError.userMessage)

        val serverError = MapeadorErroresApi.mapear(httpError(503, """{"code":"SERVICE_DOWN","message":"Mantenimiento"}"""))
        assertTrue(serverError is ErrorApi.ServerError)
        assertEquals(503, (serverError as ErrorApi.ServerError).httpCode)
        assertEquals("SERVICE_DOWN", serverError.backendCode)
        assertTrue(serverError.isRetryable)
        assertEquals("server", serverError.errorCategory)

        val client422 = MapeadorErroresApi.mapear(httpError(422, """{"detail":"INVALID_FIELD"}"""))
        assertTrue(client422 is ErrorApi.ClientError)
        assertEquals(422, (client422 as ErrorApi.ClientError).httpCode)
        assertEquals("INVALID_FIELD", client422.backendCode)
        assertFalse(client422.isRetryable)

        val client429 = MapeadorErroresApi.mapear(httpError(429, """{"detail":"RATE_LIMITED"}"""))
        assertTrue(client429 is ErrorApi.ClientError)
        assertTrue(client429.isRetryable)

        val auth401 = MapeadorErroresApi.mapear(httpError(401, """{"detail":"TOKEN_EXPIRED"}"""))
        assertTrue(auth401 is ErrorApi.UnauthorizedError)
        assertFalse(auth401.isRetryable)
        assertEquals("unauthorized", auth401.errorCategory)

        listOf(
            JsonDataException("Missing required field"),
            JsonEncodingException("Malformed JSON"),
            SerializationException("Unexpected token"),
            JSONException("Unterminated object"),
            ApiParsingException("Payload inválido", fieldOrContext = "products.items")
        ).forEach { parseThrowable ->
            val mapped = MapeadorErroresApi.mapear(parseThrowable)
            assertTrue("Debe mapear ${parseThrowable.javaClass.simpleName} como ParsingError", mapped is ErrorApi.ParsingError)
            assertFalse(mapped.isRetryable)
            assertEquals("parsing", mapped.errorCategory)
        }

        val unknown = MapeadorErroresApi.mapear(IllegalArgumentException("Estado inesperado"))
        assertTrue(unknown is ErrorApi.UnknownError)
        assertEquals("Estado inesperado", unknown.userMessage)
    }

    @Test
    fun `safeApiCall y ResultWrapper propagan CancellationException y ofrecen combinadores seguros`() = runBlocking {
        val ok: ResultWrapper<Int> = safeApiCall("TestTag", "okOp") { 21 }
        val doubled = ok.map { it * 2 }
        assertTrue(doubled.isSuccess)
        assertEquals(42, doubled.getOrNull())
        assertNull(doubled.errorOrNull())

        var capturedError: ErrorApi? = null
        val failed: ResultWrapper<Int> = safeApiCall("TestTag", "failOp") {
            throw SocketTimeoutException("Timeout")
        }
        failed.onError { capturedError = it }
        assertTrue(failed.isError)
        assertTrue(capturedError is ErrorApi.NetworkError)
        assertEquals(-1, failed.getOrElse { -1 })

        var threwApiResult = false
        try {
            failed.getOrThrow()
        } catch (e: ApiResultException) {
            threwApiResult = e.error is ErrorApi.NetworkError
        }
        assertTrue(threwApiResult)

        var cancellationRethrown = false
        try {
            safeApiCall("TestTag", "cancelOp") {
                throw CancellationException("Cancelado")
            }
        } catch (_: CancellationException) {
            cancellationRethrown = true
        }
        assertTrue("CancellationException nunca debe ser absorbida por safeApiCall", cancellationRethrown)
    }

    @Test
    fun `repositorios estandarizan errores de red, servidor y parsing mediante ResultWrapper`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()

        // 1. RepositorioCatalogo: servidor 500 -> ServerError, payload corrupto -> ParsingError
        val server500Api = object : BaseFakeEleganAuthApi() {
            override suspend fun products(): Map<String, Any?> =
                throw httpError(500, """{"detail":"INTERNAL_ERROR"}""")

            override suspend fun productById(productId: String): Map<String, Any?> =
                mapOf("unexpected" to "sin_id_ni_nombre")
        }
        val catalogoRepo = RepositorioCatalogo(apiProvider = { server500Api })
        val catalogRefresh = catalogoRepo.refreshProductsResult()
        assertTrue(catalogRefresh is ResultWrapper.Error)
        assertTrue((catalogRefresh as ResultWrapper.Error).error is ErrorApi.ServerError)
        assertTrue(catalogoRepo.observeApiError().first() is ErrorApi.ServerError)

        val detailResult = catalogoRepo.getProductByIdResult("prod_broken")
        assertTrue(detailResult is ResultWrapper.Error)
        assertTrue((detailResult as ResultWrapper.Error).error is ErrorApi.ParsingError)

        // 2. RepositorioCategorias: payload inválido -> ParsingError preservando lista previa válida
        var returnMalformedCategories = false
        val categoriasRepo = RepositorioCategorias(
            fetchRemotePayload = {
                if (returnMalformedCategories) {
                    mapOf("unexpected_key" to 123)
                } else {
                    mapOf("items" to listOf(mapOf("id" to "trajes", "title" to "Trajes")))
                }
            }
        )
        assertTrue(categoriasRepo.fetchCategoriesResult(force = true) is ResultWrapper.Success)
        assertEquals(1, categoriasRepo.observeCategories().first().size)

        returnMalformedCategories = true
        val malformedCatResult = categoriasRepo.fetchCategoriesResult(force = true)
        assertTrue(malformedCatResult is ResultWrapper.Error)
        assertTrue((malformedCatResult as ResultWrapper.Error).error is ErrorApi.ParsingError)
        assertEquals(
            "La lista previa válida debe preservarse ante ParsingError",
            1,
            categoriasRepo.observeCategories().first().size
        )

        // 3. RepositorioAnuncios: error de red -> NetworkError
        val offlineApi = object : BaseFakeEleganAuthApi() {
            override suspend fun announcements(): Map<String, Any?> = throw IOException("Offline")
            override suspend fun getCart(authorization: String): Map<String, Any?> =
                throw httpError(401, """{"detail":"UNAUTHORIZED"}""")
            override suspend fun getOrders(authorization: String): Map<String, Any?> =
                throw SocketTimeoutException("Timeout orders")
        }
        val anunciosRepo = RepositorioAnuncios(apiProvider = { offlineApi })
        val annResult = anunciosRepo.refreshAnnouncementsResult()
        assertTrue(annResult is ResultWrapper.Error)
        assertTrue((annResult as ResultWrapper.Error).error is ErrorApi.NetworkError)

        // 4. RepositorioCarrito: HTTP 401 -> UnauthorizedError
        val carritoRepo = RepositorioCarrito(
            appContext = context,
            apiProvider = { offlineApi },
            tokenProvider = { "valid_jwt_token" }
        )
        val cartResult = carritoRepo.refreshCartResult()
        assertTrue(cartResult is ResultWrapper.Error)
        assertTrue((cartResult as ResultWrapper.Error).error is ErrorApi.UnauthorizedError)
        assertTrue(carritoRepo.observeCartApiError().first() is ErrorApi.UnauthorizedError)

        // 5. RepositorioPedidos: timeout -> NetworkError (isTimeout = true)
        val pedidosRepo = RepositorioPedidos(
            appContext = context,
            carritoRepo = carritoRepo,
            apiProvider = { offlineApi },
            tokenProvider = { "valid_jwt_token" }
        )
        val ordersResult = pedidosRepo.refreshOrdersResult()
        assertTrue(ordersResult is ResultWrapper.Error)
        val ordersError = (ordersResult as ResultWrapper.Error).error
        assertTrue(ordersError is ErrorApi.NetworkError && ordersError.isTimeout)
        assertTrue(pedidosRepo.observeOrdersApiError().first() is ErrorApi.NetworkError)
    }
}
