package com.example.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.observability.MonitorErrores
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Modelo tipado e inmutable de preferencias persistentes del usuario gestionadas por Jetpack DataStore.
 */
@Immutable
data class PreferenciasUsuario(
    val visitorId: String = "",
    val lastSelectedCategory: String? = null,
    val isGuestMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val lastCatalogSyncMillis: Long = 0L
)

/**
 * Almacén asíncrono de preferencias de usuario basado en Jetpack Preferences [DataStore].
 *
 * Migra automáticamente la clave heredada `visitor_id` desde `elegan_visitor_shares`
 * y ofrece flujos reactivos [Flow] y operaciones `suspend` libres de bloqueo en el hilo principal.
 */
object AlmacenPreferenciasUsuario {
    internal val KEY_VISITOR_ID = stringPreferencesKey("visitor_id")
    internal val KEY_LAST_SELECTED_CATEGORY = stringPreferencesKey("last_selected_category")
    internal val KEY_IS_GUEST_MODE = booleanPreferencesKey("is_guest_mode")
    internal val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    internal val KEY_LAST_CATALOG_SYNC_MILLIS = longPreferencesKey("last_catalog_sync_millis")

    private val visitorIdMutex = Mutex()

    private fun store(context: Context): DataStore<Preferences> =
        AlmacenDataStoreElegan.obtenerDataStorePreferencias(
            context = context,
            storeName = AlmacenDataStoreElegan.USER_PREFS_STORE_NAME,
            legacySharedPrefsName = AlmacenDataStoreElegan.LEGACY_VISITOR_SHARES_PREFS
        )

    fun observarPreferencias(context: Context): Flow<PreferenciasUsuario> =
        observarPreferenciasDesde(store(context))

    internal fun observarPreferenciasDesde(dataStore: DataStore<Preferences>): Flow<PreferenciasUsuario> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    MonitorErrores.registrarAdvertencia(
                        tag = "PreferenciasUsuario",
                        mensaje = "Error de lectura en DataStore de preferencias; se emiten valores por defecto.",
                        error = error
                    )
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs -> mapearPreferencias(prefs) }
            .distinctUntilChanged()

    suspend fun leerPreferencias(context: Context): PreferenciasUsuario =
        observarPreferencias(context).first()

    /**
     * Obtiene el identificador persistente de visitante (para analíticas de compartidos como invitado)
     * o genera y guarda uno nuevo de forma atómica y segura ante concurrencia.
     */
    suspend fun obtenerOGenerarVisitorId(context: Context): String =
        obtenerOGenerarVisitorIdDesde(store(context))

    internal suspend fun obtenerOGenerarVisitorIdDesde(dataStore: DataStore<Preferences>): String =
        visitorIdMutex.withLock {
            val current = observarPreferenciasDesde(dataStore).first().visitorId
            if (current.isNotBlank()) return@withLock current
            val created = UUID.randomUUID().toString()
            dataStore.edit { prefs ->
                val existing = prefs[KEY_VISITOR_ID]?.takeIf { it.isNotBlank() }
                if (existing == null) {
                    prefs[KEY_VISITOR_ID] = created
                }
            }
            observarPreferenciasDesde(dataStore).first().visitorId.ifBlank { created }
        }

    suspend fun guardarUltimaCategoria(context: Context, categoryId: String?) {
        guardarUltimaCategoriaEn(store(context), categoryId)
    }

    internal suspend fun guardarUltimaCategoriaEn(
        dataStore: DataStore<Preferences>,
        categoryId: String?
    ) {
        dataStore.edit { prefs ->
            val clean = categoryId?.trim()?.takeIf { it.isNotBlank() }
            if (clean == null) {
                prefs.remove(KEY_LAST_SELECTED_CATEGORY)
            } else {
                prefs[KEY_LAST_SELECTED_CATEGORY] = clean
            }
        }
    }

    suspend fun guardarModoInvitado(context: Context, isGuest: Boolean) {
        guardarModoInvitadoEn(store(context), isGuest)
    }

    internal suspend fun guardarModoInvitadoEn(
        dataStore: DataStore<Preferences>,
        isGuest: Boolean
    ) {
        dataStore.edit { prefs ->
            prefs[KEY_IS_GUEST_MODE] = isGuest
        }
    }

    suspend fun guardarNotificacionesActivas(context: Context, enabled: Boolean) {
        store(context).edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun guardarUltimaSincronizacionCatalogo(context: Context, timestampMillis: Long) {
        store(context).edit { prefs ->
            prefs[KEY_LAST_CATALOG_SYNC_MILLIS] = timestampMillis.coerceAtLeast(0L)
        }
    }

    suspend fun limpiarPreferencias(context: Context, conservarVisitorId: Boolean = true) {
        limpiarPreferenciasEn(store(context), conservarVisitorId)
    }

    internal suspend fun limpiarPreferenciasEn(
        dataStore: DataStore<Preferences>,
        conservarVisitorId: Boolean = true
    ) {
        dataStore.edit { prefs ->
            val currentVisitorId = if (conservarVisitorId) prefs[KEY_VISITOR_ID] else null
            prefs.clear()
            if (!currentVisitorId.isNullOrBlank()) {
                prefs[KEY_VISITOR_ID] = currentVisitorId
            }
        }
    }

    private fun mapearPreferencias(prefs: Preferences): PreferenciasUsuario =
        PreferenciasUsuario(
            visitorId = prefs[KEY_VISITOR_ID].orEmpty(),
            lastSelectedCategory = prefs[KEY_LAST_SELECTED_CATEGORY]?.takeIf { it.isNotBlank() },
            isGuestMode = prefs[KEY_IS_GUEST_MODE] ?: false,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            lastCatalogSyncMillis = prefs[KEY_LAST_CATALOG_SYNC_MILLIS] ?: 0L
        )
}
