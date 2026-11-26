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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.observability.MonitorErrores
import com.example.data.remote.MobileSession
import com.example.data.remote.RealtimeEvents
import com.example.ui.AlmacenBloqueoTransferencia
import com.example.ui.auth.VentanaEmergenteRegistro
import com.example.ui.components.EleganNotificationHost
import com.example.ui.components.EleganNotificationState
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.viewmodel.EleganViewModel

@Composable
fun AppContent(
    viewModel: EleganViewModel = viewModel(),
    isAuthenticated: Boolean = true,
    onAuthenticated: () -> Unit = {},
    onSignOut: () -> Unit,
    onAccountDeleted: () -> Unit = onSignOut
) {
    val context = LocalContext.current
    val currentUserId = remember(isAuthenticated) { MobileSession.userId(context) }
    val initialTransferLock = remember(currentUserId) {
        AlmacenBloqueoTransferencia.cargar(context, currentUserId)
    }
    var activeTransferLock by remember(currentUserId) { mutableStateOf(initialTransferLock) }

    LaunchedEffect(isAuthenticated) {
        viewModel.syncAccountData()
    }

    LaunchedEffect(currentUserId, isAuthenticated) {
        RealtimeEvents.start(MobileSession.token(context))
        try {
            RealtimeEvents.events.collect { event ->
                when (event.collection) {
                    "products" -> viewModel.refreshProducts()
                    "store_categories" -> viewModel.refreshCategories()
                    "announcements" -> viewModel.refreshAnnouncements()
                    "cart" -> viewModel.refreshCart()
                    "orders" -> viewModel.refreshOrders()
                    "supportMessages" -> viewModel.refreshMessages()
                    "cash_on_delivery", "cashOnDelivery" -> viewModel.loadCashOnDeliveryConfig()
                }
            }
        } finally {
            RealtimeEvents.stop()
        }
    }

    var currentScreen by remember {
        mutableStateOf(if (initialTransferLock != null) Screen.VERIFICASION else Screen.INICIO)
    }
    var activeTabScreen by remember { mutableStateOf(Screen.INICIO) }
    var previousScreen by remember {
        mutableStateOf(if (initialTransferLock != null) Screen.CARRITO else Screen.INICIO)
    }
    var chatsReturnScreen by remember { mutableStateOf(Screen.INICIO) }
    var requestedProfileSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentScreen) {
        if (esPantallaConBarraInferior(currentScreen)) {
            activeTabScreen = currentScreen
        }
        MonitorErrores.registrarNavegacion(currentScreen.name)
    }

    var checkoutSubtotal by remember { mutableDoubleStateOf(initialTransferLock?.subtotal ?: 0.0) }
    var checkoutDiscount by remember { mutableDoubleStateOf(initialTransferLock?.discount ?: 0.0) }
    var checkoutShipping by remember { mutableDoubleStateOf(initialTransferLock?.shipping ?: 0.0) }
    var checkoutTotal by remember { mutableDoubleStateOf(initialTransferLock?.total ?: 0.0) }

    LaunchedEffect(initialTransferLock) {
        initialTransferLock?.let { lock ->
            activeTransferLock = lock
            checkoutSubtotal = lock.subtotal
            checkoutDiscount = lock.discount
            checkoutShipping = lock.shipping
            checkoutTotal = lock.total
            if (lock.couponCode.isNotBlank()) {
                viewModel.setCouponCode(lock.couponCode)
            }
            previousScreen = Screen.CARRITO
            currentScreen = Screen.VERIFICASION
        }
    }

    var showRegisterPopup by remember { mutableStateOf(false) }
    var registerPromptReason by remember { mutableStateOf<String?>(null) }
    var pendingAuthenticatedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var hasCompletedInlineAuth by remember(isAuthenticated) { mutableStateOf(false) }

    fun requireRegisteredUser(
        reason: String? = null,
        onAllowed: () -> Unit
    ) {
        if (isAuthenticated || hasCompletedInlineAuth) {
            onAllowed()
        } else {
            registerPromptReason = reason
            pendingAuthenticatedAction = onAllowed
            showRegisterPopup = true
        }
    }

    fun navigateTo(screen: Screen) {
        // Mientras el bloqueo de transferencia esté activo, no se permite navegar fuera de la pantalla de transferencia.
        if (activeTransferLock != null && screen != Screen.VERIFICASION) {
            return
        }
        if (screen == Screen.CHATS && currentScreen != Screen.CARRITO && currentScreen != Screen.CHATS) {
            chatsReturnScreen = currentScreen
        }
        if (esPantallaConBarraInferior(screen)) {
            activeTabScreen = screen
        }
        previousScreen = currentScreen
        currentScreen = screen
    }

    val eleganNotifier = remember { EleganNotificationState() }
    val contenedorDestino = if (esPantallaConBarraInferior(currentScreen)) {
        DestinoContenedorApp.PestanasPrincipales
    } else {
        DestinoContenedorApp.PantallaSecundaria(currentScreen)
    }

    CompositionLocalProvider(LocalEleganNotifier provides eleganNotifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            AnimatedContent(
                targetState = contenedorDestino,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    val origen = when (val estado = initialState) {
                        DestinoContenedorApp.PestanasPrincipales -> activeTabScreen
                        is DestinoContenedorApp.PantallaSecundaria -> estado.screen
                    }
                    val destino = when (val estado = targetState) {
                        DestinoContenedorApp.PestanasPrincipales -> activeTabScreen
                        is DestinoContenedorApp.PantallaSecundaria -> estado.screen
                    }
                    obtenerTransicionPantalla(origen, destino)
                },
                label = "screen_transition"
            ) { destino ->
                when (destino) {
                    DestinoContenedorApp.PestanasPrincipales -> {
                        PantallasConBarraInferior(
                            screen = activeTabScreen,
                            viewModel = viewModel,
                            isAuthenticated = isAuthenticated,
                            onRequireAuth = ::requireRegisteredUser,
                            onNavigateTo = ::navigateTo,
                            requestedProfileSection = requestedProfileSection,
                            onProfileSectionConsumed = { requestedProfileSection = null },
                            onSignOut = {
                                if (isAuthenticated) {
                                    viewModel.clearUserSessionData(onComplete = onSignOut)
                                } else {
                                    onSignOut()
                                }
                            }
                        )
                    }

                    is DestinoContenedorApp.PantallaSecundaria -> {
                        PantallasSecundariasNavegacion(
                            screen = destino.screen,
                            previousScreen = previousScreen,
                            chatsReturnScreen = chatsReturnScreen,
                            checkoutSubtotal = checkoutSubtotal,
                            checkoutDiscount = checkoutDiscount,
                            checkoutShipping = checkoutShipping,
                            checkoutTotal = checkoutTotal,
                            activeTransferLock = activeTransferLock,
                            viewModel = viewModel,
                            isAuthenticated = isAuthenticated,
                            onRequireAuth = ::requireRegisteredUser,
                            onNavigateTo = ::navigateTo,
                            onNavigateToPerfilOrders = {
                                requestedProfileSection = "orders"
                                navigateTo(Screen.PERFIL)
                            },
                            onTransferLockChanged = { activeTransferLock = it },
                            onTransferLockCleared = {
                                AlmacenBloqueoTransferencia.limpiar(context)
                                activeTransferLock = null
                            },
                            onTransferPaymentConfirmed = {
                                AlmacenBloqueoTransferencia.limpiar(context)
                                activeTransferLock = null
                                navigateTo(Screen.GRACIA)
                            },
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

    if (showRegisterPopup) {
        VentanaEmergenteRegistro(
            promptMessage = registerPromptReason,
            onDismiss = {
                showRegisterPopup = false
                registerPromptReason = null
                pendingAuthenticatedAction = null
            },
            onAuthSuccess = {
                hasCompletedInlineAuth = true
                showRegisterPopup = false
                registerPromptReason = null
                onAuthenticated()
                viewModel.syncAccountData()
                val action = pendingAuthenticatedAction
                pendingAuthenticatedAction = null
                action?.invoke()
            }
        )
    }
}

private sealed interface DestinoContenedorApp {
    data object PestanasPrincipales : DestinoContenedorApp
    data class PantallaSecundaria(val screen: Screen) : DestinoContenedorApp
}

private fun esPantallaConBarraInferior(screen: Screen): Boolean =
    screen == Screen.INICIO || screen == Screen.CATALOGO || screen == Screen.PERFIL
