package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.remote.MobileSession
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
                Log.e("EleganViewModel", "Error al sincronizar el perfil del usuario: ${e.message}", e)
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
                Log.e("EleganViewModel", "Error al sincronizar mensajes: ${e.message}")
            }
        }
    }

    fun sendSupportMessage(text: String, mediaUri: String, mediaType: String) {
        if (text.isBlank() && mediaUri.isBlank()) return
        scope.launch {
            try {
                var resolvedMediaUrl = mediaUri
                val user = MobileSession.user(appContext)
                if (
                    mediaUri.isNotBlank() &&
                    !mediaUri.startsWith("http://", ignoreCase = true) &&
                    !mediaUri.startsWith("https://", ignoreCase = true) &&
                    user?.canUploadMedia == true
                ) {
                    try {
                        resolvedMediaUrl = repository.uploadMedia(Uri.parse(mediaUri))
                    } catch (uploadError: Exception) {
                        Log.w("EleganViewModel", "No se pudo subir multimedia vía /api/movil/media.", uploadError)
                    }
                }
                repository.sendSupportMessage(text, resolvedMediaUrl, mediaType, scope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al enviar mensaje: ${e.message}")
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
                Log.e("EleganViewModel", "Error al guardar perfil en el backend.", e)
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
                Log.e("EleganViewModel", "Error al eliminar la cuenta.", e)
                onFailure(e.localizedMessage ?: "No se pudo eliminar la cuenta. Inténtalo de nuevo.")
            }
        }
    }}
