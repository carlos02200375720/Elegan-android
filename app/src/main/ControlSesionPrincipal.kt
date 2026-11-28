package com.example

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.example.data.local.AlmacenBloqueoTransferencia
import com.example.data.observability.MonitorErrores
import com.example.data.remote.AuthUser
import com.example.data.remote.ClienteHttpElegan
import com.example.data.remote.MobileSession
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasPendingTransferLock by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var isSessionReady by remember { mutableStateOf(false) }
    var isGuest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val (cachedUser, pendingLock, userHasTransferLock) = withContext(Dispatchers.IO) {
            val user = MobileSession.user(context)
            val anyLock = AlmacenBloqueoTransferencia.tieneBloqueoActivo(context)
            val userLock = if (user != null) {
                AlmacenBloqueoTransferencia.cargar(context, user.id) != null
            } else {
                false
            }
            Triple(user, anyLock, userLock)
        }
        hasPendingTransferLock = pendingLock
        if (cachedUser == null) {
            isSessionReady = true
        } else {
            if (userHasTransferLock) {
                currentUser = cachedUser
                isSessionReady = true
            }
            attemptAutoSignIn(
                context = context,
                onAuthSuccess = {
                    currentUser = MobileSession.user(context) ?: cachedUser
                    isSessionReady = true
                },
                onUnauthenticated = {
                    currentUser = null
                    isSessionReady = true
                },
                scope = coroutineScope
            )
        }
    }

    LaunchedEffect(Unit) {
        ClienteHttpElegan.eventosSesionExpirada.collect {
            if (currentUser != null) {
                currentUser = null
                isGuest = false
            }
        }
    }

    val activity = context as? Activity
    LaunchedEffect(currentUser) {
        MonitorErrores.actualizarUsuario(currentUser?.id, currentUser?.role)
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (!controller.isAppearanceLightStatusBars) {
                controller.isAppearanceLightStatusBars = true
            }
            if (!controller.isAppearanceLightNavigationBars) {
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    when {
        !isSessionReady -> {
            if (hasPendingTransferLock) {
                BackHandler(enabled = true) { }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        currentUser == null && !isGuest -> {
            AuthScreen(
                onAuthSuccess = {
                    isGuest = false
                    currentUser = MobileSession.user(context)
                },
                onGuestContinue = { isGuest = true }
            )
        }
        else -> {
            AppContent(
                isAuthenticated = currentUser != null,
                onAuthenticated = {
                    isGuest = false
                    currentUser = MobileSession.user(context)
                },
                onSignOut = {
                    if (isGuest) {
                        isGuest = false
                    } else {
                        signOutUser(
                            context = context,
                            onSignOutComplete = { currentUser = null },
                            scope = coroutineScope
                        )
                    }
                },
                onAccountDeleted = {
                    AlmacenBloqueoTransferencia.limpiar(context)
                    currentUser = null
                    isGuest = false
                }
            )
        }
    }
}
