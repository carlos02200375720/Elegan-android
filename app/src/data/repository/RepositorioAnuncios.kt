package com.example.data.repository

import com.example.data.model.AnnouncementJson
import com.example.data.model.announcementActive
import com.example.data.model.announcementId
import com.example.data.model.announcementMessage
import com.example.data.model.announcementTitle
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioAnuncios {

    private val fetchMutex = Mutex()
    private val announcementsFlow = MutableStateFlow<List<AnnouncementJson>>(emptyList())

    fun observeAnnouncements(): Flow<List<AnnouncementJson>> = announcementsFlow.asStateFlow()

    suspend fun refreshAnnouncements(): List<AnnouncementJson> = fetchMutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                val response = ApiAutenticacionMovil.service.announcements()
                val items = extractAnnouncementsJson(response)
                announcementsFlow.value = items
                items
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarAdvertencia(
                    tag = "EleganAnnouncements",
                    mensaje = "No se pudieron obtener los anuncios del backend.",
                    error = e,
                    extras = mapOf("operation" to "refreshAnnouncements")
                )
                announcementsFlow.value
            }
        }
    }
}

internal fun extractAnnouncementsJson(response: Map<String, Any?>): List<AnnouncementJson> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["announcements"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val typed: AnnouncementJson = map.entries
            .mapNotNull { (k, v) -> k?.toString()?.let { it to v } }
            .toMap()
        val hasContent = typed.announcementTitle().isNotBlank() || typed.announcementMessage().isNotBlank()
        val hasIdOrContent = typed.announcementId().isNotBlank() || hasContent
        typed.takeIf { hasIdOrContent && hasContent && it.announcementActive() }
    }
}
