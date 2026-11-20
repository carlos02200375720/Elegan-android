package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.observability.MonitorErrores
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorPerfilYSoporte(
    private val appContext: Context,
    private val repository: EleganRepository,
    private val scope: CoroutineScope
) {
    val messages: StateFlow<List<SupportMessage>> = repository.observeMessages()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { emit(null) }
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = null)

    fun refreshUserProfile() {
        scope.launch {
            try {
                repository.refreshUserProfile()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganViewModel", "Error al sincronizar el perfil del usuario: ${e.message}", e)
            }
        }
    }

    fun refreshMessages() {
        scope.launch {
            try {
                repository.refreshMessages()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganViewModel", "Error al sincronizar mensajes: ${e.message}", e)
            }
        }
    }

    fun sendSupportMessage(text: String, mediaUri: String, mediaType: String) {
        if (text.isBlank() && mediaUri.isBlank()) return
        scope.launch {
            try {
                var resolvedMediaUrl = mediaUri.trim()
                val isRemoteUrl = resolvedMediaUrl.startsWith("https://", ignoreCase = true) ||
                    resolvedMediaUrl.startsWith("http://", ignoreCase = true)
                if (resolvedMediaUrl.isNotBlank() && !isRemoteUrl) {
                    try {
                        resolvedMediaUrl = repository.uploadSupportAttachment(Uri.parse(resolvedMediaUrl))
                    } catch (uploadError: Exception) {
                        MonitorErrores.registrarError(
                            "EleganViewModel",
                            "No se pudo subir el adjunto del chat de soporte. El mensaje no será enviado.",
                            uploadError
                        )
                        return@launch
                    }
                }
                if (resolvedMediaUrl.isNotBlank() &&
                    !resolvedMediaUrl.startsWith("https://", ignoreCase = true) &&
                    !resolvedMediaUrl.startsWith("http://", ignoreCase = true)
                ) {
                    MonitorErrores.registrarError("EleganViewModel", "Se bloqueó el envío de una URI local como adjunto de soporte.")
                    return@launch
                }
                repository.sendSupportMessage(text, resolvedMediaUrl, mediaType, scope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganViewModel", "Error al enviar mensaje: ${e.message}", e)
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
        scope.launch {
            try {
                repository.saveUserProfile(name, email, phone, address, city)
                onResult(Result.success(Unit))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganViewModel", "Error al guardar perfil en el backend.", e)
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteAccount(
        onCleared: () -> Unit,
        onComplete: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        scope.launch {
            try {
                repository.deleteUserAccountData()
                onCleared()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                MonitorErrores.registrarError("EleganViewModel", "Error al eliminar la cuenta.", e)
                onFailure(e.localizedMessage ?: "No se pudo eliminar la cuenta. Inténtalo de nuevo.")
            }
        }
    }
}
