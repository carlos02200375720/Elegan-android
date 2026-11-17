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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onNavigateTo: (Screen) -> Unit,
    onSignOut: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val cartItems by viewModel.cartItems.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val savedProductIds by viewModel.savedProductIds.collectAsState()
    val savedProducts by viewModel.savedProducts.collectAsState()
    val publicationStats by viewModel.publicationStats.collectAsState()
    val likedPublicationIds by viewModel.likedPublicationIds.collectAsState()
    val publicationComments by viewModel.publicationComments.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoadingProducts by viewModel.isLoadingProducts.collectAsState()
    val productsError by viewModel.productsError.collectAsState()

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
                    viewModel.setCategory("todo")
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
                        announcements = announcements,
                        userProfile = userProfile,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        savedProductIds = savedProductIds,
                        publicationStats = publicationStats,
                        likedPublicationIds = likedPublicationIds,
                        publicationComments = publicationComments,
                        onToggleLike = { id, onResult -> viewModel.togglePublicationLike(id, onResult) },
                        onOpenComments = viewModel::openPublicationComments,
                        onSubmitComment = { productId, displayName, text, onResult ->
                            viewModel.addPublicationComment(productId, displayName, text, onResult)
                        },
                        onRecordShare = { productId, onResult ->
                            viewModel.recordPublicationShare(productId, onResult)
                        },
                        onToggleSaveProduct = { id, onResult -> viewModel.toggleSaveProduct(id, onResult) },
                        onNavigateToCatalogo = { cat ->
                            viewModel.setCategory(cat)
                            onNavigateTo(Screen.CATALOGO)
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
                        onRefreshProducts = { viewModel.refreshProducts() }
                    )
                }

                Screen.CATALOGO -> {
                    BackHandler { onNavigateTo(Screen.INICIO) }
                    CatalogoScreen(
                        products = products.filter { it.productInStock() },
                        selectedCategory = selectedCategory,
                        onCategoryChange = { viewModel.setCategory(it) },
                        onNavigateToMenu = { onNavigateTo(Screen.CATEGORIA) },
                        onProductClick = { prod ->
                            viewModel.selectProduct(prod)
                            onNavigateTo(Screen.PRODUCTO)
                        },
                        onQuickAddToCart = { prod ->
                            val defaultSize = prod.productSizes().firstOrNull() ?: "Estándar"
                            val defaultColor = prod.productColors().firstOrNull().orEmpty()
                            viewModel.addToCart(prod, defaultSize, 1L, defaultColor)
                        },
                        cartItemCount = cartItems.sumOf { it.quantity },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        savedProductIds = savedProductIds,
                        onToggleSaveProduct = { id -> viewModel.toggleSaveProduct(id) },
                        isLoadingProducts = isLoadingProducts,
                        productsError = productsError,
                        onRefreshProducts = { viewModel.refreshProducts() }
                    )
                }

                Screen.PERFIL -> {
                    BackHandler { onNavigateTo(Screen.INICIO) }
                    PerfilScreen(
                        userProfile = userProfile,
                        orders = orders,
                        savedProducts = savedProducts,
                        cartItemCount = cartItems.sumOf { it.quantity },
                        onNavigateToCart = { onNavigateTo(Screen.CARRITO) },
                        onProductClick = { prod ->
                            viewModel.selectProduct(prod)
                            onNavigateTo(Screen.PRODUCTO)
                        },
                        onExploreClick = { onNavigateTo(Screen.INICIO) },
                        onSaveProfile = { nm, em, ph, ad, ct, onResult -> viewModel.saveProfile(nm, em, ph, ad, ct, onResult) },
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
