package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductJson
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.ui.GalleryMediaItem
import com.example.ui.getProductMediaItems
import com.example.ui.theme.EleganGold

/**
 * Item individual de la cuadrícula de 3 columnas.
 * Muestra la portada de la publicación, indicador de video o galería, precio y permite abrirla al toque.
 */
@Composable
fun SavedPublicationGridItem(
    product: ProductJson,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val productId = product.productId()
    val productName = product.productName()
    val productImageUrl = product.productImageUrl()
    val productPrice = product.productPrice()
    val mediaItems = getProductMediaItems(product)
    val hasVideo = product.productHasVideo() || mediaItems.any { it is GalleryMediaItem.Video }
    val isMultiMedia = mediaItems.size > 1

    val previewUrl = when (val first = mediaItems.firstOrNull()) {
        is GalleryMediaItem.Video -> first.posterUrl.ifBlank { productImageUrl }
        is GalleryMediaItem.Image -> first.imageUrl
        null -> productImageUrl
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFEFEBE8))
            .clickable(onClick = onClick)
            .testTag("saved_post_$productId")
    ) {
        if (previewUrl.isNotBlank()) {
            AsyncImage(
                model = previewUrl,
                contentDescription = productName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.Checkroom,
                contentDescription = productName,
                tint = Color(0xFFB5ABA0),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(28.dp)
            )
        }

        // Degradado oscuro inferior para legibilidad del nombre y precio
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        // Distintivo superior derecho: video o multi-imagen
        if (hasVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Video",
                    tint = EleganGold,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else if (isMultiMedia) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Collections,
                    contentDescription = "Galería",
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Título breve y precio en la parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Text(
                text = productName,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$${productPrice.toInt()}",
                color = EleganGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
