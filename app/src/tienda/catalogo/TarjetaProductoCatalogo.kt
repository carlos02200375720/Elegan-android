package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ProductJson
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun ProductGridCard(
    product: ProductJson,
    isSaved: Boolean,
    onClick: () -> Unit,
    onToggleSave: () -> Unit,
    categoryLabel: String = product.productCategoryDisplayName()
) {
    val productId = product.productId()
    val productName = product.productName()
    val productImageUrl = product.productImageUrl()
    val productPrice = product.productPrice()

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("catalog_item_$productId"),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactCard = maxWidth < 165.dp
            val contentPadding = if (isCompactCard) 8.dp else 10.dp

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFFF3EFEA))
                ) {
                    AsyncImage(
                        model = productImageUrl,
                        contentDescription = productName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (categoryLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 8.dp),
                            color = EleganBurgundyDark,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .widthIn(max = this@BoxWithConstraints.maxWidth * 0.78f)
                        ) {
                            Text(
                                text = categoryLabel.uppercase(),
                                color = Color.White,
                                fontSize = if (isCompactCard) 7.5.sp else 8.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactCard) 5.dp else 6.dp,
                                    vertical = 2.dp
                                )
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(contentPadding)) {
                    Text(
                        text = productName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EleganCharcoal,
                            fontSize = if (isCompactCard) 13.sp else 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(if (isCompactCard) 2.dp else 3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = formatearPrecioRd(productPrice),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundy,
                                fontSize = if (isCompactCard) 13.sp else 14.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        IconButton(
                            onClick = onToggleSave,
                            modifier = Modifier
                                .size(if (isCompactCard) 28.dp else 32.dp)
                                .testTag("catalog_save_$productId")
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isSaved) "Guardado en colección" else "Guardar en colección guardado",
                                tint = if (isSaved) EleganGold else Color(0xFF8E8E93),
                                modifier = Modifier.size(if (isCompactCard) 20.dp else 22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
