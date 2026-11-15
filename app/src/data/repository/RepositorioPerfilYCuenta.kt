package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilUsuarioRespuesta
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class RepositorioPerfilYCuenta(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito,
    private val guardadosYLikesRepo: RepositorioGuardadosYLikes,
    private val pedidosRepo: RepositorioPedidos,
    private val mensajesRepo: RepositorioMensajesSoporte
) {
    private val profilePrefs by lazy {
        appContext.getSharedPreferences("elegan_profile_cache", Context.MODE_PRIVATE)
    }
    private val profileLoadMutex = Mutex()

    @Volatile
    private var loadedProfileKey: String? = null
    private val localProfileFlow = MutableStateFlow<UserProfile?>(null)

    private fun cacheKey(userKey: String = userCacheKey(appContext)): String = "user_profile_$userKey"

    private fun mapRemoteProfile(remote: PerfilUsuarioRespuesta): UserProfile = UserProfile(
        id = remote.userId,
        userId = remote.userId,
        displayName = remote.displayName.ifBlank { remote.name },
        email = remote.email,
        photoUrl = remote.photoUrl,
        phone = remote.phone,
        address = remote.address,
        city = remote.city,
        updatedAt = remote.updatedAt
    )

    private suspend fun ensureProfileLoaded() {
        val currentKey = userCacheKey(appContext)
        if (loadedProfileKey == currentKey) return
        profileLoadMutex.withLock {
            if (loadedProfileKey == currentKey) return
            val cachedUser = MobileSession.user(appContext)
            val saved = withContext(Dispatchers.IO) { loadPersistedProfile(currentKey) }
            val resolved = try {
                val token = MobileSession.token(appContext)
                    ?: throw IllegalStateException("No hay sesión autenticada.")
                mapRemoteProfile(
                    ApiAutenticacionMovil.service.profile("Bearer $token").item
                )
            } catch (error: Exception) {
                Log.w("EleganRepository", "No se pudo actualizar el perfil desde el backend; se usará la caché existente.", error)
                when {
                    saved != null -> saved.copy(
                        userId = saved.userId.ifBlank { cachedUser?.effectiveId.orEmpty() },
                        displayName = saved.displayName.ifBlank { cachedUser?.name.orEmpty() },
                        email = saved.email.ifBlank { cachedUser?.email.orEmpty() },
                        photoUrl = saved.photoUrl.ifBlank { cachedUser?.effectivePhotoUrl.orEmpty() }
                    )
                    cachedUser != null -> UserProfile(
                        id = cachedUser.effectiveId,
                        userId = cachedUser.effectiveId,
                        displayName = cachedUser.name,
                        email = cachedUser.email.orEmpty(),
                        photoUrl = cachedUser.effectivePhotoUrl
                    )
                    else -> null
                }
            }
            localProfileFlow.value = resolved
            if (resolved != null) savePersistedProfile(resolved, currentKey)
            loadedProfileKey = currentKey
        }
    }

    private fun loadPersistedProfile(userKey: String): UserProfile? {
        val raw = profilePrefs.getString(cacheKey(userKey), null) ?: return null
        return try {
            val obj = JSONObject(raw)
            UserProfile(
                id = obj.optString("id", ""),
                userId = obj.optString("userId", ""),
                displayName = obj.optString("displayName", ""),
                email = obj.optString("email", ""),
                photoUrl = obj.optString("photoUrl", ""),
                phone = obj.optString("phone", ""),
                address = obj.optString("address", ""),
                city = obj.optString("city", ""),
                updatedAt = obj.optString("updatedAt", "").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.w("EleganRepository", "No se pudo leer el perfil local: ${e.message}")
            null
        }
    }

    private suspend fun savePersistedProfile(profile: UserProfile, userKey: String = userCacheKey(appContext)) {
        withContext(Dispatchers.IO) {
            try {
                val obj = JSONObject().apply {
                    put("id", profile.id)
                    put("userId", profile.userId)
                    put("displayName", profile.displayName)
                    put("email", profile.email)
                    put("photoUrl", profile.photoUrl)
                    put("phone", profile.phone)
                    put("address", profile.address)
                    put("city", profile.city)
                    put("updatedAt", profile.updatedAt.orEmpty())
                }
                profilePrefs.edit().putString(cacheKey(userKey), obj.toString()).commit()
            } catch (e: Exception) {
                Log.w("EleganRepository", "No se pudo guardar el perfil local: ${e.message}")
            }
        }
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        ensureProfileLoaded()
        localProfileFlow.collect { emit(it) }
    }.flowOn(Dispatchers.IO)

    suspend fun saveUserProfile(
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String
    ) {
        val token = MobileSession.token(appContext)
            ?: throw IllegalStateException("Debes iniciar sesión para guardar tu perfil.")
        val response = ApiAutenticacionMovil.service.updateProfile(
            authorization = "Bearer $token",
            request = PerfilActualizarRequest(
                name = name.trim(),
                email = email.trim(),
                phone = phone.trim(),
                address = address.trim(),
                city = city.trim()
            )
        )
        if (!response.success) {
            throw IllegalStateException("El servidor no confirmó la actualización del perfil.")
        }

        val profile = mapRemoteProfile(response.item)
        localProfileFlow.value = profile
        savePersistedProfile(profile)
        try {
            MobileSession.validateSession(appContext)
        } catch (error: Exception) {
            Log.w("EleganRepository", "El perfil se guardó, pero no se pudo refrescar la sesión local.", error)
        }
    }

    suspend fun deleteUserAccountData() {
        withContext(Dispatchers.IO) {
            val userKey = userCacheKey(appContext)
            pedidosRepo.ensureLoaded()
            mensajesRepo.ensureLoaded()

            val mediaPaths = (pedidosRepo.paymentProofUrls() + mensajesRepo.mediaUrls())
                .filter { it.startsWith("file://") }
            val localMediaRoot = File(appContext.filesDir, "elegan_local_media")
            mediaPaths.forEach { rawUri ->
                try {
                    val filePath = Uri.parse(rawUri).path
                    if (!filePath.isNullOrBlank()) {
                        val file = File(filePath)
                        if (file.exists() && file.absolutePath.startsWith(localMediaRoot.absolutePath)) {
                            file.delete()
                        }
                    }
                } catch (_: Exception) {}
            }

            carritoRepo.clearUserData(userKey)
            guardadosYLikesRepo.clearUserData(userKey)
            pedidosRepo.clearUserData(userKey)
            mensajesRepo.clearUserData(userKey)

            profilePrefs.edit().remove(cacheKey(userKey)).commit()
            localProfileFlow.value = null
            loadedProfileKey = null

            MobileSession.deleteAccount(appContext)
        }
    }
}
