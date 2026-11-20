package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AnnouncementDestination
import com.example.data.model.announcementDestination
import com.example.data.model.productColors
import com.example.data.model.productInStock
import com.example.data.model.productSizes
import com.example.ui.CatalogoScreen
import com.example.ui.InicioScreen
import com.example.ui.PerfilScreen
import com.example.ui.viewmodel.EleganViewModel

@Composable
internal fun PantallasConBarraInferior(
    screen: Screen,
    viewModel: EleganViewModel,
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onNavigateTo: (Screen) -> Unit,
    onSignOut: () -> Unit
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    val announcements by viewModel.announcements.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val orders by viewModel.orders.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val savedProductIds by viewModel.savedProductIds.collectAsStateWithLifecycle()
    val savedProducts by viewModel.savedProducts.collectAsStateWithLifecycle()
    val publicationStats by viewModel.publicationStats.collectAsStateWithLifecycle()
    val likedPublicationIds by viewModel.likedPublicationIds.collectAsStateWithLifecycle()
    val publicationComments by viewModel.publicationComments.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsStateWithLifecycle()
    val productsError by viewModel.productsError.collectAsStateWithLifecycle()

    LaunchedEffect(screen) {
        if (screen == Screen.PERFIL) {
            viewModel.refreshOrders()
        }
    }

    Scaffold(
        contentWindowInsets = if (screen == Screen.PERFIL) {
            WindowInsets(0, 0, 0, 0)
        } else {
            ScaffoldDefaults.contentWindowInsets
        },
        bottomBar = {
            EleganBottomBar(
                currentScreen = screen,
                cartItemCount = cartItems.sumOf { it.quantity },
                onNavigateToInicio = { onNavigateTo(Screen.INICIO) },
                onNavigateToCatalogo = {
                    viewModel.setCategory(null)
                    onNavigateTo(Screen.CATALOGO)
                },
                onNavigateToCarrito = { onNavigateTo(Screen.CARRITO) },
                onNavigateToChats = { onNavigateTo(Screen.CHATS) },
                onNavigateToPerfil = { onNavigateTo(Screen.PERFIL) }
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            when (screen) {
                Screen.INICIO -> {
                    InicioScreen(
                        featuredProducts = products.filter { it.productInStock() },
                        categories = categories,
                        announcements = announcements,
                        userProfile = userProfile,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        savedProductIds = savedProductIds,
                        publicationStats = publicationStats,
                        likedPublicationIds = likedPublicationIds,
                        publicationComments = publicationComments,
                        isAuthenticated = isAuthenticated,
                        onRequireAuth = onRequireAuth,
                        onToggleLike = { id, onResult ->
                            onRequireAuth("Regístrate para dar me gusta a tus publicaciones favoritas.") {
                                viewModel.togglePublicationLike(id, onResult)
                            }
                        },
                        onOpenComments = viewModel::openPublicationComments,
                        onSubmitComment = { productId, displayName, text, onResult ->
                            onRequireAuth("Regístrate para comentar en las publicaciones.") {
                                viewModel.addPublicationComment(productId, displayName, text, onResult)
                            }
                        },
                        onRecordShare = { productId, onResult ->
                            viewModel.recordPublicationShare(productId, onResult)
                        },
                        onToggleSaveProduct = { id, onResult ->
                            onRequireAuth("Regístrate para guardar publicaciones en tu perfil.") {
                                viewModel.toggleSaveProduct(id, onResult)
                            }
                        },
                        onNavigateToCatalogo = { cat ->
                            viewModel.setCategory(cat)
                            onNavigateTo(Screen.CATALOGO)
                        },
                        onAnnouncementClick = { announcement ->
                            when (val destination = announcement.announcementDestination()) {
                                is AnnouncementDestination.Product -> {
                                    viewModel.selectProductById(
                                        productId = destination.productId,
                                        onFound = { onNavigateTo(Screen.PRODUCTO) },
                                        onNotFound = {
                                            viewModel.setCategory(null)
                                            onNavigateTo(Screen.CATALOGO)
                                        }
                                    )
                                }
                                is AnnouncementDestination.Category -> {
                                    val resolvedCategoryId = categories
                                        .firstOrNull { it.id == destination.categoryId }?.id
                                        ?: categories.firstOrNull {
                                            it.id.equals(destination.categoryId, ignoreCase = true)
                                        }?.id
                                        ?: destination.categoryId
                                    viewModel.setCategory(resolvedCategoryId)
                                    onNavigateTo(Screen.CATALOGO)
                                }
                                AnnouncementDestination.None -> {
                                    viewModel.setCategory(null)
                                    onNavigateTo(Screen.CATALOGO)
                                }
                            }
                        },
                        onNavigateToProduct = { prod ->
                            viewModel.selectProduct(prod)
                            onNavigateTo(Screen.PRODUCTO)
                        },
                        onNavigateToMenu = { onNavigateTo(Screen.CATEGORIA) },
                        onNavigateToCategoryMenu = { onNavigateTo(Screen.CATEGORIA) },
                        onNavigateToChats = { onNavigateTo(Screen.CHATS) },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        isLoadingProducts = isLoadingProducts,
                        productsError = productsError,
                    onRefreshProducts = {
                        viewModel.refreshProducts()
                        viewModel.refreshCategories()
                    }
                    )
                }

                Screen.CATALOGO -> {
                    BackHandler { onNavigateTo(Screen.INICIO) }
                    CatalogoScreen(
                        products = products.filter { it.productInStock() },
                        categories = categories,
                        selectedCategory = selectedCategory,
                        onCategoryChange = { viewModel.setCategory(it) },
                        onNavigateToMenu = { onNavigateTo(Screen.CATEGORIA) },
                        onProductClick = { prod ->
                            viewModel.selectProduct(prod)
                            onNavigateTo(Screen.PRODUCTO)
                        },
                        onQuickAddToCart = { prod ->
                            onRequireAuth("Regístrate para agregar productos a tu carrito.") {
                                val defaultSize = prod.productSizes().firstOrNull() ?: "Estándar"
                                val defaultColor = prod.productColors().firstOrNull().orEmpty()
                                viewModel.addToCart(prod, defaultSize, 1L, defaultColor)
                            }
                        },
                        cartItemCount = cartItems.sumOf { it.quantity },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        savedProductIds = savedProductIds,
                        isAuthenticated = isAuthenticated,
                        onRequireAuth = onRequireAuth,
                        onToggleSaveProduct = { id ->
                            onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                                viewModel.toggleSaveProduct(id)
                            }
                        },
                        isLoadingProducts = isLoadingProducts,
                        productsError = productsError,
                        onRefreshProducts = {
                            viewModel.refreshProducts()
                            viewModel.refreshCategories()
                        }
                    )
                }

                Screen.PERFIL -> {
                    BackHandler { onNavigateTo(Screen.INICIO) }
                    PerfilScreen(
                        userProfile = userProfile,
                        orders = orders,
                        savedProducts = savedProducts,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        isAuthenticated = isAuthenticated,
                        onRequireAuth = onRequireAuth,
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        onProductClick = { prod ->
                            viewModel.selectProduct(prod)
                            onNavigateTo(Screen.PRODUCTO)
                        },
                        onExploreClick = { onNavigateTo(Screen.INICIO) },
                        onSaveProfile = { nm, em, ph, ad, ct, onResult ->
                            onRequireAuth("Regístrate para guardar tus datos de entrega.") {
                                viewModel.saveProfile(nm, em, ph, ad, ct, onResult)
                            }
                        },
                        onNavigateToChats = { onNavigateTo(Screen.CHATS) },
                        onNavigateToCategoryMenu = { onNavigateTo(Screen.CATEGORIA) },
                        onSignOut = onSignOut
                    )
                }

                else -> {}
            }
        }
    }
}
