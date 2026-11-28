package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.components.SavedPublicationsGrid
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    userProfile: UserProfile?,
    orders: List<Order>,
    savedProducts: List<Product> = emptyList(),
    catalogProducts: List<Product> = emptyList(),
    cartItemCount: Long = 0L,
    sessionUserName: String = "",
    sessionUserEmail: String = "",
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onNavigateToCart: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSaveProfile: (String, String, String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToCategoryMenu: () -> Unit = {},
    requestedSection: String? = null,
    onRequestedSectionConsumed: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val fallbackName = sessionUserName.takeIf { it.isNotBlank() }
    val fallbackEmail = sessionUserEmail.takeIf { it.isNotBlank() }

    var name by remember(userProfile, isAuthenticated, fallbackName) {
        mutableStateOf(userProfile?.displayName?.takeIf { it.isNotBlank() } ?: fallbackName.orEmpty())
    }
    var email by remember(userProfile, isAuthenticated, fallbackEmail) {
        mutableStateOf(userProfile?.email?.takeIf { it.isNotBlank() } ?: fallbackEmail.orEmpty())
    }
    var phone by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }
    var address by remember(userProfile) { mutableStateOf(userProfile?.address ?: "") }
    var city by remember(userProfile) { mutableStateOf(userProfile?.city ?: "") }
    val hasSavedDelivery = userProfile != null && (
        userProfile.phone.isNotBlank() || userProfile.address.isNotBlank() || userProfile.city.isNotBlank()
        )
    var isEditingDelivery by remember(userProfile) { mutableStateOf(!hasSavedDelivery) }

    val notifier = LocalEleganNotifier.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var selectedProfileSection by remember {
        mutableStateOf(requestedSection?.takeIf { it.isNotBlank() } ?: "saved")
    }

    LaunchedEffect(requestedSection) {
        val target = requestedSection?.takeIf { it.isNotBlank() }
        if (target != null) {
            selectedProfileSection = target
            onRequestedSectionConsumed()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            val isCompact = maxWidth < 350.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                maxWidth >= 600.dp -> 24.dp
                else -> 16.dp
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color.White).testTag("perfil_screen"),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
            ) {
                item {
                    TarjetaIdentidadPerfil(
                        displayName = (
                            userProfile?.displayName?.takeIf { it.isNotBlank() }
                                ?: fallbackName
                                ?: if (isAuthenticated) "Caballero Elegan" else "Visitante Elegan"
                            ).trim(),
                        email = userProfile?.email?.takeIf { it.isNotBlank() }
                            ?: fallbackEmail
                            ?: if (isAuthenticated) "" else "Explora libremente · Crea tu cuenta al interactuar",
                        isCompact = isCompact
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                            .padding(horizontal = horizontalPad)
                            .testTag("profile_sections_container"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            SelectorSeccionesPerfil(
                                selectedSection = selectedProfileSection,
                                onSelectSection = { selectedProfileSection = it }
                            )
                            HorizontalDivider(color = Color(0xFFEAE5E0))
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                                when (selectedProfileSection) {
                                    "saved" -> SavedPublicationsGrid(
                                        savedProducts = savedProducts,
                                        onProductClick = onProductClick,
                                        onExploreClick = onExploreClick,
                                        modifier = Modifier.fillMaxWidth().testTag("profile_saved_section")
                                    )
                                    "delivery" -> SeccionEntregaPerfil(
                                        name = name,
                                        onNameChange = { name = it },
                                        email = email,
                                        onEmailChange = { email = it },
                                        phone = phone,
                                        onPhoneChange = { phone = it },
                                        address = address,
                                        onAddressChange = { address = it },
                                        city = city,
                                        onCityChange = { city = it },
                                        isEditingDelivery = isEditingDelivery,
                                        onStartEdit = { isEditingDelivery = true },
                                        onSaveClick = {
                                            val executeSave: () -> Unit = {
                                                val cleanPhone = phone.trim()
                                                val cleanAddress = address.trim()
                                                val cleanCity = city.trim()
                                                val cleanName = name.trim().ifBlank { fallbackName.orEmpty().trim() }
                                                val cleanEmail = email.trim().ifBlank { fallbackEmail.orEmpty().trim() }
                                                if (cleanName.isBlank() || cleanEmail.isBlank()) {
                                                    val msg = "El nombre y el correo son obligatorios."
                                                    if (notifier != null) notifier.show(msg)
                                                    else scope.launch { snackbarHostState.showSnackbar(msg) }
                                                } else {
                                                    name = cleanName
                                                    email = cleanEmail
                                                    phone = cleanPhone
                                                    address = cleanAddress
                                                    city = cleanCity
                                                    onSaveProfile(cleanName, cleanEmail, cleanPhone, cleanAddress, cleanCity) { result ->
                                                        result.onSuccess {
                                                            isEditingDelivery = false
                                                            if (notifier != null) notifier.show("Datos guardados correctamente en tu cuenta")
                                                            else scope.launch { snackbarHostState.showSnackbar("Datos guardados correctamente en tu cuenta") }
                                                        }.onFailure { error ->
                                                            val msg = error.message?.takeIf { it.isNotBlank() }
                                                                ?: "No se pudieron guardar los datos en el servidor."
                                                            if (notifier != null) notifier.show(msg)
                                                            else scope.launch { snackbarHostState.showSnackbar(msg) }
                                                        }
                                                    }
                                                }
                                            }
                                            if (!isAuthenticated) {
                                                onRequireAuth("Regístrate para guardar tus datos de entrega.", executeSave)
                                            } else {
                                                executeSave()
                                            }
                                        }
                                    )
                                    "orders" -> SeccionPedidosPerfil(
                                        orders = orders,
                                        catalogProducts = catalogProducts
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    TarjetaSoporteVipPerfil(
                        horizontalPad = horizontalPad,
                        isCompact = isCompact,
                        onNavigateToChats = onNavigateToChats
                    )
                }

                if (!isAuthenticated) {
                    item {
                        BotonRegistroInvitadoPerfil(
                            horizontalPad = horizontalPad,
                            onRequireAuth = {
                                onRequireAuth("Crea tu cuenta para guardar favoritos, pedidos y datos de envío.") {}
                            }
                        )
                    }
                }
            }

            CabeceraSuperiorPerfil(
                cartItemCount = cartItemCount,
                onNavigateToCategoryMenu = onNavigateToCategoryMenu,
                onNavigateToCart = onNavigateToCart
            )
        }
    }
}
