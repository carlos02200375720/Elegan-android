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
import com.example.data.local.AlmacenPreferenciasUsuario
import com.example.data.observability.MonitorErrores
import com.example.data.remote.AuthUser
import com.example.data.remote.ClienteHttpElegan
import com.example.data.remote.MobileSession
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import kotlinx.coroutines.launch

@Composable
fun MainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var hasPendingTransferLock by remember { mutableStateOf(false) }
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var isSessionReady by remember { mutableStateOf(false) }
    var isGuest by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cachedUser = MobileSession.userAsync(context)
        val pendingLock = AlmacenBloqueoTransferencia.tieneBloqueoActivoAsync(context)
        val userHasTransferLock = if (cachedUser != null) {
            AlmacenBloqueoTransferencia.cargarAsync(context, cachedUser.id) != null
        } else {
            false
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
                    coroutineScope.launch {
                        currentUser = MobileSession.userAsync(context) ?: cachedUser
                        isSessionReady = true
                    }
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
                AlmacenPreferenciasUsuario.guardarModoInvitado(context, false)
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
                    coroutineScope.launch {
                        AlmacenPreferenciasUsuario.guardarModoInvitado(context, false)
                        currentUser = MobileSession.userAsync(context)
                    }
                },
                onGuestContinue = {
                    isGuest = true
                    coroutineScope.launch {
                        AlmacenPreferenciasUsuario.guardarModoInvitado(context, true)
                    }
                }
            )
        }
        else -> {
            AppContent(
                isAuthenticated = currentUser != null,
                onAuthenticated = {
                    isGuest = false
                    coroutineScope.launch {
                        AlmacenPreferenciasUsuario.guardarModoInvitado(context, false)
                        currentUser = MobileSession.userAsync(context)
                    }
                },
                onSignOut = {
                    if (isGuest) {
                        isGuest = false
                        coroutineScope.launch {
                            AlmacenPreferenciasUsuario.guardarModoInvitado(context, false)
                        }
                    } else {
                        signOutUser(
                            context = context,
                            onSignOutComplete = { currentUser = null },
                            scope = coroutineScope
                        )
                    }
                },
                onAccountDeleted = {
                    currentUser = null
                    isGuest = false
                    coroutineScope.launch {
                        AlmacenBloqueoTransferencia.limpiarAsync(context)
                        AlmacenPreferenciasUsuario.limpiarPreferencias(context, conservarVisitorId = false)
                    }
                }
            )
        }
    }
}
