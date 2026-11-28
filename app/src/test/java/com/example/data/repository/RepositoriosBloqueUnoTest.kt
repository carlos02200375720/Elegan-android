package com.example.data.repository

import com.example.data.remote.RestablecerContrasenaRequest
import com.example.data.remote.SolicitarRecuperacionRequest
import com.example.BaseFakeEleganAuthApi
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class RepositoriosBloqueUnoTest {

    @Test
    fun repositorioDocumentosLegalesMapeaDocumentoPublicadoYNoEncontrado() = runBlocking {
        val fakeApi = object : BaseFakeEleganAuthApi() {
            override suspend fun getLegalDocuments(): Map<String, Any?> = mapOf(
                "items" to listOf(
                    mapOf(
                        "type" to "privacy",
                        "title" to "Política de Privacidad",
                        "subtitle" to "Actualizada 2026",
                        "content" to "Contenido oficial de privacidad"
                    )
                )
            )
        }
        val repo = RepositorioDocumentosLegales(apiProvider = { fakeApi })

        val encontrado = repo.obtenerDocumento("privacy")
        assertTrue(encontrado is EstadoDocumentoLegal.Exito)
        val doc = (encontrado as EstadoDocumentoLegal.Exito).documento
        assertEquals("Política de Privacidad", doc.title)
        assertEquals("Actualizada 2026", doc.subtitle)
        assertEquals("Contenido oficial de privacidad", doc.content)

        val faltante = repo.obtenerDocumento("terms")
        assertTrue(faltante is EstadoDocumentoLegal.Error)
    }

    @Test
    fun repositorioAutenticacionRecortaCorreoYMapeaErroresDeRecuperacion() = runBlocking {
        var capturedEmail: String? = null
        var capturedReset: RestablecerContrasenaRequest? = null

        val fakeApi = object : BaseFakeEleganAuthApi() {
            override suspend fun requestPasswordReset(
                request: SolicitarRecuperacionRequest
            ): Map<String, Any?> {
                capturedEmail = request.email
                return mapOf("ok" to true)
            }

            override suspend fun resetPassword(
                request: RestablecerContrasenaRequest
            ): Map<String, Any?> {
                capturedReset = request
                val errorBody = """{"error":"expired"}""".toResponseBody("application/json".toMediaType())
                throw HttpException(Response.error<Map<String, Any?>>(400, errorBody))
            }
        }

        val repo = RepositorioAutenticacion(apiProvider = { fakeApi })
        repo.solicitarCodigoRecuperacion("  cliente@elegan.test  ")
        assertEquals("cliente@elegan.test", capturedEmail)

        val fallo = runCatching {
            repo.restablecerContrasena("  cliente@elegan.test ", " 123456 ", "NuevaClave123")
        }.exceptionOrNull()

        assertEquals("cliente@elegan.test", capturedReset?.email)
        assertEquals("123456", capturedReset?.code)
        assertTrue(fallo is IllegalStateException)
        assertTrue(fallo?.message?.contains("vencido") == true)
    }
}
