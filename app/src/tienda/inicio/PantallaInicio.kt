package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.AnnouncementJson
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.PublicationComment
import com.example.data.model.PublicationStats
import com.example.data.model.UserProfile
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productVideoUrl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InicioScreen(
    featuredProducts: List<ProductJson>,
    categories: List<CategoryItem> = emptyList(),
    announcements: List<AnnouncementJson> = emptyList(),
    userProfile: UserProfile? = null,
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
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCatalogo: (String?) -> Unit,
    onAnnouncementClick: (AnnouncementJson) -> Unit = {},
    onNavigateToProduct: (ProductJson) -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToCategoryMenu: () -> Unit = onNavigateToMenu,
    onNavigateToChats: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val videoProductIds = remember(featuredProducts) {
        featuredProducts.asSequence()
            .filter { it.productHasVideo() && it.productVideoUrl().isNotBlank() }
            .map { it.productId() }
            .toSet()
    }
    var focusedProductId by remember { mutableStateOf<String?>(null) }
    var localSavedProductIds by remember { mutableStateOf(setOf<String>()) }
    var activeCommentProduct by remember { mutableStateOf<ProductJson?>(null) }

    EfectoFocoVideoInicio(
        listState = listState,
        videoProductIds = videoProductIds,
        onFocusedProductChange = { focusedProductId = it }
    )
    EfectoPrecargaImagenesInicio(
        listState = listState,
        featuredProducts = featuredProducts
    )

    val displayName = resolverNombreSaludoInicio(context, userProfile)

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
            focusedProductId = focusedProductId,
            isLoadingProducts = isLoadingProducts,
            productsError = productsError,
            onRefreshProducts = onRefreshProducts,
            onToggleSaveProduct = onToggleSaveProduct,
            onToggleLocalSave = { productId, willSave ->
                localSavedProductIds = if (willSave) {
                    localSavedProductIds + productId
                } else {
                    localSavedProductIds - productId
                }
            },
            onToggleLike = onToggleLike,
            onOpenCommentsForProduct = { prod ->
                activeCommentProduct = prod
                onOpenComments(prod.productId())
            },
            onRecordShare = onRecordShare,
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
            isAuthenticated = isAuthenticated,
            onRequireAuth = onRequireAuth,
            onDismiss = {
                activeCommentProduct = null
                onOpenComments(null)
            },
            onSubmitComment = onSubmitComment
        )
    }
}
