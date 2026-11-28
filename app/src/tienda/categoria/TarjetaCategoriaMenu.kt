package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.media.ConfiguracionCacheImagenesCoil
import com.example.data.model.CategoryItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
internal fun TarjetaCategoriaMenu(
    catItem: CategoryItem,
    productCount: Int,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (isCompact) 4.dp else 8.dp,
                vertical = if (isCompact) 12.dp else 14.dp
            )
            .testTag("menu_item_${catItem.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 42.dp else 48.dp)
                .clip(CircleShape)
                .background(Color(0xFFF6EFE9)),
            contentAlignment = Alignment.Center
        ) {
            val context = LocalContext.current
            val categoryThumbnailRequest = remember(catItem.imageUrl, context) {
                catItem.imageUrl.takeIf { it.isNotBlank() }?.let { url ->
                    ConfiguracionCacheImagenesCoil.peticionMiniaturaCompacta(context, url)
                }
            }
            if (categoryThumbnailRequest != null) {
                AsyncImage(
                    model = categoryThumbnailRequest,
                    contentDescription = catItem.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(if (isCompact) 42.dp else 48.dp)
                )
            } else {
                Icon(
                    imageVector = getCategoryStoryIcon(catItem),
                    contentDescription = catItem.title,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(if (isCompact) 21.dp else 24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(if (isCompact) 12.dp else 16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = catItem.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontSize = if (isCompact) 15.sp else 16.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF2EBE5)
                ) {
                    Text(
                        text = "$productCount",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundyDark,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (catItem.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = catItem.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray,
                        fontSize = if (isCompact) 11.5.sp else 12.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Ir a categoría",
            tint = EleganGold,
            modifier = Modifier.size(20.dp)
        )
    }
}
