package com.example.data.repository

import android.content.Context
import com.example.data.error.ErrorApi
import com.example.data.error.ResultWrapper
import com.example.data.error.safeApiCall
import com.example.data.model.UserProfile
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import com.example.data.remote.PerfilActualizarRequest
import com.example.data.remote.PerfilUsuarioRespuesta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val profileApiErrorFlow = MutableStateFlow<ErrorApi?>(null)

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

    fun observeProfileApiError(): Flow<ErrorApi?> = profileApiErrorFlow.asStateFlow()

    private suspend fun ensureProfileLoaded() = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)
        if (token.isNullOrBlank()) {
            profileFlow.value = null
            profileApiErrorFlow.value = null
            loadedToken = null
            return@withContext
        }
        if (loadedToken == token && profileFlow.value != null) return@withContext
        profileLoadMutex.withLock {
            if (loadedToken == token && profileFlow.value != null) return@withLock
            val cachedUser = MobileSession.user(appContext)
            val result = safeApiCall(
                tag = "EleganRepository",
                operation = "ensureProfileLoaded"
            ) {
                mapRemoteProfile(
                    ApiAutenticacionMovil.service.profile("Bearer $token").item
                )
            }
            val resolved = when (result) {
                is ResultWrapper.Success -> {
                    profileApiErrorFlow.value = null
                    result.data
                }
                is ResultWrapper.Error -> {
                    profileApiErrorFlow.value = result.error
                    cachedUser?.let {
                        UserProfile(
                            id = it.effectiveId,
                            userId = it.effectiveId,
                            displayName = it.name,
                            email = it.email.orEmpty()
                        )
                    }
                }
                ResultWrapper.Loading -> null
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
    ) = withContext(Dispatchers.IO) {
        val token = MobileSession.token(appContext)
            ?: throw ErrorApi.UnauthorizedError(userMessage = "Debes iniciar sesión para guardar tu perfil.").toException()

        val updateResult = safeApiCall(
            tag = "EleganRepository",
            operation = "saveUserProfile"
        ) {
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
            mapRemoteProfile(response.item)
        }

        val profile = when (updateResult) {
            is ResultWrapper.Success -> {
                profileApiErrorFlow.value = null
                updateResult.data
            }
            is ResultWrapper.Error -> {
                profileApiErrorFlow.value = updateResult.error
                throw updateResult.error.toException()
            }
            ResultWrapper.Loading -> return@withContext
        }

        profileFlow.value = profile
        loadedToken = token
        safeApiCall(
            tag = "EleganRepository",
            operation = "validateSessionAfterProfileSave"
        ) {
            MobileSession.validateSession(appContext)
        }
    }

    suspend fun clearUserSessionData() {
        withContext(Dispatchers.IO) {
            carritoRepo.clearCart(syncRemote = false)
            guardadosYLikesRepo.clearPrivateUserData()
            pedidosRepo.clearUserData()
            mensajesRepo.clearUserData()
            profileFlow.value = null
            profileApiErrorFlow.value = null
            loadedToken = null
        }
    }

    suspend fun deleteUserAccountData() {
        withContext(Dispatchers.IO) {
            val token = MobileSession.token(appContext)
                ?: throw ErrorApi.UnauthorizedError(userMessage = "Debes iniciar sesión para eliminar tu cuenta.").toException()

            // El servidor confirma la eliminación antes de borrar la sesión local.
            safeApiCall(
                tag = "EleganRepository",
                operation = "deleteAccount"
            ) {
                ApiAutenticacionMovil.service.deleteAccount("Bearer $token")
            }.getOrThrow()

            carritoRepo.clearCart(syncRemote = false)
            guardadosYLikesRepo.clearUserData()
            pedidosRepo.clearUserData()
            mensajesRepo.clearUserData()

            profileFlow.value = null
            profileApiErrorFlow.value = null
            loadedToken = null
            MobileSession.deleteAccount(appContext)
        }
    }
}
