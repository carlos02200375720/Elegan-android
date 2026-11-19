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
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productDescription
import com.example.data.model.productHasVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.model.productMediaFormat
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productReviewCount
import com.example.data.model.productVideoUrl

@Composable
fun ProductPublicationPost(
    product: ProductJson,
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
    modifier: Modifier = Modifier,
    categories: List<CategoryItem> = emptyList()
) {
    val productId = product.productId()
    val mediaFormat = product.productMediaFormat()
    val productImageUrl = product.productImageUrl()
    val productImages = product.productImages()
    val productVideoUrl = product.productVideoUrl()
    val productHasVideo = product.productHasVideo()
    val productPrice = product.productPrice()
    val productCategoryName = remember(product, categories) {
        product.productCategoryDisplayName(categories)
    }
    val productName = product.productName()
    val productDescription = product.productDescription()

    val ratio = remember(mediaFormat, index) {
        when (mediaFormat.trim()) {
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
        productId,
        productImageUrl,
        productImages,
        productVideoUrl,
        productHasVideo
    ) {
        getProductMediaItems(product)
    }
    val priceText = remember(productPrice) {
        "$${String.format("%.2f", productPrice)}"
    }
    val categoryLabel = remember(productCategoryName) {
        if (productCategoryName.isNotBlank()) {
            "Boutique Oficial • ${productCategoryName.replaceFirstChar { it.uppercase() }}"
        } else {
            "Boutique Oficial"
        }
    }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .testTag("publication_post_$productId")
    ) {
        CabeceraPublicacionProducto(
            productName = productName,
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
            productId = productId,
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
            productId = productId,
            description = productDescription,
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
    product: ProductJson,
    onClick: () -> Unit
) {
    ProductPublicationPost(
        product = product,
        index = 0,
        isLiked = false,
        isSaved = false,
        likeCount = product.productReviewCount().toInt().coerceAtLeast(10),
        commentCount = 2,
        onLikeClick = {},
        onCommentClick = {},
        onSaveClick = {},
        onShareClick = {},
        onProductClick = onClick
    )
}
