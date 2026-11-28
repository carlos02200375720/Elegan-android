package com.example.data.repository

import com.example.data.model.LegalDocument
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.RealtimeEvents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

sealed interface EstadoDocumentoLegal {
    data object Cargando : EstadoDocumentoLegal
    data class Exito(val documento: LegalDocument) : EstadoDocumentoLegal
    data class Error(val mensaje: String) : EstadoDocumentoLegal
}

/**
 * Repositorio dedicado a documentos legales publicados por el backend (Política de Privacidad
 * y Términos y Condiciones), desacoplando los diálogos de Compose de Retrofit y WebSockets.
 */
class RepositorioDocumentosLegales internal constructor(
    private val apiProvider: () -> EleganAuthApi
) {
    constructor() : this(apiProvider = { ApiAutenticacionMovil.service })

    suspend fun obtenerDocumento(documentType: String): EstadoDocumentoLegal = withContext(Dispatchers.IO) {
        try {
            val response = apiProvider().getLegalDocuments()
            val items = response["items"] as? List<*> ?: emptyList<Any?>()
            val item = items.filterIsInstance<Map<*, *>>()
                .firstOrNull { it["type"]?.toString() == documentType }
            if (item == null) {
                EstadoDocumentoLegal.Error("El documento todavía no está publicado.")
            } else {
                EstadoDocumentoLegal.Exito(
                    LegalDocument(
                        title = item["title"]?.toString().orEmpty(),
                        subtitle = item["subtitle"]?.toString().orEmpty(),
                        content = item["content"]?.toString().orEmpty()
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia(
                tag = "LegalDocs",
                mensaje = "No se pudo cargar el documento legal $documentType.",
                error = e
            )
            EstadoDocumentoLegal.Error(
                "No se pudo cargar el documento. Comprueba tu conexión e inténtalo de nuevo."
            )
        }
    }

    fun observarDocumento(documentType: String): Flow<EstadoDocumentoLegal> = flow {
        emit(EstadoDocumentoLegal.Cargando)
        emit(obtenerDocumento(documentType))
        RealtimeEvents.events.collect { event ->
            if (event.collection == "legal_documents") {
                emit(EstadoDocumentoLegal.Cargando)
                emit(obtenerDocumento(documentType))
            }
        }
    }
}
