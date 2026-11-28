package com.example.data.remote

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.data.local.AlmacenDataStoreElegan
import com.example.data.local.CifradoKeystoreAesGcm
import com.example.data.observability.MonitorErrores
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Snapshot inmutable del estado de sesión persistido en Jetpack DataStore.
 */
@Immutable
data class EstadoSesionMovil(
    val token: String? = null,
    val userId: String? = null,
    val user: AuthUser? = null
) {
    val isAuthenticated: Boolean
        get() = user != null && !token.isNullOrBlank() && !token.startsWith("local_session_")
}

/**
 * Almacenamiento cifrado y asíncrono de sesión móvil respaldado por Jetpack Preferences [DataStore]
 * y Android Keystore (AES-256-GCM) mediante [CifradoKeystoreAesGcm].
 *
 * - Migra automáticamente los datos previos desde `SharedPreferences("elegan_mobile_session")`.
 * - Expone flujos reactivos [Flow] y operaciones `suspend` que nunca bloquean el hilo principal.
 * - Mantiene una caché en memoria sincronizada para lecturas ultrarrápidas desde interceptores HTTP.
 */
internal object AlmacenCifradoSesion {
    internal val KEY_TOKEN = stringPreferencesKey("auth_token")
    internal val KEY_USER_ID = stringPreferencesKey("user_id")
    internal val KEY_EMAIL = stringPreferencesKey("email")
    internal val KEY_NAME = stringPreferencesKey("name")
    internal val KEY_PHOTO = stringPreferencesKey("photo_url")
    internal val KEY_ROLE = stringPreferencesKey("role")
    internal val KEY_PROVIDER = stringPreferencesKey("provider")

    private const val KEY_ALIAS = "elegan_mobile_session_aes_key_v1"
    private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    private val cifrador = CifradoKeystoreAesGcm(
        keyAlias = KEY_ALIAS,
        legacyAliases = listOf(LEGACY_MASTER_KEY_ALIAS)
    )
    private val writeMutex = Mutex()
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val decryptedFieldCache = ConcurrentHashMap<String, String>()
    private val sessionCache = ConcurrentHashMap<String, EstadoSesionMovil>()

    private fun store(context: Context): DataStore<Preferences> =
        AlmacenDataStoreElegan.obtenerDataStorePreferencias(
            context = context,
            storeName = AlmacenDataStoreElegan.SESSION_STORE_NAME,
            legacySharedPrefsName = AlmacenDataStoreElegan.SESSION_STORE_NAME
        )

    private fun cacheKey(context: Context): String =
        AlmacenDataStoreElegan.claveArchivo(context, AlmacenDataStoreElegan.SESSION_STORE_NAME)

    fun observeSession(context: Context): Flow<EstadoSesionMovil> {
        val dataStore = store(context)
        val key = cacheKey(context)
        return observeSessionFrom(dataStore = dataStore, cacheKey = key)
    }

    internal fun observeSessionFrom(
        dataStore: DataStore<Preferences>,
        cacheKey: String? = null
    ): Flow<EstadoSesionMovil> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    MonitorErrores.registrarAdvertencia(
                        tag = "MobileSession",
                        mensaje = "Error de lectura en DataStore de sesión; se emite estado vacío.",
                        error = error
                    )
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { prefs ->
                decodificarEstadoSesion(
                    prefs = prefs,
                    dataStore = dataStore,
                    cacheKey = cacheKey
                )
            }
            .distinctUntilChanged()

    fun observeUser(context: Context): Flow<AuthUser?> =
        observeSession(context).map { it.user }.distinctUntilChanged()

    fun observeToken(context: Context): Flow<String?> =
        observeSession(context).map { it.token }.distinctUntilChanged()

    suspend fun readSessionAsync(context: Context): EstadoSesionMovil = withContext(Dispatchers.IO) {
        observeSession(context).first()
    }

