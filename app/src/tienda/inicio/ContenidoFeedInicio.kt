package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Product
import com.example.data.model.PublicationStats

@Composable
internal fun ContenidoFeedInicio(
    listState: LazyListState,
    featuredProducts: List<Product>,
    savedProductIds: Set<String>,
    localSavedProductIds: Set<String>,
    publicationStats: Map<String, PublicationStats>,
    likedPublicationIds: Set<String>,
    focusedProductId: String?,
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenCommentsForProduct: (Product) -> Unit,
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCatalogo: (String) -> Unit,
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToChats: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val isExpanded = maxWidth >= 600.dp
        val feedItemModifier = if (isExpanded) {
            Modifier.fillMaxWidth().widthIn(max = 620.dp)
        } else {
            Modifier.fillMaxWidth()
        }
        val categoriesModifier = if (isExpanded) {
            Modifier.fillMaxWidth().widthIn(max = 840.dp)
        } else {
            Modifier.fillMaxWidth()
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .testTag("inicio_screen"),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "hero_banner") {
                InicioHeroBanner(
                    onNavigateToCatalogo = onNavigateToCatalogo,
                    onNavigateToMenu = onNavigateToMenu
                )
            }

            item(key = "categories_section") {
                CategoryStoriesSection(
                    onNavigateToCatalogo = onNavigateToCatalogo,
                    onNavigateToMenu = onNavigateToMenu,
                    modifier = categoriesModifier
                )
            }

            item(key = "feed_spacer") {
                Spacer(modifier = Modifier.height(if (maxWidth < 360.dp) 12.dp else 16.dp))
            }

            itemsIndexed(
                items = featuredProducts,
                key = { _, prod -> prod.id },
                contentType = { _, _ -> "product_post" }
            ) { index, prod ->
                val isLiked = prod.id in likedPublicationIds
                val isSaved = if (onToggleSaveProduct != null) {
                    savedProductIds.contains(prod.id)
                } else {
                    localSavedProductIds.contains(prod.id)
                }
                val isPostFocused = prod.hasVideo && (focusedProductId == prod.id)

                ElementoPublicacionFeedInicio(
                    product = prod,
                    index = index,
                    stats = publicationStats[prod.id],
                    isLiked = isLiked,
                    isSaved = isSaved,
                    isFocused = isPostFocused,
                    onToggleSaveProduct = onToggleSaveProduct,
                    onToggleLocalSave = onToggleLocalSave,
                    onToggleLike = onToggleLike,
                    onOpenComments = onOpenCommentsForProduct,
                    onRecordShare = onRecordShare,
                    onNavigateToProduct = onNavigateToProduct,
                    modifier = feedItemModifier
                )
            }

            item(key = "concierge_vip_card") {
                ConciergeVipCard(
                    onNavigateToChats = onNavigateToChats,
                    modifier = feedItemModifier
                )
            }

            item(key = "value_props_section") {
                InicioValuePropsSection(modifier = feedItemModifier)
            }
        }
    }
}
