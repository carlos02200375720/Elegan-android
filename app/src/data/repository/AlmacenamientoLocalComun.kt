package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.example.data.remote.MobileSession
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal fun optionalUserId(appContext: Context): String? = MobileSession.userId(appContext)

internal fun userCacheKey(appContext: Context): String {
    val user = MobileSession.user(appContext)
    val email = user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
    val id = user?.effectiveId?.trim()?.takeIf { it.isNotBlank() }
    return email ?: id ?: "guest"
}

internal fun nowIsoUtc(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date())

internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (i in 0 until length()) {
            val value = optString(i, "")
            if (value.isNotBlank()) add(value)
        }
    }
}

internal suspend fun persistirArchivoMultimediaLocal(
    appContext: Context,
    source: String,
    entityId: String,
    label: String
): String {
    if (source.isBlank() || source.startsWith("android.resource://")) return source
    return withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(source)
            val root = File(appContext.filesDir, "elegan_local_media").apply { if (!exists()) mkdirs() }
            if (uri.scheme == "file") {
                val existing = File(uri.path ?: "")
                if (existing.exists() && existing.absolutePath.startsWith(root.absolutePath)) {
                    return@withContext source
                }
            }
            val isVideo = label.contains("video", ignoreCase = true) || source.endsWith(".mp4", true)
            val ext = if (isVideo) "mp4" else "jpg"
            val dest = File(root, "${entityId}_${label}_${System.currentTimeMillis()}.$ext")
            copiarUriAArchivo(appContext, uri, source, dest)
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo guardar archivo multimedia local: ${e.message}")
            source
        }
    }
}

internal suspend fun persistirMediaSoporteLocal(
    appContext: Context,
    source: String,
    msgId: String,
    isVideo: Boolean
): String {
    if (source.isBlank() || source.startsWith("android.resource://")) return source
    return withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(source)
            if (uri.scheme == "file" && uri.path?.let { File(it).exists() } == true) {
                return@withContext source
            }
            val ext = if (isVideo) "mp4" else "jpg"
            val dir = File(appContext.filesDir, "support_media").apply { if (!exists()) mkdirs() }
            val dest = File(dir, "$msgId.$ext")
            copiarUriAArchivo(appContext, uri, source, dest)
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo copiar medio local del chat: ${e.message}")
            source
        }
    }
}

private fun copiarUriAArchivo(
    appContext: Context,
    uri: Uri,
    source: String,
    destFile: File
): String {
    val input = when (uri.scheme) {
        "content" -> appContext.contentResolver.openInputStream(uri)
        "file" -> File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
        else -> File(source).takeIf { it.exists() }?.inputStream()
    }
    return if (input != null) {
        input.use { src -> destFile.outputStream().use { dst -> src.copyTo(dst) } }
        Uri.fromFile(destFile).toString()
    } else {
        source
    }
}

internal fun loadBooleanOverrideMap(prefs: SharedPreferences, key: String): Map<String, Boolean> {
    val result = mutableMapOf<String, Boolean>()
    try {
        val raw = prefs.getString(key, null)
        if (!raw.isNullOrBlank()) {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val id = keys.next()
                if (id.isNotBlank()) result[id] = obj.optBoolean(id, true)
            }
        }
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron leer los estados locales ($key): ${e.message}")
    }
    return result
}

internal fun saveBooleanOverrideMap(prefs: SharedPreferences, key: String, map: Map<String, Boolean>) {
    try {
        val obj = JSONObject()
        map.forEach { (id, state) -> obj.put(id, state) }
        prefs.edit().putString(key, obj.toString()).commit()
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudieron guardar los estados locales ($key): ${e.message}")
    }
}

internal fun loadPersistedIdSet(prefs: SharedPreferences, key: String): Set<String> {
    val raw = prefs.getString(key, null) ?: return emptySet()
    return try {
        val array = JSONArray(raw)
        buildSet {
            for (i in 0 until array.length()) {
                val id = array.optString(i, "").trim()
                if (id.isNotBlank()) add(id)
            }
        }
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudo leer conjunto local ($key): ${e.message}")
        emptySet()
    }
}

internal fun savePersistedIdSet(prefs: SharedPreferences, key: String, ids: Set<String>) {
    try {
        val array = JSONArray()
        ids.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).commit()
    } catch (e: Exception) {
        Log.w("EleganRepository", "No se pudo guardar conjunto local ($key): ${e.message}")
    }
}