    suspend fun tokenAsync(context: Context): String? = readSessionAsync(context).token

    suspend fun userIdAsync(context: Context): String? = readSessionAsync(context).userId

    suspend fun userAsync(context: Context): AuthUser? = readSessionAsync(context).user

    suspend fun saveAsync(context: Context, response: AuthResponse) {
        saveTo(
            dataStore = store(context),
            response = response,
            cacheKey = cacheKey(context)
        )
    }

    internal suspend fun saveTo(
        dataStore: DataStore<Preferences>,
        response: AuthResponse,
        cacheKey: String? = null
    ) = withContext(Dispatchers.IO) {
        require(response.token.isNotBlank()) { "El backend devolvió un token vacío." }
        require(response.user.effectiveId.isNotBlank()) { "El backend devolvió un usuario sin identificador." }

        val role = if (
            response.user.isAdmin ||
            response.user.role == "admin" ||
            response.user.role == "superadmin"
        ) "admin" else "customer"

        val normalizedUser = AuthUser(
            id = response.user.effectiveId,
            email = response.user.email.orEmpty(),
            name = response.user.name,
            photoUrl = response.user.effectivePhotoUrl,
            role = role,
            provider = response.user.provider.ifBlank { "password" }
        )

        val plainEntries: List<Pair<Preferences.Key<String>, String>> = listOf(
            Pair(KEY_TOKEN, response.token),
            Pair(KEY_USER_ID, normalizedUser.id),
            Pair(KEY_EMAIL, normalizedUser.email.orEmpty()),
            Pair(KEY_NAME, normalizedUser.name),
            Pair(KEY_PHOTO, normalizedUser.photoUrl),
            Pair(KEY_ROLE, normalizedUser.role),
            Pair(KEY_PROVIDER, normalizedUser.provider)
        )

        val snapshot = EstadoSesionMovil(
            token = response.token.takeIf { it.isNotBlank() },
            userId = normalizedUser.id.takeIf { it.isNotBlank() },
            user = if (response.token.startsWith("local_session_")) null else normalizedUser
        )

        writeMutex.withLock {
            suspend fun escribir() {
                val encryptedEntries = plainEntries.map { (key, plain) ->
                    Triple(key, cifrador.cifrar(plain), plain)
                }
                dataStore.edit { prefs ->
                    prefs.clear()
                    encryptedEntries.forEach { (key, encrypted, plain) ->
                        prefs[key] = encrypted
                        if (!cifrador.isRobolectric()) {
                            decryptedFieldCache[encrypted] = plain
                        }
                    }
                }
                if (cacheKey != null) {
                    sessionCache[cacheKey] = snapshot
                }
            }

            try {
                escribir()
            } catch (firstError: Exception) {
                recuperarAlmacenInvalidoAsync(dataStore, cacheKey, firstError)
                try {
                    escribir()
                } catch (recoveryError: Exception) {
                    MonitorErrores.registrarError(
                        "MobileSession",
                        "No se pudo guardar la sesión cifrada en DataStore con Android Keystore.",
                        recoveryError
                    )
                    throw IllegalStateException("No se pudo guardar el almacenamiento seguro de sesión.", recoveryError)
                }
            }
        }
    }

    suspend fun clearAsync(context: Context) {
        clearFrom(
            dataStore = store(context),
            cacheKey = cacheKey(context)
        )
    }

