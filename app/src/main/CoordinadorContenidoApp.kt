package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.local.AlmacenBloqueoTransferencia
import com.example.data.observability.MonitorErrores
import com.example.data.remote.AccionRefrescoTiempoReal
import com.example.data.remote.CoordinadorEventosTiempoReal
import com.example.data.remote.MobileSession
import com.example.data.remote.RealtimeEvents
import com.example.ui.auth.VentanaEmergenteRegistro
import com.example.ui.components.EleganNotificationHost
import com.example.ui.components.EleganNotificationState
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.viewmodel.CarritoYPedidosViewModel
import com.example.ui.viewmodel.CatalogoViewModel
import com.example.ui.viewmodel.InteraccionesViewModel
import com.example.ui.viewmodel.PerfilYSoporteViewModel
import com.example.ui.viewmodel.rememberEleganViewModelFactory

@Composable
fun AppContent(
    viewModelFactory: ViewModelProvider.Factory = rememberEleganViewModelFactory(),
    catalogoViewModel: CatalogoViewModel = viewModel(factory = viewModelFactory),
    interaccionesViewModel: InteraccionesViewModel = viewModel(factory = viewModelFactory),
    carritoYPedidosViewModel: CarritoYPedidosViewModel = viewModel(factory = viewModelFactory),
    perfilYSoporteViewModel: PerfilYSoporteViewModel = viewModel(factory = viewModelFactory),
    navController: NavHostController = rememberNavController(),
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

    fun syncAccountData() {
        interaccionesViewModel.refreshInteractions()
        carritoYPedidosViewModel.refreshCart()
        carritoYPedidosViewModel.refreshOrders()
        perfilYSoporteViewModel.refreshUserProfile()
        perfilYSoporteViewModel.refreshMessages()
    }

    LaunchedEffect(isAuthenticated) {
        syncAccountData()
    }

    LaunchedEffect(currentUserId, isAuthenticated) {
        val coordinadorTiempoReal = CoordinadorEventosTiempoReal(
            scope = this,
            onEjecutar = { accion ->
                when (accion) {
                    AccionRefrescoTiempoReal.PRODUCTOS -> {
                        catalogoViewModel.refreshProducts()
                        catalogoViewModel.refreshSelectedProduct()
                    }
                    AccionRefrescoTiempoReal.CATEGORIAS -> catalogoViewModel.refreshCategories()
                    AccionRefrescoTiempoReal.ANUNCIOS -> catalogoViewModel.refreshAnnouncements()
                    AccionRefrescoTiempoReal.CARRITO -> carritoYPedidosViewModel.refreshCart()
                    AccionRefrescoTiempoReal.PEDIDOS -> carritoYPedidosViewModel.refreshOrders()
                    AccionRefrescoTiempoReal.MENSAJES_SOPORTE -> perfilYSoporteViewModel.refreshMessages()
                    AccionRefrescoTiempoReal.PERFIL_USUARIO -> perfilYSoporteViewModel.refreshUserProfile()
                    AccionRefrescoTiempoReal.COMENTARIOS_ABIERTOS ->
                        interaccionesViewModel.refreshCurrentPublicationComments()
                    AccionRefrescoTiempoReal.INTERACCIONES_VISIBLES ->
                        interaccionesViewModel.refreshInteractions(forceVisibleCounts = true)
                    AccionRefrescoTiempoReal.CONTRA_ENTREGA ->
                        carritoYPedidosViewModel.loadCashOnDeliveryConfig()
                }
            }
        )
        RealtimeEvents.start(MobileSession.token(context))
        try {
            RealtimeEvents.events.collect { event ->
                coordinadorTiempoReal.procesarEvento(event)
            }
        } finally {
            coordinadorTiempoReal.cancelarTodo()
            RealtimeEvents.stop()
        }
    }

    val startDestination: RutaApp = remember {
        if (initialTransferLock != null) RutaApp.Verificacion else RutaApp.Inicio
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination.aRutaApp(fallback = startDestination)
    val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
    val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

    var requestedProfileSection by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentRoute) {
        MonitorErrores.registrarNavegacion(currentRoute.nombreAnalytics())
    }

    LaunchedEffect(initialTransferLock) {
        initialTransferLock?.let { lock ->
            activeTransferLock = lock
            carritoYPedidosViewModel.updateCheckoutTotals(
                subtotal = lock.subtotal,
                discount = lock.discount,
                shipping = lock.shipping,
                total = lock.total
            )
            if (lock.couponCode.isNotBlank()) {
                carritoYPedidosViewModel.setCouponCode(lock.couponCode)
            }
            val currentDest = navController.currentDestination
            if (currentDest != null && currentDest.aRutaApp(fallback = startDestination) != RutaApp.Verificacion) {
                navController.navigate(RutaApp.Verificacion) {
                    launchSingleTop = true
                }
            }
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

    fun navegarAPestana(destino: RutaApp) {
        if (activeTransferLock != null) return
        if (destino == RutaApp.Inicio) {
            val popped = navController.popBackStack<RutaApp.Inicio>(inclusive = false)
            if (!popped) {
                navController.navigate(RutaApp.Inicio) {
                    popUpTo(navController.graph.id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            return
        }
        val hasInicio = runCatching { navController.getBackStackEntry<RutaApp.Inicio>() }.isSuccess
        navController.navigate(destino) {
            if (hasInicio) {
                popUpTo<RutaApp.Inicio> { saveState = true }
                restoreState = true
            }
            launchSingleTop = true
        }
    }

    fun navigateTo(destino: RutaApp) {
        // Mientras el bloqueo de transferencia esté activo, no se permite navegar fuera de Verificación.
        if (activeTransferLock != null && destino != RutaApp.Verificacion) {
            return
        }
        if (destino.tieneBarraInferior()) {
            navegarAPestana(destino)
            return
        }
        if (destino == RutaApp.Gracia) {
            val hasInicio = runCatching { navController.getBackStackEntry<RutaApp.Inicio>() }.isSuccess
            navController.navigate(RutaApp.Gracia) {
                if (hasInicio) {
                    popUpTo<RutaApp.Inicio> { inclusive = false }
                } else {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
                launchSingleTop = true
            }
            return
        }
        navController.navigate(destino) {
            launchSingleTop = destino !is RutaApp.Producto
        }
    }

    fun navigateBack(fallback: RutaApp = RutaApp.Inicio) {
        if (activeTransferLock != null) return
        if (!navController.popBackStack()) {
            navigateTo(fallback)
        }
    }

    val eleganNotifier = remember { EleganNotificationState() }

    CompositionLocalProvider(LocalEleganNotifier provides eleganNotifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color.White,
                bottomBar = {
                    if (currentRoute.tieneBarraInferior()) {
                        EleganBottomBar(
                            currentRoute = currentRoute,
                            cartItemCount = totalCartCount,
                            onNavigateToInicio = { navigateTo(RutaApp.Inicio) },
                            onNavigateToCatalogo = {
                                catalogoViewModel.setCategory(null)
                                navigateTo(RutaApp.Catalogo)
                            },
                            onNavigateToCarrito = { navigateTo(RutaApp.Carrito) },
                            onNavigateToChats = { navigateTo(RutaApp.Chats) },
                            onNavigateToPerfil = { navigateTo(RutaApp.Perfil) }
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding),
                    enterTransition = {
                        obtenerEnterTransition(
                            origen = initialState.destination.aRutaApp(),
                            destino = targetState.destination.aRutaApp()
                        )
                    },
                    exitTransition = {
                        obtenerExitTransition(
                            origen = initialState.destination.aRutaApp(),
                            destino = targetState.destination.aRutaApp()
                        )
                    },
                    popEnterTransition = {
                        obtenerEnterTransition(
                            origen = initialState.destination.aRutaApp(),
                            destino = targetState.destination.aRutaApp()
                        )
                    },
                    popExitTransition = {
                        obtenerExitTransition(
                            origen = initialState.destination.aRutaApp(),
                            destino = targetState.destination.aRutaApp()
                        )
                    }
                ) {
                    registrarRutasConBarraInferior(
                        catalogoViewModel = catalogoViewModel,
                        interaccionesViewModel = interaccionesViewModel,
                        carritoYPedidosViewModel = carritoYPedidosViewModel,
                        perfilYSoporteViewModel = perfilYSoporteViewModel,
                        isAuthenticated = isAuthenticated,
                        onRequireAuth = ::requireRegisteredUser,
                        onNavigateTo = ::navigateTo,
                        requestedProfileSection = { requestedProfileSection },
                        onProfileSectionConsumed = { requestedProfileSection = null },
                        onSignOut = {
                            if (isAuthenticated) {
                                perfilYSoporteViewModel.clearUserSessionData(
                                    onCleared = { carritoYPedidosViewModel.clearOrderState() },
                                    onComplete = onSignOut
                                )
                            } else {
                                onSignOut()
                            }
                        }
                    )
                    registrarRutasSecundarias(
                        catalogoViewModel = catalogoViewModel,
                        interaccionesViewModel = interaccionesViewModel,
                        carritoYPedidosViewModel = carritoYPedidosViewModel,
                        perfilYSoporteViewModel = perfilYSoporteViewModel,
                        isAuthenticated = isAuthenticated,
                        activeTransferLock = { activeTransferLock },
                        onRequireAuth = ::requireRegisteredUser,
                        onNavigateTo = ::navigateTo,
                        onBack = ::navigateBack,
                        onNavigateToPerfilOrders = {
                            requestedProfileSection = "orders"
                            navigateTo(RutaApp.Perfil)
                        },
                        onTransferLockChanged = { activeTransferLock = it },
                        onTransferLockCleared = {
                            AlmacenBloqueoTransferencia.limpiar(context)
                            activeTransferLock = null
                        },
                        onTransferPaymentConfirmed = {
                            AlmacenBloqueoTransferencia.limpiar(context)
                            activeTransferLock = null
                            navigateTo(RutaApp.Gracia)
                        },
                        onAccountDeleted = onAccountDeleted
                    )
                }
            }

            val hasBottomBarOverlay = currentRoute.tieneBarraInferior() ||
                currentRoute is RutaApp.Producto

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
                syncAccountData()
                val action = pendingAuthenticatedAction
                pendingAuthenticatedAction = null
                action?.invoke()
            }
        )
    }
}
