package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.UserProfile
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilUsuarioRespuesta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class RepositorioPerfilYCuenta(
    private val appContext: Context,
    private val carritoRepo: RepositorioCarrito,
    private val guardadosYLikesRepo: RepositorioGuardadosYLikes,
    private val pedidosRepo: RepositorioPedidos,
    private val mensajesRepo: RepositorioMensajesSoporte
) {
    private val profileLoadMutex = Mutex()

    @Volatile
    private var loadedToken: String? = null
    private val profileFlow = MutableStateFlow<UserProfile?>(null)

    private fun mapRemoteProfile(remote: PerfilUsuarioRespuesta): UserProfile = UserProfile(
        id = remote.userId,
        userId = remote.userId,
        displayName = remote.displayName.ifBlank { remote.name },
        email = remote.email,
        phone = remote.phone,
        address = remote.address,
        city = remote.city,
        updatedAt = remote.updatedAt
    )

    private suspend fun ensureProfileLoaded() {
        val token = MobileSession.token(appContext)
        if (token.isNullOrBlank()) {
            profileFlow.value = null
            loadedToken = null
            return
        }
        if (loadedToken == token && profileFlow.value != null) return
        profileLoadMutex.withLock {
            if (loadedToken == token && profileFlow.value != null) return
            val cachedUser = MobileSession.user(appContext)
            val resolved = try {
                mapRemoteProfile(
                    ApiAutenticacionMovil.service.profile("Bearer $token").item
                )
            } catch (error: Exception) {
                Log.w("EleganRepository", "No se pudo obtener el perfil desde el backend.", error)
                cachedUser?.let {
                    UserProfile(
                        id = it.effectiveId,
                        userId = it.effectiveId,
                        displayName = it.name,
                        email = it.email.orEmpty()
                    )
                }
            }
            profileFlow.value = resolved
            loadedToken = token
        }
    }

    suspend fun refreshUserProfile() {
        loadedToken = null
        ensureProfileLoaded()
    }

    fun observeUserProfile(): Flow<UserProfile?> = flow {
        ensureProfileLoaded()
        profileFlow.collect { emit(it) }
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
        profileFlow.value = profile
        loadedToken = token
        try {
            MobileSession.validateSession(appContext)
        } catch (error: Exception) {
            Log.w("EleganRepository", "El perfil se guardó, pero no se pudo refrescar la sesión.", error)
        }
    }

    suspend fun clearUserSessionData() {
        withContext(Dispatchers.IO) {
            carritoRepo.clearCart(syncRemote = false)
            guardadosYLikesRepo.clearPrivateUserData()
            pedidosRepo.clearUserData()
            mensajesRepo.clearUserData()
            profileFlow.value = null
            loadedToken = null
        }
    }

    suspend fun deleteUserAccountData() {
        withContext(Dispatchers.IO) {
            carritoRepo.clearCart(syncRemote = false)
            guardadosYLikesRepo.clearUserData()
            pedidosRepo.clearUserData()
            mensajesRepo.clearUserData()

            profileFlow.value = null
            loadedToken = null

            MobileSession.deleteAccount(appContext)
        }
    }
}
