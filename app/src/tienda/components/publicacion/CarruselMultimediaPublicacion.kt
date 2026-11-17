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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganGold

@Composable
fun CarruselMultimediaPublicacion(
    product: Product,
    mediaItems: List<GalleryMediaItem>,
    pagerState: PagerState,
    ratio: Float,
    priceText: String,
    isFocused: Boolean,
    onProductClick: () -> Unit
) {
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
                contentDescription = product.name,
                tint = Color(0xFFB5ABA0),
                modifier = Modifier.size(56.dp)
            )
        } else if (mediaItems.size == 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("post_media_pager_${product.id}")
            ) {
                ElementoMultimediaPublicacion(
                    product = product,
                    item = mediaItems.firstOrNull(),
                    page = 0,
                    ratio = ratio,
                    isVideoFocused = isFocused,
                    onProductClick = onProductClick
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                key = { page -> "${product.id}_$page" },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("post_media_pager_${product.id}")
            ) { page ->
                ElementoMultimediaPublicacion(
                    product = product,
                    item = mediaItems.getOrNull(page),
                    page = page,
                    ratio = ratio,
                    isVideoFocused = isFocused && (pagerState.currentPage == page),
                    onProductClick = onProductClick
                )
            }
        }

        if (mediaItems.isNotEmpty()) {
            val currentItem = mediaItems.getOrNull(pagerState.currentPage)
            val isCurrentVideo = currentItem is GalleryMediaItem.Video

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 9.dp, vertical = 4.dp)
                    .testTag("post_content_indicator_${product.id}")
            ) {
                Icon(
                    imageVector = if (isCurrentVideo) Icons.Default.Videocam else Icons.Default.Collections,
                    contentDescription = "Tipo de contenido",
                    tint = if (isCurrentVideo) EleganGold else Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${pagerState.currentPage + 1}/${mediaItems.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.84f))
                .testTag("post_price_${product.id}")
        ) {
            Text(
                text = priceText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = EleganBurgundy
                ),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}
