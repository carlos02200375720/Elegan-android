package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

internal class RepositorioMedia(private val appContext: Context) {

    suspend fun uploadMedia(mediaUri: Uri): String = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)
            ?: throw IllegalStateException("Debes iniciar sesión para subir multimedia.")
        val user = MobileSession.user(appContext)
        if (user == null || !user.canUploadMedia) {
            throw SecurityException("Solo usuarios con rol admin o superadmin pueden subir multimedia.")
        }

        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(mediaUri)?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
        val fileName = resolveFileName(mediaUri) ?: if (mimeType.startsWith("video/")) {
            "upload_${System.currentTimeMillis()}.mp4"
        } else {
            "upload_${System.currentTimeMillis()}.jpg"
        }

        val bytes = resolver.openInputStream(mediaUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo multimedia seleccionado.")

        val body = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, body)

        val response = ApiAutenticacionMovil.service.uploadMedia(
            authorization = "Bearer $token",
            file = part
        )
        extractUploadedMediaUrl(response)
            ?: throw IllegalStateException("El servidor no devolvió la URL del archivo subido.")
    }

    private fun resolveFileName(uri: Uri): String? {
        return try {
            appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx)?.takeIf { it.isNotBlank() } else null
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}

internal fun extractUploadedMediaUrl(response: Map<String, Any?>): String? {
    val nested = (response["item"] as? Map<*, *>)
        ?: (response["media"] as? Map<*, *>)
        ?: (response["data"] as? Map<*, *>)
        ?: response
    val rawUrl = listOf("url", "mediaUrl", "media_url", "fileUrl", "file_url", "path")
        .firstNotNullOfOrNull { key ->
            (nested[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
                ?: (response[key] as? String)?.trim()?.takeIf { it.isNotBlank() }
        }
        ?: return null
    return resolveBackendMediaUrl(rawUrl)
}
