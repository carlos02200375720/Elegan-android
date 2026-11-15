package com.example.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.Product
import com.example.data.model.PublicationStats
import com.example.ui.components.LocalEleganNotifier

@Composable
internal fun ElementoPublicacionFeedInicio(
    product: Product,
    index: Int,
    stats: PublicationStats?,
    isLiked: Boolean,
    isSaved: Boolean,
    isFocused: Boolean,
    onToggleSaveProduct: ((String, (Result<Boolean>) -> Unit) -> Unit)?,
    onToggleLocalSave: (String, Boolean) -> Unit,
    onToggleLike: (String, (Result<Boolean>) -> Unit) -> Unit,
    onOpenComments: (Product) -> Unit,
    onRecordShare: (String, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToProduct: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifier = LocalEleganNotifier.current

    val currentLikes = stats?.likesCount?.toInt() ?: 0
    val currentCommentCount = stats?.commentsCount?.toInt() ?: 0
    val currentShareCount = stats?.sharesCount?.toInt() ?: 0
    val currentSaveCount = stats?.savesCount?.toInt() ?: 0

    val onLikeAction = remember(product.id, onToggleLike, notifier) {
        {
            onToggleLike(product.id) { result ->
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
        product.id,
        isSaved,
        onToggleSaveProduct,
        notifier
    ) {
        {
            val willSave = !isSaved
            if (onToggleSaveProduct != null) {
                onToggleSaveProduct(product.id) { result ->
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
                onToggleLocalSave(product.id, willSave)
                notifier?.show(
                    if (willSave) "Publicación guardada en tu perfil"
                    else "Publicación eliminada de guardados"
                )
            }
        }
    }
    val onShareAction = remember(product, onRecordShare, notifier) {
        {
            onRecordShare(product.id) { result ->
                if (result.isSuccess) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "ELEGAN: Descubre ${product.name} por $${String.format("%.2f", product.price)} USD. Pieza exclusiva de la boutique."
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
