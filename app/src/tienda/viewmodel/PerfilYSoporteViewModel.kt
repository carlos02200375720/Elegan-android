package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.observability.MonitorErrores
import com.example.data.remote.AuthUser
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel dedicado a los flujos de Perfil, Sesión/Cuenta y Chat de Soporte Concierge.
 * Completamente libre de dependencias de `Context` o `AndroidViewModel`.
 */
class PerfilYSoporteViewModel(
    private val repository: EleganRepository,
    autoLoad: Boolean = true
) : ViewModel() {

    private val _sessionUser = MutableStateFlow<AuthUser?>(repository.currentSessionUser())
    val sessionUser: StateFlow<AuthUser?> = _sessionUser.asStateFlow()

    val messages: StateFlow<List<SupportMessage>> = repository.observeMessages()
        .catch { emit(emptyList()) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { emit(null) }
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000L), initialValue = null)

    init {
        if (autoLoad) {
            refreshUserProfile()
            refreshMessages()
        }
    }

    fun refreshSessionUserSnapshot() {
        _sessionUser.value = repository.currentSessionUser()
    }

    fun clearSessionSnapshot() {
        _sessionUser.value = null
    }

    fun refreshUserProfile() {
        refreshSessionUserSnapshot()
        viewModelScope.launch {
            try {
                repository.refreshUserProfile()
                refreshSessionUserSnapshot()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError(
                    "EleganPerfil",
                    "Error al sincronizar el perfil del usuario: ${e.message}",
                    e
                )
            }
        }
    }

    fun refreshMessages() {
        viewModelScope.launch {
            try {
                repository.refreshMessages()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError(
                    "EleganSoporte",
                    "Error al sincronizar mensajes: ${e.message}",
                    e
                )
            }
        }
    }

    fun sendSupportMessage(text: String, mediaUri: String = "", mediaType: String = "") {
        if (text.isBlank() && mediaUri.isBlank()) return
        viewModelScope.launch {
            try {
                var resolvedMediaUrl = mediaUri.trim()
                if (resolvedMediaUrl.startsWith("http://", ignoreCase = true)) {
                    MonitorErrores.registrarError(
                        "EleganSoporte",
                        "Se bloqueó un adjunto de soporte con URL en texto plano (HTTP)."
                    )
                    return@launch
                }
                val isRemoteHttpsUrl = resolvedMediaUrl.startsWith("https://", ignoreCase = true)
                if (resolvedMediaUrl.isNotBlank() && !isRemoteHttpsUrl) {
                    try {
                        resolvedMediaUrl = repository.uploadSupportAttachment(Uri.parse(resolvedMediaUrl))
                    } catch (e: CancellationException) {
                        throw e
                    } catch (uploadError: Exception) {
                        MonitorErrores.registrarError(
                            "EleganSoporte",
                            "No se pudo subir el adjunto del chat de soporte. El mensaje no será enviado.",
                            uploadError
                        )
                        return@launch
                    }
                }
                if (resolvedMediaUrl.isNotBlank() &&
                    !resolvedMediaUrl.startsWith("https://", ignoreCase = true)
                ) {
                    MonitorErrores.registrarError(
                        "EleganSoporte",
                        "Se bloqueó el envío de un adjunto de soporte sin URL HTTPS válida."
                    )
                    return@launch
                }
                repository.sendSupportMessage(text, resolvedMediaUrl, mediaType, viewModelScope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganSoporte", "Error al enviar mensaje: ${e.message}", e)
            }
        }
    }

    fun saveProfile(
        name: String,
        email: String,
        phone: String,
        address: String,
        city: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.saveUserProfile(name, email, phone, address, city)
                refreshSessionUserSnapshot()
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganPerfil", "Error al guardar perfil en el backend.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun clearUserSessionData(
        onCleared: () -> Unit = {},
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.clearUserSessionData()
                clearSessionSnapshot()
                onCleared()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (error: Exception) {
                MonitorErrores.registrarError(
                    "EleganPerfil",
                    "No se pudieron limpiar los datos de la sesión.",
                    error
                )
                onComplete()
            }
        }
    }

    fun deleteAccount(
        onCleared: () -> Unit = {},
        onComplete: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.deleteUserAccountData()
                clearSessionSnapshot()
                onCleared()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganPerfil", "Error al eliminar la cuenta.", e)
                onFailure(e.localizedMessage ?: "No se pudo eliminar la cuenta. Inténtalo de nuevo.")
            }
        }
    }
}
