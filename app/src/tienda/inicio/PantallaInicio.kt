package com.example.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListPrefetchStrategy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Announcement
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.UserProfile
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productVideoUrl
import com.example.ui.components.LocalEleganNotifier

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InicioScreen(
    featuredProducts: List<Product>,
    categories: List<CategoryItem> = emptyList(),
    announcements: List<Announcement> = emptyList(),
    userProfile: UserProfile? = null,
    currentUserId: String = "",
    sessionUserName: String = "",
    sessionUserEmail: String = "",
    cartItemCount: Long = 0L,
    savedProductIds: Set<String> = emptySet(),
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)? = null,
    publicationStats: Map<String, PublicationStats>,
    likedPublicationIds: Set<String>,
    publicationComments: List<PublicationComment>,
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenComments: (String?) -> Unit,
    onSubmitComment: (String, String, String, (Result<Unit>) -> Unit) -> Unit,
    onReportComment: (String, (Result<Unit>) -> Unit) -> Unit = { _, onResult -> onResult(Result.success(Unit)) },
    onBlockUser: (String, (Result<Unit>) -> Unit) -> Unit = { _, onResult -> onResult(Result.success(Unit)) },
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCatalogo: (String?) -> Unit,
    onAnnouncementClick: (Announcement) -> Unit = {},
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToCategoryMenu: () -> Unit = onNavigateToMenu,
    onNavigateToChats: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {},
    onVisibleProductsChanged: (List<String>) -> Unit = {}
) {
    val prefetchStrategy = remember { LazyListPrefetchStrategy(nestedPrefetchItemCount = 3) }
    val listState = rememberLazyListState(prefetchStrategy = prefetchStrategy)
    val context = LocalContext.current
    val notifier = LocalEleganNotifier.current
    val videoProductIds = remember(featuredProducts) {
        featuredProducts.asSequence()
            .filter { it.productHasVideo() }
            .map { it.productId() }
            .toSet()
    }
    val headerItemCount = remember(categories.isNotEmpty(), announcements.isNotEmpty()) {
        2 + (if (categories.isNotEmpty()) 1 else 0) + (if (announcements.isNotEmpty()) 1 else 0)
    }
    val focusedProductIdState = remember { mutableStateOf<String?>(null) }
    var localSavedProductIds by remember { mutableStateOf(setOf<String>()) }
    var activeCommentProduct by remember { mutableStateOf<Product?>(null) }
    var pendingShareProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingShareRequestId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(onRecordShare, notifier) {
        DespachadorCompartidoPublicacion.confirmaciones.collect { evento ->
            if (evento.productId == pendingShareProductId && evento.requestId == pendingShareRequestId) {
                pendingShareProductId = null
                pendingShareRequestId = null
                onRecordShare(evento.productId) { result ->
                    if (result.isFailure) {
                        notifier?.show("No se pudo registrar el compartido")
                    }
                }
            }
        }
    }

    EfectoFocoVideoInicio(
        listState = listState,
        videoProductIds = videoProductIds,
        focusedProductIdState = focusedProductIdState
    )
    EfectoConteosVisiblesInicio(
        listState = listState,
        featuredProducts = featuredProducts,
        headerItemCount = headerItemCount,
        onVisibleProductsChanged = onVisibleProductsChanged
    )
    EfectoPrecargaImagenesInicio(
        listState = listState,
        featuredProducts = featuredProducts,
        headerItemCount = headerItemCount
    )

    val displayName = remember(userProfile, sessionUserName, sessionUserEmail) {
        resolverNombreSaludoInicio(userProfile, sessionUserName, sessionUserEmail)
    }
    val onToggleLocalSave = remember {
        { productId: String, willSave: Boolean ->
            localSavedProductIds = if (willSave) {
                localSavedProductIds + productId
            } else {
                localSavedProductIds - productId
            }
        }
    }
    val onOpenCommentsForProduct = remember(onOpenComments) {
        { prod: Product ->
            activeCommentProduct = prod
            onOpenComments(prod.productId())
        }
    }
    val onShareProduct = remember(context) {
        { prod: Product ->
            val productId = prod.productId()
            pendingShareProductId = productId
            pendingShareRequestId = lanzarSelectorCompartirPublicacion(
                context = context,
                productId = productId,
                productName = prod.productName(),
                productPrice = prod.productPrice()
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        InicioTopBar(
            displayName = displayName,
            cartItemCount = cartItemCount,
            onNavigateToCategoryMenu = onNavigateToCategoryMenu,
            onNavigateToCart = onNavigateToCart
        )

        ContenidoFeedInicio(
            listState = listState,
            featuredProducts = featuredProducts,
            categories = categories,
            announcements = announcements,
            savedProductIds = savedProductIds,
            localSavedProductIds = localSavedProductIds,
            publicationStats = publicationStats,
            likedPublicationIds = likedPublicationIds,
            focusedProductIdState = focusedProductIdState,
            isLoadingProducts = isLoadingProducts,
            productsError = productsError,
            onRefreshProducts = onRefreshProducts,
            onToggleSaveProduct = onToggleSaveProduct,
            onToggleLocalSave = onToggleLocalSave,
            onToggleLike = onToggleLike,
            onOpenCommentsForProduct = onOpenCommentsForProduct,
            onShareProduct = onShareProduct,
            onNavigateToCatalogo = onNavigateToCatalogo,
            onAnnouncementClick = onAnnouncementClick,
            onNavigateToProduct = onNavigateToProduct,
            onNavigateToMenu = onNavigateToMenu,
            onNavigateToChats = onNavigateToChats,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
    }

    activeCommentProduct?.let { prod ->
        PublicationCommentsBottomSheet(
            product = prod,
            displayName = displayName,
            comments = publicationComments,
            currentUserId = currentUserId,
            isAuthenticated = isAuthenticated,
            onRequireAuth = onRequireAuth,
            onDismiss = {
                activeCommentProduct = null
                onOpenComments(null)
            },
            onSubmitComment = onSubmitComment,
            onReportComment = onReportComment,
            onBlockUser = onBlockUser
        )
    }
}
