package com.example.data.repository

import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.model.Announcement
import com.example.data.model.toAnnouncement
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class RepositorioAnuncios(
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.service }
) {

    private val fetchMutex = Mutex()
    private val announcementsFlow = MutableStateFlow<List<Announcement>>(emptyList())
    private val lastErrorFlow = MutableStateFlow<ErrorApi?>(null)

    fun observeAnnouncements(): Flow<List<Announcement>> = announcementsFlow.asStateFlow()

    fun observeApiError(): Flow<ErrorApi?> = lastErrorFlow.asStateFlow()

    suspend fun refreshAnnouncementsResult(): ResultWrapper<List<Announcement>> = fetchMutex.withLock {
        val result = safeApiCall(
            tag = "EleganAnnouncements",
            operation = "refreshAnnouncements"
        ) {
            val response = apiProvider().announcements()
            extractAnnouncementsJson(response)
        }
        when (result) {
            is ResultWrapper.Success -> {
                announcementsFlow.value = result.data
                lastErrorFlow.value = null
            }
            is ResultWrapper.Error -> {
                lastErrorFlow.value = result.error
            }
            ResultWrapper.Loading -> Unit
        }
        result
    }

    suspend fun refreshAnnouncements(): List<Announcement> =
        refreshAnnouncementsResult().getOrElse { announcementsFlow.value }
}

internal fun extractAnnouncementsJson(response: Map<String, Any?>): List<Announcement> {
    val rawList = (response["items"] as? List<*>)
        ?: (response["announcements"] as? List<*>)
        ?: (response["data"] as? List<*>)
        ?: return emptyList()

    return rawList.mapNotNull { entry ->
        val map = entry as? Map<*, *> ?: return@mapNotNull null
        val parsed = map.toAnnouncement()
        val hasContent = parsed.title.isNotBlank() || parsed.message.isNotBlank()
        val hasIdOrContent = parsed.id.isNotBlank() || hasContent
        parsed.takeIf { hasIdOrContent && hasContent && it.active }
    }
}
