package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Product

@Composable
fun ProductPublicationPost(
    product: Product,
    index: Int,
    isLiked: Boolean,
    isSaved: Boolean,
    likeCount: Int,
    commentCount: Int,
    shareCount: Int = 0,
    saveCount: Int = 0,
    isFocused: Boolean = false,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit,
    onProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ratio = remember(product.mediaFormat, index) {
        when (product.mediaFormat.trim()) {
            "3:4" -> (3f / 4f)
            "4:3" -> (4f / 3f)
            "1:1" -> 1f
            else -> when (index % 3) {
                0 -> 1f
                1 -> (3f / 4f)
                else -> (4f / 3f)
            }
        }
    }

    val mediaItems = remember(
        product.id,
        product.imageUrl,
        product.images,
        product.videoUrl,
        product.isVideo
    ) {
        getProductMediaItems(product)
    }
    val priceText = remember(product.price) {
        "$${String.format("%.2f", product.price)}"
    }
    val categoryLabel = remember(product.category) {
        "Boutique Oficial • ${product.category.replaceFirstChar { it.uppercase() }}"
    }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .testTag("publication_post_${product.id}")
    ) {
        CabeceraPublicacionProducto(
            productName = product.name,
            categoryLabel = categoryLabel,
            onProductClick = onProductClick
        )

        CarruselMultimediaPublicacion(
            product = product,
            mediaItems = mediaItems,
            pagerState = pagerState,
            ratio = ratio,
            priceText = priceText,
            isFocused = isFocused,
            onProductClick = onProductClick
        )

        BarraInteraccionPublicacion(
            productId = product.id,
            isLiked = isLiked,
            isSaved = isSaved,
            likeCount = likeCount,
            commentCount = commentCount,
            saveCount = saveCount,
            shareCount = shareCount,
            onLikeClick = onLikeClick,
            onCommentClick = onCommentClick,
            onSaveClick = onSaveClick,
            onShareClick = onShareClick
        )

        DetallesYAccionesPublicacion(
            productId = product.id,
            description = product.description,
            commentCount = commentCount,
            onCommentClick = onCommentClick,
            onProductClick = onProductClick
        )

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Color(0xFFEBE6DF))
        )
    }
}

@Composable
fun ProductHomeCard(
    product: Product,
    onClick: () -> Unit
) {
    ProductPublicationPost(
        product = product,
        index = 0,
        isLiked = false,
        isSaved = false,
        likeCount = product.reviewCount.toInt().coerceAtLeast(10),
        commentCount = 2,
        onLikeClick = {},
        onCommentClick = {},
        onSaveClick = {},
        onShareClick = {},
        onProductClick = onClick
    )
}
