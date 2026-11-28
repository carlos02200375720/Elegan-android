package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.example.data.model.AnnouncementDestination
import com.example.data.model.announcementDestination
import com.example.data.model.productColors
import com.example.data.model.productId
import com.example.data.model.productSizes
import com.example.ui.CatalogoScreen
import com.example.ui.InicioScreen
import com.example.ui.PerfilScreen
import com.example.ui.viewmodel.CarritoYPedidosViewModel
import com.example.ui.viewmodel.CatalogoViewModel
import com.example.ui.viewmodel.InteraccionesViewModel
import com.example.ui.viewmodel.PerfilYSoporteViewModel

internal fun NavGraphBuilder.registrarRutasConBarraInferior(
    catalogoViewModel: CatalogoViewModel,
    interaccionesViewModel: InteraccionesViewModel,
    carritoYPedidosViewModel: CarritoYPedidosViewModel,
    perfilYSoporteViewModel: PerfilYSoporteViewModel,
    isAuthenticated: Boolean,
    onRequireAuth: (String, () -> Unit) -> Unit,
    onNavigateTo: (RutaApp) -> Unit,
    requestedProfileSection: () -> String?,
    onProfileSectionConsumed: () -> Unit,
    onSignOut: () -> Unit
) {
    composable<RutaApp.Inicio> {
        val inStockProducts by catalogoViewModel.inStockProducts.collectAsStateWithLifecycle()
        val announcements by catalogoViewModel.announcements.collectAsStateWithLifecycle()
        val categories by catalogoViewModel.categories.collectAsStateWithLifecycle()
        val isLoadingProducts by catalogoViewModel.isLoadingProducts.collectAsStateWithLifecycle()
        val productsError by catalogoViewModel.productsError.collectAsStateWithLifecycle()
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val userProfile by perfilYSoporteViewModel.userProfile.collectAsStateWithLifecycle()
        val sessionUser by perfilYSoporteViewModel.sessionUser.collectAsStateWithLifecycle()
        val savedProductIds by interaccionesViewModel.savedProductIds.collectAsStateWithLifecycle()
        val publicationStats by interaccionesViewModel.publicationStats.collectAsStateWithLifecycle()
        val likedPublicationIds by interaccionesViewModel.likedPublicationIds.collectAsStateWithLifecycle()
        val publicationComments by interaccionesViewModel.publicationComments.collectAsStateWithLifecycle()

        val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
        val topAndHorizontalInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(topAndHorizontalInsets)
        ) {
            InicioScreen(
                featuredProducts = inStockProducts,
                categories = categories,
                announcements = announcements,
                userProfile = userProfile,
                currentUserId = sessionUser?.effectiveId.orEmpty(),
                sessionUserName = sessionUser?.name.orEmpty(),
                sessionUserEmail = sessionUser?.email.orEmpty(),
                cartItemCount = totalCartCount,
                savedProductIds = savedProductIds,
                publicationStats = publicationStats,
                likedPublicationIds = likedPublicationIds,
                publicationComments = publicationComments,
                isAuthenticated = isAuthenticated,
                onRequireAuth = onRequireAuth,
                onToggleLike = { id, onResult ->
                    onRequireAuth("Regístrate para dar me gusta a tus publicaciones favoritas.") {
                        interaccionesViewModel.togglePublicationLike(id, onResult)
                    }
                },
                onOpenComments = interaccionesViewModel::openPublicationComments,
                onSubmitComment = { productId, displayName, text, onResult ->
                    onRequireAuth("Regístrate para comentar en las publicaciones.") {
                        interaccionesViewModel.addPublicationComment(productId, displayName, text, onResult)
                    }
                },
                onReportComment = { commentId, onResult ->
                    onRequireAuth("Inicia sesión para reportar comentarios.") {
                        interaccionesViewModel.reportPublicationComment(commentId, onResult = onResult)
                    }
                },
                onBlockUser = { userId, onResult ->
                    onRequireAuth("Inicia sesión para bloquear usuarios.") {
                        interaccionesViewModel.blockUser(userId, onResult)
                    }
                },
                onRecordShare = { productId, onResult ->
                    interaccionesViewModel.recordPublicationShare(productId, onResult)
                },
                onToggleSaveProduct = { id, onResult ->
                    onRequireAuth("Regístrate para guardar publicaciones en tu perfil.") {
                        interaccionesViewModel.toggleSaveProduct(id, onResult)
                    }
                },
                onNavigateToCatalogo = { cat ->
                    catalogoViewModel.setCategory(cat)
                    onNavigateTo(RutaApp.Catalogo)
                },
                onAnnouncementClick = { announcement ->
                    when (val destination = announcement.announcementDestination()) {
                        is AnnouncementDestination.Product -> {
                            catalogoViewModel.selectProductById(
                                productId = destination.productId,
                                onFound = { onNavigateTo(RutaApp.Producto(destination.productId)) },
                                onNotFound = {
                                    catalogoViewModel.setCategory(null)
                                    onNavigateTo(RutaApp.Catalogo)
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
                            catalogoViewModel.setCategory(resolvedCategoryId)
                            onNavigateTo(RutaApp.Catalogo)
                        }
                        AnnouncementDestination.None -> {
                            catalogoViewModel.setCategory(null)
                            onNavigateTo(RutaApp.Catalogo)
                        }
                    }
                },
                onNavigateToProduct = { prod ->
                    catalogoViewModel.selectProduct(prod)
                    onNavigateTo(RutaApp.Producto(prod.productId()))
                },
                onNavigateToMenu = { onNavigateTo(RutaApp.Categoria) },
                onNavigateToCategoryMenu = { onNavigateTo(RutaApp.Categoria) },
                onNavigateToChats = { onNavigateTo(RutaApp.Chats) },
                onNavigateToCart = { onNavigateTo(RutaApp.Carrito) },
                isLoadingProducts = isLoadingProducts,
                productsError = productsError,
                onRefreshProducts = {
                    catalogoViewModel.refreshProducts()
                    catalogoViewModel.refreshCategories()
                },
                onVisibleProductsChanged = interaccionesViewModel::onVisibleProductsChanged
            )
        }
    }

    composable<RutaApp.Catalogo> {
        val inStockProducts by catalogoViewModel.inStockProducts.collectAsStateWithLifecycle()
        val categories by catalogoViewModel.categories.collectAsStateWithLifecycle()
        val categoryTitleById by catalogoViewModel.categoryTitleById.collectAsStateWithLifecycle()
        val selectedCategory by catalogoViewModel.selectedCategory.collectAsStateWithLifecycle()
        val isLoadingProducts by catalogoViewModel.isLoadingProducts.collectAsStateWithLifecycle()
        val productsError by catalogoViewModel.productsError.collectAsStateWithLifecycle()
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val savedProductIds by interaccionesViewModel.savedProductIds.collectAsStateWithLifecycle()

        val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }
        val topAndHorizontalInsets = ScaffoldDefaults.contentWindowInsets
            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(topAndHorizontalInsets)
        ) {
            CatalogoScreen(
                products = inStockProducts,
                categories = categories,
                categoryTitleById = categoryTitleById,
                selectedCategory = selectedCategory,
                onCategoryChange = { catalogoViewModel.setCategory(it) },
                onNavigateToMenu = { onNavigateTo(RutaApp.Categoria) },
                onProductClick = { prod ->
                    catalogoViewModel.selectProduct(prod)
                    onNavigateTo(RutaApp.Producto(prod.productId()))
                },
                onQuickAddToCart = { prod ->
                    onRequireAuth("Regístrate para agregar productos a tu carrito.") {
                        val defaultSize = prod.productSizes().firstOrNull() ?: "Estándar"
                        val defaultColor = prod.productColors().firstOrNull().orEmpty()
                        carritoYPedidosViewModel.addToCart(prod, defaultSize, 1L, defaultColor)
                    }
                },
                cartItemCount = totalCartCount,
                onNavigateToCart = { onNavigateTo(RutaApp.Carrito) },
                savedProductIds = savedProductIds,
                isAuthenticated = isAuthenticated,
                onRequireAuth = onRequireAuth,
                onToggleSaveProduct = { id ->
                    onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                        interaccionesViewModel.toggleSaveProduct(id)
                    }
                },
                isLoadingProducts = isLoadingProducts,
                productsError = productsError,
                onRefreshProducts = {
                    catalogoViewModel.refreshProducts()
                    catalogoViewModel.refreshCategories()
                }
            )
        }
    }

    composable<RutaApp.Perfil> {
        val products by catalogoViewModel.products.collectAsStateWithLifecycle()
        val cartItems by carritoYPedidosViewModel.cartItems.collectAsStateWithLifecycle()
        val orders by carritoYPedidosViewModel.orders.collectAsStateWithLifecycle()
        val userProfile by perfilYSoporteViewModel.userProfile.collectAsStateWithLifecycle()
        val sessionUser by perfilYSoporteViewModel.sessionUser.collectAsStateWithLifecycle()
        val savedProducts by interaccionesViewModel.savedProducts.collectAsStateWithLifecycle()

        val totalCartCount = remember(cartItems) { cartItems.sumOf { it.quantity } }

        LaunchedEffect(Unit) {
            carritoYPedidosViewModel.refreshOrders()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            PerfilScreen(
                userProfile = userProfile,
                orders = orders,
                savedProducts = savedProducts,
                catalogProducts = products,
                cartItemCount = totalCartCount,
                sessionUserName = sessionUser?.name.orEmpty(),
                sessionUserEmail = sessionUser?.email.orEmpty(),
                isAuthenticated = isAuthenticated,
                onRequireAuth = onRequireAuth,
                onNavigateToCart = { onNavigateTo(RutaApp.Carrito) },
                onProductClick = { prod ->
                    catalogoViewModel.selectProduct(prod)
                    onNavigateTo(RutaApp.Producto(prod.productId()))
                },
                onExploreClick = { onNavigateTo(RutaApp.Inicio) },
                onSaveProfile = { nm, em, ph, ad, ct, onResult ->
                    onRequireAuth("Regístrate para guardar tus datos de entrega.") {
                        perfilYSoporteViewModel.saveProfile(nm, em, ph, ad, ct, onResult)
                    }
                },
                onNavigateToChats = { onNavigateTo(RutaApp.Chats) },
                onNavigateToCategoryMenu = { onNavigateTo(RutaApp.Categoria) },
                requestedSection = requestedProfileSection(),
                onRequestedSectionConsumed = onProfileSectionConsumed,
                onSignOut = onSignOut
            )
        }
    }
}