    internal suspend fun clearFrom(
        dataStore: DataStore<Preferences>,
        cacheKey: String? = null
    ) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            decryptedFieldCache.clear()
            if (cacheKey != null) {
                sessionCache[cacheKey] = EstadoSesionMovil()
            }
            dataStore.edit { it.clear() }
        }
    }

    fun cachedSession(context: Context): EstadoSesionMovil? =
        sessionCache[cacheKey(context)]

    fun save(context: Context, response: AuthResponse) {
        runBlocking(Dispatchers.IO) {
            saveAsync(context, response)
        }
    }

    fun token(context: Context): String? =
        obtenerSesionSincrona(context).token

    fun userId(context: Context): String? =
        obtenerSesionSincrona(context).userId

    fun user(context: Context): AuthUser? =
        obtenerSesionSincrona(context).user

    fun clear(context: Context) {
        val key = cacheKey(context)
        sessionCache[key] = EstadoSesionMovil()
        runBlocking(Dispatchers.IO) {
            clearAsync(context)
        }
    }

    private fun obtenerSesionSincrona(context: Context): EstadoSesionMovil {
        val key = cacheKey(context)
        sessionCache[key]?.let { return it }
        return runBlocking(Dispatchers.IO) {
            readSessionAsync(context)
        }
    }

    private fun decodificarEstadoSesion(
        prefs: Preferences,
        dataStore: DataStore<Preferences>,
        cacheKey: String?
    ): EstadoSesionMovil {
        val rawToken = prefs[KEY_TOKEN]
        val rawUserId = prefs[KEY_USER_ID]
        if (rawToken == null && rawUserId == null) {
            val empty = EstadoSesionMovil()
            if (cacheKey != null) sessionCache[cacheKey] = empty
            return empty
        }

        return try {
            val token = descifrarCampo(rawToken)?.takeIf { it.isNotBlank() }
            val userId = descifrarCampo(rawUserId)?.takeIf { it.isNotBlank() }
            val user = if (userId != null && token != null && !token.startsWith("local_session_")) {
                AuthUser(
                    id = userId,
                    email = descifrarCampo(prefs[KEY_EMAIL]).orEmpty(),
                    name = descifrarCampo(prefs[KEY_NAME]).orEmpty(),
                    photoUrl = descifrarCampo(prefs[KEY_PHOTO]).orEmpty(),
                    role = descifrarCampo(prefs[KEY_ROLE]).orEmpty().ifBlank { "customer" },
                    provider = descifrarCampo(prefs[KEY_PROVIDER]).orEmpty().ifBlank { "password" }
                )
            } else {
                null
            }
            EstadoSesionMovil(
                token = token,
                userId = userId,
                user = user
            ).also { snapshot ->
                if (cacheKey != null) sessionCache[cacheKey] = snapshot
            }
        } catch (error: Exception) {
            programarRecuperacionAlmacenInvalido(dataStore, cacheKey, error)
            EstadoSesionMovil()
        }
    }

    private fun descifrarCampo(raw: String?): String? {
        if (raw == null) return null
        if (cifrador.isRobolectric()) return raw
        decryptedFieldCache[raw]?.let { return it }
        val decoded = cifrador.descifrar(raw)
            ?: throw IllegalStateException("Valor de sesión cifrado con formato inválido.")
        decryptedFieldCache[raw] = decoded
        return decoded
    }

    private fun programarRecuperacionAlmacenInvalido(
        dataStore: DataStore<Preferences>,
        cacheKey: String?,
        error: Exception?
    ) {
        decryptedFieldCache.clear()
        if (cacheKey != null) {
            sessionCache[cacheKey] = EstadoSesionMovil()
        }
        recoveryScope.launch {
            writeMutex.withLock {
                recuperarAlmacenInvalidoAsync(dataStore, cacheKey, error)
            }
        }
    }

    private suspend fun recuperarAlmacenInvalidoAsync(
        dataStore: DataStore<Preferences>,
        cacheKey: String?,
        error: Exception?
    ) {
        decryptedFieldCache.clear()
        if (cacheKey != null) {
            sessionCache[cacheKey] = EstadoSesionMovil()
        }
        if (error != null) {
            MonitorErrores.registrarAdvertencia(
                "MobileSession",
                "Almacenamiento cifrado de sesión inválido o heredado; se limpiará para recrearlo.",
                error
            )
        }
        runCatching { dataStore.edit { it.clear() } }
        cifrador.eliminarClaves()
    }
}
