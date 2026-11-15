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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.remote.MobileSession
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.components.SavedPublicationsGrid
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    userProfile: UserProfile?,
    orders: List<Order>,
    savedProducts: List<Product> = emptyList(),
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    onProductClick: (Product) -> Unit = {},
    onExploreClick: () -> Unit = {},
    onSaveProfile: (name: String, email: String, phone: String, address: String, city: String) -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToCategoryMenu: () -> Unit = {},
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val user = remember { MobileSession.user(context) }

    var name by remember(userProfile) { mutableStateOf(userProfile?.displayName ?: user?.name.orEmpty()) }
    var email by remember(userProfile) { mutableStateOf(userProfile?.email ?: user?.email.orEmpty()) }
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
    var selectedProfileSection by remember { mutableStateOf("saved") }

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
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCompact) 12.dp else 16.dp)
            ) {
                item {
                    TarjetaIdentidadPerfil(
                        photoUrl = user?.photoUrl?.toString() ?: userProfile?.photoUrl,
                        displayName = (userProfile?.displayName ?: user?.name ?: "Caballero Elegan").trim(),
                        email = userProfile?.email ?: user?.email ?: "",
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
                                            val cleanPhone = phone.trim()
                                            val cleanAddress = address.trim()
                                            val cleanCity = city.trim()
                                            val cleanName = name.trim()
                                            val cleanEmail = email.trim()
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
                                                onSaveProfile(cleanName, cleanEmail, cleanPhone, cleanAddress, cleanCity)
                                                isEditingDelivery = false
                                                if (notifier != null) notifier.show("Datos de entrega guardados")
                                                else scope.launch { snackbarHostState.showSnackbar("Datos guardados correctamente") }
                                            }
                                        }
                                    )
                                    "orders" -> SeccionPedidosPerfil(orders = orders)
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

                item {
                    BotonCerrarSesionPerfil(horizontalPad = horizontalPad, onSignOut = onSignOut)
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
