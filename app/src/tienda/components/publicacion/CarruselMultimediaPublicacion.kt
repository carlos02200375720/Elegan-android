package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.example.data.model.Product
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganGold
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun CarruselMultimediaPublicacion(
    product: Product,
    mediaItems: List<GalleryMediaItem>,
    ratio: Float,
    priceText: String,
    isFocusedProvider: () -> Boolean,
    onProductClick: () -> Unit,
    pagerState: PagerState? = null
) {
    val productId = product.productId()
    val productName = product.productName()
    val badgeShape = remember { RoundedCornerShape(7.dp) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RectangleShape)
            .background(Color(0xFFF0EBE3)),
        contentAlignment = Alignment.Center
    ) {
        if (mediaItems.isEmpty()) {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = productName,
                tint = Color(0xFFB5ABA0),
                modifier = Modifier.size(56.dp)
            )
        } else if (mediaItems.size == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("post_media_pager_$productId")
            ) {
                ElementoMultimediaPublicacion(
                    product = product,
                    item = mediaItems.firstOrNull(),
                    page = 0,
                    ratio = ratio,
                    isVideoFocusedProvider = isFocusedProvider,
                    onProductClick = onProductClick
                )
            }

            IndicadorContenidoPublicacion(
                productId = productId,
                isVideo = mediaItems.firstOrNull() is GalleryMediaItem.Video,
                pageLabel = "1/1",
                shape = badgeShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        } else {
            val activePagerState = pagerState ?: rememberPagerState(pageCount = { mediaItems.size })
            val context = LocalContext.current
            val imageLoader = remember(context) { context.imageLoader }
            val prefetchedSlides = remember(productId, mediaItems) { HashSet<String>(mediaItems.size) }

            LaunchedEffect(activePagerState, mediaItems, imageLoader) {
                snapshotFlow { activePagerState.currentPage }
                    .distinctUntilChanged()
                    .collectLatest { currentPage ->
                        listOf(currentPage + 1, currentPage + 2, currentPage - 1).forEach { targetIndex ->
                            val url = when (val slide = mediaItems.getOrNull(targetIndex)) {
                                is GalleryMediaItem.Image -> slide.imageUrl
                                is GalleryMediaItem.Video -> slide.posterUrl
                                null -> ""
                            }
                            if (url.isNotBlank() && prefetchedSlides.add(url)) {
                                imageLoader.enqueue(
                                    construirPeticionImagenPublicacion(context, url)
                                )
                            }
                        }
                    }
            }

            HorizontalPager(
                state = activePagerState,
                beyondViewportPageCount = 1,
                key = { page -> "${productId}_$page" },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("post_media_pager_$productId")
            ) { page ->
                ElementoMultimediaPublicacion(
                    product = product,
                    item = mediaItems.getOrNull(page),
                    page = page,
                    ratio = ratio,
                    isVideoFocusedProvider = { isFocusedProvider() && (activePagerState.currentPage == page) },
                    onProductClick = onProductClick
                )
            }

            IndicadorContenidoPagerPublicacion(
                productId = productId,
                mediaItems = mediaItems,
                pagerState = activePagerState,
                shape = badgeShape,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .clip(badgeShape)
                .background(Color.White.copy(alpha = 0.84f))
                .testTag("post_price_$productId")
        ) {
            Text(
                text = priceText,
                maxLines = 1,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EleganBurgundy
                ),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp)
            )
        }
    }
}

@Composable
private fun IndicadorContenidoPagerPublicacion(
    productId: String,
    mediaItems: List<GalleryMediaItem>,
    pagerState: PagerState,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    val currentPage by remember(pagerState) {
        derivedStateOf { pagerState.currentPage }
    }
    val isCurrentVideo = mediaItems.getOrNull(currentPage) is GalleryMediaItem.Video
    IndicadorContenidoPublicacion(
        productId = productId,
        isVideo = isCurrentVideo,
        pageLabel = "${currentPage + 1}/${mediaItems.size}",
        shape = shape,
        modifier = modifier
    )
}

@Composable
private fun IndicadorContenidoPublicacion(
    productId: String,
    isVideo: Boolean,
    pageLabel: String,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(shape)
            .background(Color.Black.copy(alpha = 0.65f))
            .padding(horizontal = 4.5.dp, vertical = 2.dp)
            .testTag("post_content_indicator_$productId")
    ) {
        Icon(
            imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Collections,
            contentDescription = "Tipo de contenido",
            tint = if (isVideo) EleganGold else Color.White.copy(alpha = 0.95f),
            modifier = Modifier.size(7.dp)
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = pageLabel,
            maxLines = 1,
            color = Color.White,
            fontSize = 7.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.25.sp
        )
    }
}
