package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.resolveBackendMediaUrl
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.remote.MobileSession
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink

internal class RepositorioMedia(
    private val appContext: Context,
    private val apiProvider: () -> EleganAuthApi = { ApiAutenticacionMovil.uploadService }
) {

    suspend fun uploadMedia(mediaUri: Uri): String =
        uploadMediaInternal(mediaUri, supportAttachment = false)

    suspend fun uploadSupportAttachment(mediaUri: Uri): String =
        uploadMediaInternal(mediaUri, supportAttachment = true)

    private suspend fun uploadMediaInternal(
        mediaUri: Uri,
        supportAttachment: Boolean
    ): String = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)
            ?.takeIf { it.isNotBlank() && !it.startsWith("local_session_") }
            ?: throw IllegalStateException("Debes iniciar sesión para subir multimedia.")

        if (!supportAttachment) {
            val user = MobileSession.user(appContext)
            if (user == null || !user.canUploadMedia) {
                throw SecurityException("Solo usuarios con rol admin o superadmin pueden subir multimedia.")
            }
        }

        val resolver = appContext.contentResolver
        val mimeType = resolver.getType(mediaUri)?.takeIf { it.isNotBlank() }
            ?: "application/octet-stream"
        val rawFileName = resolveFileName(mediaUri) ?: if (mimeType.startsWith("video/")) {
            "upload_${System.currentTimeMillis()}.mp4"
        } else {
            "upload_${System.currentTimeMillis()}.jpg"
        }
        val fileName = rawFileName.replace("\"", "_").replace("\r", "_").replace("\n", "_")
        val body = StreamingUriRequestBody(
            contentResolver = resolver,
            uri = mediaUri,
            mediaType = mimeType.toMediaTypeOrNull(),
            knownSize = resolveContentLength(resolver, mediaUri)
        )
        val part = MultipartBody.Part.createFormData("file", fileName, body)

        val response = if (supportAttachment) {
            apiProvider().uploadSupportMedia(
                authorization = "Bearer $token",
                file = part
            )
        } else {
            apiProvider().uploadMedia(
                authorization = "Bearer $token",
                file = part
            )
        }
        extractUploadedMediaUrl(response)
            ?: throw IllegalStateException("El servidor no devolvió la URL del archivo subido.")
    }

    suspend fun uploadPaymentProof(orderId: String, mediaUri: Uri): String = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)
            ?: throw IllegalStateException("Debes iniciar sesión para enviar el comprobante.")
        val resolver = appContext.contentResolver
        val detectedMime = resolver.getType(mediaUri)?.lowercase().orEmpty()
        val mimeType = if (detectedMime.isNotBlank()) {
            detectedMime
        } else {
            val fileNameHint = (resolveFileName(mediaUri) ?: mediaUri.path.orEmpty()).lowercase()
            when {
                fileNameHint.endsWith(".jpg") || fileNameHint.endsWith(".jpeg") -> "image/jpeg"
                fileNameHint.endsWith(".png") -> "image/png"
                fileNameHint.endsWith(".webp") -> "image/webp"
                else -> ""
            }
        }
        if (mimeType !in setOf("image/jpeg", "image/png", "image/webp")) {
            throw IllegalArgumentException("Selecciona una imagen JPG, PNG o WEBP.")
        }
        val name = resolveFileName(mediaUri)?.replace("\"", "_") ?: "comprobante.jpg"
        val body = StreamingUriRequestBody(resolver, mediaUri, mimeType.toMediaTypeOrNull(), resolveContentLength(resolver, mediaUri))
        val part = MultipartBody.Part.createFormData("file", name, body)
        val response = try {
            apiProvider().uploadPaymentProof("Bearer $token", orderId, part)
        } catch (e: retrofit2.HttpException) {
            val errorBody = runCatching { e.response()?.errorBody()?.string() }.getOrNull().orEmpty()
            val message = when {
                errorBody.contains("PRIVATE_PROOF_STORAGE_NOT_CONFIGURED", ignoreCase = true) ->
                    "El servidor todavía no tiene configurado el almacenamiento privado de comprobantes."
                errorBody.contains("PROOF_TOO_LARGE", ignoreCase = true) ->
                    "La imagen del comprobante no puede superar 10 MB."
                errorBody.contains("PROOF_EXPIRED", ignoreCase = true) ->
                    "El plazo para enviar este comprobante venció. Contacta a Elegan para continuar."
                else -> "No se pudo enviar el comprobante. Inténtalo de nuevo."
            }
            throw IllegalStateException(message, e)
        }
        (response["success"] as? Boolean)?.let { if (!it) throw IllegalStateException("No se pudo guardar el comprobante.") }
        response["paymentProofUrl"] as? String ?: response["url"] as? String ?: "uploaded"
    }

    private fun resolveFileName(uri: Uri): String? {
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            uri.lastPathSegment?.takeIf { it.isNotBlank() }?.let { return it }
        }
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

    private fun resolveContentLength(resolver: ContentResolver, uri: Uri): Long {
        if ("file".equals(uri.scheme, ignoreCase = true)) {
            val fileLength = uri.path?.let(::File)?.takeIf { it.isFile }?.length()
            if (fileLength != null && fileLength >= 0L) return fileLength
        }
        try {
            resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !cursor.isNull(idx)) {
                        val size = cursor.getLong(idx)
                        if (size >= 0L) return size
                    }
                }
            }
        } catch (_: Exception) {
        }
        return try {
            resolver.openAssetFileDescriptor(uri, "r")?.use { afd ->
                val len = afd.length
                if (len != AssetFileDescriptor.UNKNOWN_LENGTH && len >= 0L) len else -1L
            } ?: -1L
        } catch (_: Exception) {
            -1L
        }
    }
}

private class StreamingUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?,
    private val knownSize: Long
) : RequestBody() {

    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = if (knownSize >= 0L) knownSize else -1L

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: throw IOException("No se pudo leer el archivo multimedia seleccionado.")
        inputStream.use { stream ->
            val buffer = ByteArray(STREAM_BUFFER_BYTES)
            while (true) {
                val read = stream.read(buffer)
                if (read == -1) break
                sink.write(buffer, 0, read)
                sink.emit()
            }
        }
    }

    private companion object {
        private const val STREAM_BUFFER_BYTES = 64 * 1024
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
