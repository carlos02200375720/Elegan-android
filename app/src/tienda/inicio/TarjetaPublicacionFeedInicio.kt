package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.model.productCommentCount
import com.example.data.model.productId
import com.example.ui.components.LocalEleganNotifier

@Composable
internal fun ElementoPublicacionFeedInicio(
    product: ProductJson,
    index: Int,
    stats: PublicationStats?,
    isLiked: Boolean,
    isSaved: Boolean,
    isFocusedProvider: () -> Boolean,
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenComments: (ProductJson) -> Unit,
    onShareProduct: (ProductJson) -> Unit,
    onNavigateToProduct: (ProductJson) -> Unit,
    isCompact: Boolean,
    brandLogoPainter: Painter,
    modifier: Modifier = Modifier,
    categories: List<CategoryItem> = emptyList()
) {
    val notifier = LocalEleganNotifier.current

    val currentLikes = stats?.likesCount?.toInt() ?: 0
    val currentCommentCount = maxOf(
        stats?.commentsCount ?: 0L,
        product.productCommentCount()
    ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val currentShareCount = stats?.sharesCount?.toInt() ?: 0
    val currentSaveCount = stats?.savesCount?.toInt() ?: 0

    val productId = product.productId()

    val onLikeAction = remember(productId, onToggleLike, notifier) {
        {
            onToggleLike(productId) { result ->
                notifier?.show(
                    if (result.isSuccess) {
                        if (result.getOrNull() == true) "Te gusta esta publicación"
                        else "Quitaste tu like"
                    } else {
                        "No se pudo actualizar el like"
                    }
                )
            }
        }
    }
    val onCommentAction = remember(product, onOpenComments) {
        { onOpenComments(product) }
    }
    val onSaveAction = remember<() -> Unit>(
        productId,
        isSaved,
        onToggleSaveProduct,
        onToggleLocalSave,
        notifier
    ) {
        {
            val willSave = !isSaved
            if (onToggleSaveProduct != null) {
                onToggleSaveProduct(productId) { result ->
                    notifier?.show(
                        if (result.isSuccess) {
                            val confirmedSaved = result.getOrNull() ?: willSave
                            if (confirmedSaved) "Publicación guardada en tu perfil"
                            else "Publicación eliminada de guardados"
                        } else {
                            "No se pudo actualizar la publicación guardada"
                        }
                    )
                }
            } else {
                onToggleLocalSave(productId, willSave)
                notifier?.show(
                    if (willSave) "Publicación guardada en tu perfil"
                    else "Publicación eliminada de guardados"
                )
            }
        }
    }
    val onShareAction = remember(product, onShareProduct) {
        { onShareProduct(product) }
    }
    val onProductAction = remember(product, onNavigateToProduct) {
        { onNavigateToProduct(product) }
    }

    ProductPublicationPost(
        product = product,
        index = index,
        isLiked = isLiked,
        isSaved = isSaved,
        likeCount = currentLikes,
        commentCount = currentCommentCount,
        shareCount = currentShareCount,
        saveCount = currentSaveCount,
        onLikeClick = onLikeAction,
        onCommentClick = onCommentAction,
        onSaveClick = onSaveAction,
        onShareClick = onShareAction,
        onProductClick = onProductAction,
        modifier = modifier,
        categories = categories,
        isCompact = isCompact,
        brandLogoPainter = brandLogoPainter,
        isFocusedProvider = isFocusedProvider
    )
}
