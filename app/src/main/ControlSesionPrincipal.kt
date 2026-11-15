package com.example

import android.app.Activity
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
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.signOutUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class Screen {
    INICIO,
    CATEGORIA,
    CATALOGO,
    PRODUCTO,
    CARRITO,
    VERIFICASION,
    GRACIA,
    PERFIL,
    CHATS
}

@Composable
fun MainApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf<AuthUser?>(null) }
    var isSessionReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val loadedUser = withContext(Dispatchers.IO) {
            MobileSession.user(context)
        }
        currentUser = loadedUser
        isSessionReady = true
        attemptAutoSignIn(
            context = context,
            onAuthSuccess = { currentUser = MobileSession.user(context) },
            onUnauthenticated = { currentUser = null },
            scope = coroutineScope
        )
    }

    val activity = context as? Activity
    LaunchedEffect(currentUser) {
        activity?.window?.let { window ->
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    when {
        !isSessionReady -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            )
        }
        currentUser == null -> {
            AuthScreen(
                onAuthSuccess = { currentUser = MobileSession.user(context) }
            )
        }
        else -> {
            AppContent(
                onSignOut = {
                    signOutUser(
                        context = context,
                        onSignOutComplete = { currentUser = null },
                        scope = coroutineScope
                    )
                },
                onAccountDeleted = { currentUser = null }
            )
        }
    }
}
