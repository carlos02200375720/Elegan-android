package com.example.ui.viewmodel

import android.util.Log
import com.example.data.model.SupportMessage
import com.example.data.model.UserProfile
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class ControladorPerfilYSoporte(
    private val repository: EleganRepository,
    private val scope: CoroutineScope
) {
    val messages: StateFlow<List<SupportMessage>> = repository.observeMessages()
        .catch { emit(emptyList()) }
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile()
        .catch { emit(null) }
        .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(5000L), initialValue = null)

    fun sendSupportMessage(text: String, mediaUri: String, mediaType: String) {
        if (text.isBlank() && mediaUri.isBlank()) return
        scope.launch {
            try {
                repository.sendSupportMessage(text, mediaUri, mediaType, scope)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al enviar mensaje: ${e.message}")
            }
        }
    }

    fun saveProfile(name: String, email: String, phone: String, address: String, city: String) {
        scope.launch {
            try {
                repository.saveUserProfile(name, email, phone, address, city)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al guardar perfil: ${e.message}")
            }
        }
    }

    fun deleteAccount(
        onCleared: () -> Unit,
        onComplete: () -> Unit
    ) {
        scope.launch {
            try {
                repository.deleteUserAccountData()
                onCleared()
                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EleganViewModel", "Error al eliminar la cuenta: ${e.message}")
            }
        }
    }
}
