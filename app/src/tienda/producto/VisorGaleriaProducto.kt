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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.imageLoader
import com.example.data.model.Product
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganGold
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun VisorGaleriaProducto(
    product: Product,
    mediaItems: List<GalleryMediaItem>,
    pagerState: PagerState,
    categoryLabel: String = product.productCategoryDisplayName()
) {
    val productId = product.productId()
    val productName = product.productName()
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    val prefetchedSlides = remember(productId, mediaItems) { HashSet<String>(mediaItems.size) }

    LaunchedEffect(pagerState, mediaItems, imageLoader) {
        if (mediaItems.size <= 1) return@LaunchedEffect
        snapshotFlow { pagerState.currentPage }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color(0xFFF0EBE3))
    ) {
        if (mediaItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Checkroom,
                    contentDescription = productName,
                    tint = Color(0xFFB5ABA0),
                    modifier = Modifier.size(64.dp)
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                key = { page -> "${productId}_gallery_$page" },
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (val item = mediaItems.getOrNull(page)) {
                    is GalleryMediaItem.Video -> {
                        val isVideoFocused = (pagerState.currentPage == page)
                        EleganVideoPlayer(
                            videoUrl = item.videoUrl,
                            posterUrl = item.posterUrl,
                            aspectRatio = 1f,
                            autoPlay = false,
                            isFocused = isVideoFocused,
                            showControls = true,
                            testTagPrefix = "gallery_video_$productId",
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    is GalleryMediaItem.Image -> {
                        ImagenPublicacionProducto(
                            imageUrl = item.imageUrl,
                            contentDescription = "$productName - Imagen ${page + 1}"
                        )
                    }

                    null -> {}
                }
            }
        }

        if (mediaItems.isNotEmpty()) {
            val currentItem = mediaItems.getOrNull(pagerState.currentPage)
            val isCurrentVideo = currentItem is GalleryMediaItem.Video

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 10.dp, bottom = 10.dp)
                    .testTag("gallery_content_indicator")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 5.5.dp, vertical = 2.5.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentVideo) Icons.Default.Videocam else Icons.Default.Collections,
                        contentDescription = "Contenido multimedia",
                        tint = if (isCurrentVideo) EleganGold else Color.White.copy(alpha = 0.95f),
                        modifier = Modifier.size(9.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isCurrentVideo) {
                            "Video • ${pagerState.currentPage + 1}/${mediaItems.size}"
                        } else {
                            "${pagerState.currentPage + 1}/${mediaItems.size}"
                        },
                        color = Color.White,
                        fontSize = 8.sp,
                        lineHeight = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.25.sp,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }
            }
        }

        if (categoryLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = EleganBurgundyDark.copy(alpha = 0.9f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 10.dp, bottom = 10.dp)
                    .testTag("gallery_category_badge")
            ) {
                Text(
                    text = categoryLabel.uppercase(),
                    color = Color.White,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.5.dp)
                )
            }
        }
    }
}
