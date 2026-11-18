package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganLikeRed

@Composable
fun BarraInteraccionPublicacion(
    productId: String,
    isLiked: Boolean,
    isSaved: Boolean,
    likeCount: Int,
    commentCount: Int,
    saveCount: Int,
    shareCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onSaveClick: () -> Unit,
    onShareClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BotonMetricaInteraccion(
            icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = "Me gusta",
            tint = if (isLiked) EleganLikeRed else EleganCharcoal,
            iconSize = 22.dp,
            count = likeCount,
            tag = "post_like_$productId",
            onClick = onLikeClick,
            modifier = Modifier.weight(1f)
        )
        BotonMetricaInteraccion(
            icon = Icons.Outlined.ChatBubbleOutline,
            contentDescription = "Comentario",
            tint = EleganCharcoal,
            iconSize = 21.dp,
            count = commentCount,
            tag = "post_comment_$productId",
            onClick = onCommentClick,
            modifier = Modifier.weight(1f)
        )
        BotonMetricaInteraccion(
            icon = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = "Guardar",
            tint = if (isSaved) EleganGold else EleganCharcoal,
            iconSize = 22.dp,
            count = saveCount,
            tag = "post_save_$productId",
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        )
        BotonMetricaInteraccion(
            icon = Icons.Outlined.Share,
            contentDescription = "Compartir",
            tint = EleganCharcoal,
            iconSize = 21.dp,
            count = shareCount,
            tag = "post_share_$productId",
            onClick = onShareClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BotonMetricaInteraccion(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    iconSize: Dp,
    count: Int,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .testTag(tag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium.copy(
                color = EleganCharcoal,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}
