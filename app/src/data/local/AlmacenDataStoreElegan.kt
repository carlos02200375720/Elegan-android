package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.data.observability.MonitorErrores
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Fábrica y registro centralizado de instancias singleton de Jetpack Preferences [DataStore].
 *
 * Garantiza una única instancia activa por archivo canónico dentro del proceso (requisito estricto
 * de DataStore), recuperación automática ante corrupción mediante [ReplaceFileCorruptionHandler]
 * y migración transparente única desde [android.content.SharedPreferences] heredados.
 */
object AlmacenDataStoreElegan {
    const val SESSION_STORE_NAME = "elegan_mobile_session"
    const val TRANSFER_LOCK_STORE_NAME = "elegan_transfer_lock_state"
    const val USER_PREFS_STORE_NAME = "elegan_user_preferences"
    const val LEGACY_VISITOR_SHARES_PREFS = "elegan_visitor_shares"

    private val registry = ConcurrentHashMap<String, DataStore<Preferences>>()

    internal fun claveArchivo(context: Context, storeName: String): String {
        val file = context.applicationContext.preferencesDataStoreFile(storeName)
        return runCatching { file.canonicalPath }.getOrDefault(file.absolutePath)
    }

    /**
     * Obtiene o crea la instancia singleton de [DataStore] asociada a [storeName].
     * Si [legacySharedPrefsName] no es nulo, configura [SharedPreferencesMigration] para migrar
     * automáticamente los datos de instalaciones previas sin pérdida de estado.
     */
    fun obtenerDataStorePreferencias(
        context: Context,
        storeName: String,
        legacySharedPrefsName: String? = null
    ): DataStore<Preferences> {
        val appContext = context.applicationContext
        val targetFile = appContext.preferencesDataStoreFile(storeName)
        val key = runCatching { targetFile.canonicalPath }.getOrDefault(targetFile.absolutePath)

        return registry.computeIfAbsent(key) {
            crearInstanciaDataStore(
                appContext = appContext,
                storeName = storeName,
                targetFile = targetFile,
                legacySharedPrefsName = legacySharedPrefsName
            )
        }
    }

    internal fun crearDataStoreAislado(
        context: Context,
        storeName: String,
        legacySharedPrefsName: String? = null,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ): DataStore<Preferences> {
        val appContext = context.applicationContext
        val targetFile = appContext.preferencesDataStoreFile(storeName)
        val key = runCatching { targetFile.canonicalPath }.getOrDefault(targetFile.absolutePath)
        return registry.computeIfAbsent(key) {
            crearInstanciaDataStore(
                appContext = appContext,
                storeName = storeName,
                targetFile = targetFile,
                legacySharedPrefsName = legacySharedPrefsName,
                scope = scope
            )
        }
    }

    private fun crearInstanciaDataStore(
        appContext: Context,
        storeName: String,
        targetFile: File,
        legacySharedPrefsName: String?,
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    ): DataStore<Preferences> {
        val migrations = if (!legacySharedPrefsName.isNullOrBlank()) {
            listOf(SharedPreferencesMigration(appContext, legacySharedPrefsName))
        } else {
            emptyList()
        }

        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { error ->
                MonitorErrores.registrarAdvertencia(
                    tag = "EleganDataStore",
                    mensaje = "Archivo DataStore '$storeName' corrupto; se restablece con preferencias vacías.",
                    error = error,
                    extras = mapOf("storeName" to storeName)
                )
                emptyPreferences()
            },
            migrations = migrations,
            scope = scope,
            produceFile = { targetFile }
        )
    }
}
