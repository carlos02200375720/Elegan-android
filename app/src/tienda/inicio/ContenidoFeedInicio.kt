package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun ContenidoFeedInicio(
    listState: LazyListState,
    featuredProducts: List<ProductJson>,
    savedProductIds: Set<String>,
    localSavedProductIds: Set<String>,
    publicationStats: Map<String, PublicationStats>,
    likedPublicationIds: Set<String>,
    focusedProductId: String?,
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {},
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenCommentsForProduct: (ProductJson) -> Unit,
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCatalogo: (String) -> Unit,
    onNavigateToProduct: (ProductJson) -> Unit,
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
                    products = featuredProducts,
                    modifier = categoriesModifier
                )
            }

            item(key = "feed_spacer") {
                Spacer(modifier = Modifier.height(if (maxWidth < 360.dp) 12.dp else 16.dp))
            }

            if (featuredProducts.isEmpty() && (isLoadingProducts || !productsError.isNullOrBlank())) {
                item(key = "feed_backend_status") {
                    Column(
                        modifier = feedItemModifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingProducts) {
                            CircularProgressIndicator(color = EleganBurgundy)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cargando colección desde el servidor...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                        } else if (!productsError.isNullOrBlank()) {
                            Text(
                                text = "No se pudo cargar el catálogo",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                ),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = productsError,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = onRefreshProducts,
                                colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            itemsIndexed(
                items = featuredProducts,
                key = { _, prod -> prod.productId() },
                contentType = { _, _ -> "product_post" }
            ) { index, prod ->
                val prodId = prod.productId()
                val isLiked = prodId in likedPublicationIds
                val isSaved = if (onToggleSaveProduct != null) {
                    savedProductIds.contains(prodId)
                } else {
                    localSavedProductIds.contains(prodId)
                }
                val isPostFocused = prod.productHasVideo() && (focusedProductId == prodId)

                ElementoPublicacionFeedInicio(
                    product = prod,
                    index = index,
                    stats = publicationStats[prodId],
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
