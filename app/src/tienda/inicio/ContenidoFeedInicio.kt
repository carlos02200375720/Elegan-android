package com.example.ui

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.R
import com.example.data.model.Announcement
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.PublicationStats
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.ui.components.BannerCatalogoSinConexion
import com.example.ui.components.ErrorCargaSiguientePagina
import com.example.ui.components.FinCatalogoPaginado
import com.example.ui.components.IndicadorCargaSiguientePagina
import com.example.ui.components.IndicadorSincronizacionEnSegundoPlano
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun ContenidoFeedInicio(
    listState: LazyListState,
    featuredProducts: List<Product>,
    pagedProducts: LazyPagingItems<Product>? = null,
    categories: List<CategoryItem> = emptyList(),
    announcements: List<Announcement> = emptyList(),
    savedProductIds: Set<String>,
    localSavedProductIds: Set<String>,
    publicationStats: Map<String, PublicationStats>,
    likedPublicationIds: Set<String>,
    focusedProductIdState: State<String?>,
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {},
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenCommentsForProduct: (Product) -> Unit,
    onShareProduct: (Product) -> Unit,
    onNavigateToCatalogo: (String?) -> Unit,
    onAnnouncementClick: (Announcement) -> Unit,
    onNavigateToProduct: (Product) -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToChats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brandLogoPainter = painterResource(id = R.drawable.icono)

    BoxWithConstraints(modifier = modifier) {
        val containerWidth = maxWidth
        val isExpanded = containerWidth >= 600.dp
        val feedItemWidth = if (isExpanded) minOf(containerWidth, 620.dp) else containerWidth
        val categoriesWidth = if (isExpanded) minOf(containerWidth, 840.dp) else containerWidth
        val isCompactPost = (feedItemWidth - 28.dp) < 330.dp
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
            contentPadding = PaddingValues(bottom = 32.dp),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ) {
            item(key = "hero_banner", contentType = "hero_banner") {
                InicioHeroBanner(
                    onNavigateToCatalogo = onNavigateToCatalogo,
                    onNavigateToMenu = onNavigateToMenu,
                    availableWidth = containerWidth
                )
            }

            if (categories.isNotEmpty()) {
                item(key = "categories_section", contentType = "categories_section") {
                    CategoryStoriesSection(
                        categories = categories,
                        onNavigateToCatalogo = onNavigateToCatalogo,
                        onNavigateToMenu = onNavigateToMenu,
                        products = featuredProducts,
                        modifier = categoriesModifier,
                        availableWidth = categoriesWidth
                    )
                }
            }

            if (announcements.isNotEmpty()) {
                item(key = "announcements_section", contentType = "announcements_section") {
                    SeccionAnunciosInicio(
                        announcements = announcements,
                        onAnnouncementClick = onAnnouncementClick,
                        modifier = categoriesModifier
                    )
                }
            }

            item(key = "feed_spacer", contentType = "feed_spacer") {
                Spacer(modifier = Modifier.height(if (containerWidth < 360.dp) 12.dp else 16.dp))
            }

            val refreshState = pagedProducts?.loadState?.refresh
            val effectiveIsEmpty = if (pagedProducts != null) {
                pagedProducts.itemCount == 0
            } else {
                featuredProducts.isEmpty()
            }
            val effectiveIsLoading = if (pagedProducts != null) {
                refreshState is LoadState.Loading
            } else {
                isLoadingProducts
            }
            val effectiveError = if (pagedProducts != null) {
                (refreshState as? LoadState.Error)?.let { err ->
                    err.error.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "No se pudo cargar el catálogo desde el servidor."
                }
            } else {
                productsError
            }

            if (effectiveIsEmpty && (effectiveIsLoading || !effectiveError.isNullOrBlank())) {
                item(key = "feed_backend_status", contentType = "feed_backend_status") {
                    Column(
                        modifier = feedItemModifier.padding(horizontal = 24.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (effectiveIsLoading) {
                            CircularProgressIndicator(color = EleganBurgundy)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Cargando colección desde el servidor...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                        } else if (!effectiveError.isNullOrBlank()) {
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
                                text = effectiveError,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    pagedProducts?.retry()
                                    onRefreshProducts()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }

            if (!effectiveIsEmpty && effectiveIsLoading) {
                item(key = "feed_background_sync", contentType = "paging_status") {
                    IndicadorSincronizacionEnSegundoPlano(modifier = feedItemModifier)
                }
            } else if (!effectiveIsEmpty && !effectiveError.isNullOrBlank()) {
                item(key = "feed_offline_banner", contentType = "paging_status") {
                    BannerCatalogoSinConexion(
                        onRetry = {
                            pagedProducts?.refresh()
                            onRefreshProducts()
                        },
                        modifier = feedItemModifier
                    )
                }
            }

            if (pagedProducts != null) {
                items(
                    count = pagedProducts.itemCount,
                    key = pagedProducts.itemKey { prod -> prod.productId() },
                    contentType = pagedProducts.itemContentType { "product_post" }
                ) { index ->
                    val prod = pagedProducts[index] ?: return@items
                    val prodId = prod.productId()
                    val isLiked = prodId in likedPublicationIds
                    val isSaved = if (onToggleSaveProduct != null) {
                        savedProductIds.contains(prodId)
                    } else {
                        localSavedProductIds.contains(prodId)
                    }
                    val hasVideo = prod.productHasVideo()

                    ElementoPublicacionFeedInicio(
                        product = prod,
                        index = index,
                        stats = publicationStats[prodId],
                        isLiked = isLiked,
                        isSaved = isSaved,
                        isFocusedProvider = { hasVideo && focusedProductIdState.value == prodId },
                        onToggleSaveProduct = onToggleSaveProduct,
                        onToggleLocalSave = onToggleLocalSave,
                        onToggleLike = onToggleLike,
                        onOpenComments = onOpenCommentsForProduct,
                        onShareProduct = onShareProduct,
                        onNavigateToProduct = onNavigateToProduct,
                        isCompact = isCompactPost,
                        brandLogoPainter = brandLogoPainter,
                        modifier = feedItemModifier,
                        categories = categories
                    )
                }

                when (val appendState = pagedProducts.loadState.append) {
                    is LoadState.Loading -> {
                        item(key = "feed_paging_append_loading", contentType = "paging_status") {
                            IndicadorCargaSiguientePagina(modifier = feedItemModifier)
                        }
                    }
                    is LoadState.Error -> {
                        item(key = "feed_paging_append_error", contentType = "paging_status") {
                            ErrorCargaSiguientePagina(
                                message = appendState.error.localizedMessage,
                                onRetry = { pagedProducts.retry() },
                                modifier = feedItemModifier
                            )
                        }
                    }
                    is LoadState.NotLoading -> {
                        if (appendState.endOfPaginationReached && pagedProducts.itemCount > 0) {
                            item(key = "feed_paging_end", contentType = "paging_status") {
                                FinCatalogoPaginado(modifier = feedItemModifier)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = featuredProducts,
                    key = { index, prod -> prod.productId().ifBlank { "product_$index" } },
                    contentType = { _, _ -> "product_post" }
                ) { index, prod ->
                    val prodId = prod.productId()
                    val isLiked = prodId in likedPublicationIds
                    val isSaved = if (onToggleSaveProduct != null) {
                        savedProductIds.contains(prodId)
                    } else {
                        localSavedProductIds.contains(prodId)
                    }
                    val hasVideo = prod.productHasVideo()

                    ElementoPublicacionFeedInicio(
                        product = prod,
                        index = index,
                        stats = publicationStats[prodId],
                        isLiked = isLiked,
                        isSaved = isSaved,
                        isFocusedProvider = { hasVideo && focusedProductIdState.value == prodId },
                        onToggleSaveProduct = onToggleSaveProduct,
                        onToggleLocalSave = onToggleLocalSave,
                        onToggleLike = onToggleLike,
                        onOpenComments = onOpenCommentsForProduct,
                        onShareProduct = onShareProduct,
                        onNavigateToProduct = onNavigateToProduct,
                        isCompact = isCompactPost,
                        brandLogoPainter = brandLogoPainter,
                        modifier = feedItemModifier,
                        categories = categories
                    )
                }
            }

            item(key = "concierge_vip_card", contentType = "concierge_vip_card") {
                ConciergeVipCard(
                    onNavigateToChats = onNavigateToChats,
                    modifier = feedItemModifier,
                    availableWidth = feedItemWidth
                )
            }

            item(key = "value_props_section", contentType = "value_props_section") {
                InicioValuePropsSection(
                    modifier = feedItemModifier,
                    availableWidth = feedItemWidth
                )
            }
        }
    }
}
