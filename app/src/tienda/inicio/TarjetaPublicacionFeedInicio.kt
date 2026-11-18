package com.example.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.ProductJson
import com.example.data.model.PublicationStats
import com.example.data.model.productId
import com.example.data.model.productCommentCount
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.ui.components.LocalEleganNotifier

@Composable
internal fun ElementoPublicacionFeedInicio(
    product: ProductJson,
    index: Int,
    stats: PublicationStats?,
    isLiked: Boolean,
    isSaved: Boolean,
    isFocused: Boolean,
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenComments: (ProductJson) -> Unit,
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToProduct: (ProductJson) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifier = LocalEleganNotifier.current

    val currentLikes = stats?.likesCount?.toInt() ?: 0
    val currentCommentCount = maxOf(
        stats?.commentsCount ?: 0L,
        product.productCommentCount()
    ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    val currentShareCount = stats?.sharesCount?.toInt() ?: 0
    val currentSaveCount = stats?.savesCount?.toInt() ?: 0

    val productId = product.productId()
    val productName = product.productName()
    val productPrice = product.productPrice()

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
        notifier
    ) {
        {
            val willSave = !isSaved
            if (onToggleSaveProduct != null) {
                onToggleSaveProduct(productId) { result ->
                    notifier?.show(
                        if (result.isSuccess) {
                            if (willSave) "Publicación guardada en tu perfil"
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
    val onShareAction = remember(product, onRecordShare, notifier) {
        {
            onRecordShare(productId) { result ->
                if (result.isSuccess) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "ELEGAN: Descubre $productName por $${String.format("%.2f", productPrice)} USD. Pieza exclusiva de la boutique."
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(sendIntent, "Compartir publicación")
                    )
                } else {
                    notifier?.show("No se pudo registrar el compartido")
                }
            }
        }
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
        isFocused = isFocused,
        onLikeClick = onLikeAction,
        onCommentClick = onCommentAction,
        onSaveClick = onSaveAction,
        onShareClick = onShareAction,
        onProductClick = onProductAction,
        modifier = modifier
    )
}
