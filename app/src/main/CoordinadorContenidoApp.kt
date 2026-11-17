package com.example

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EleganNotificationHost
import com.example.ui.components.EleganNotificationState
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.viewmodel.EleganViewModel

@Composable
fun AppContent(
    viewModel: EleganViewModel = viewModel(),
    onSignOut: () -> Unit,
    onAccountDeleted: () -> Unit = onSignOut
) {
    LaunchedEffect(Unit) {
        viewModel.syncAccountData()
    }

    var currentScreen by remember { mutableStateOf(Screen.INICIO) }
    var previousScreen by remember { mutableStateOf(Screen.INICIO) }
    var chatsReturnScreen by remember { mutableStateOf(Screen.INICIO) }

    var checkoutSubtotal by remember { mutableDoubleStateOf(0.0) }
    var checkoutDiscount by remember { mutableDoubleStateOf(0.0) }
    var checkoutShipping by remember { mutableDoubleStateOf(0.0) }
    var checkoutTotal by remember { mutableDoubleStateOf(0.0) }

    fun navigateTo(screen: Screen) {
        if (screen == Screen.CHATS && currentScreen != Screen.CARRITO && currentScreen != Screen.CHATS) {
            chatsReturnScreen = currentScreen
        }
        previousScreen = currentScreen
        currentScreen = screen
    }

    val eleganNotifier = remember { EleganNotificationState() }

    CompositionLocalProvider(LocalEleganNotifier provides eleganNotifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { obtenerTransicionPantalla(initialState, targetState) },
                label = "screen_transition"
            ) { screen ->
                val isTabScreen = screen == Screen.INICIO ||
                    screen == Screen.CATALOGO ||
                    screen == Screen.PERFIL

                if (isTabScreen) {
                    PantallasConBarraInferior(
                        screen = screen,
                        viewModel = viewModel,
                        onNavigateTo = ::navigateTo,
                        onSignOut = onSignOut
                    )
                } else {
                    PantallasSecundariasNavegacion(
                        screen = screen,
                        previousScreen = previousScreen,
                        chatsReturnScreen = chatsReturnScreen,
                        checkoutSubtotal = checkoutSubtotal,
                        checkoutDiscount = checkoutDiscount,
                        checkoutShipping = checkoutShipping,
                        checkoutTotal = checkoutTotal,
                        viewModel = viewModel,
                        onNavigateTo = ::navigateTo,
                        onUpdateCheckoutTotals = { sub, disc, ship, tot ->
                            checkoutSubtotal = sub
                            checkoutDiscount = disc
                            checkoutShipping = ship
                            checkoutTotal = tot
                        },
                        onAccountDeleted = onAccountDeleted
                    )
                }
            }

            val hasBottomBarOverlay = currentScreen == Screen.INICIO ||
                currentScreen == Screen.CATALOGO ||
                currentScreen == Screen.PERFIL ||
                currentScreen == Screen.PRODUCTO

            EleganNotificationHost(
                state = eleganNotifier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = if (hasBottomBarOverlay) 72.dp else 24.dp)
            )
        }
    }
}
