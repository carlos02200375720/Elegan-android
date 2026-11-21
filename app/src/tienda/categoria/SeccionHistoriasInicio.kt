package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun CategoryStoriesSection(
    categories: List<CategoryItem>,
    onNavigateToCatalogo: (String?) -> Unit,
    onNavigateToMenu: () -> Unit,
    products: List<ProductJson> = emptyList(),
    modifier: Modifier = Modifier,
    availableWidth: Dp? = null
) {
    if (categories.isEmpty()) return
    val coverByCategoryId = remember(categories, products) {
        categories.associate { cat -> cat.id to resolveCategoryCoverImage(cat, products) }
    }

    if (availableWidth != null) {
        CategoryStoriesSectionContent(
            maxWidth = availableWidth,
            categories = categories,
            coverByCategoryId = coverByCategoryId,
            onNavigateToCatalogo = onNavigateToCatalogo,
            onNavigateToMenu = onNavigateToMenu,
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        )
    } else {
        BoxWithConstraints(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
        ) {
            CategoryStoriesSectionContent(
                maxWidth = maxWidth,
                categories = categories,
                coverByCategoryId = coverByCategoryId,
                onNavigateToCatalogo = onNavigateToCatalogo,
                onNavigateToMenu = onNavigateToMenu,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CategoryStoriesSectionContent(
    maxWidth: Dp,
    categories: List<CategoryItem>,
    coverByCategoryId: Map<String, String?>,
    onNavigateToCatalogo: (String?) -> Unit,
    onNavigateToMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCompact = maxWidth < 350.dp
    val isExpanded = maxWidth >= 600.dp
    val headerHorizontalPad = if (isCompact) 12.dp else 16.dp
    val horizontalPadding = if (isCompact) 10.dp else 12.dp
    val spacing = if (isCompact) 6.dp else 8.dp
    val visibleCards = if (isExpanded) 6f else 4f
    val cardWidth = ((maxWidth - (horizontalPadding * 2) - (spacing * (visibleCards - 1f))) / visibleCards)
        .coerceIn(66.dp, 118.dp)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = headerHorizontalPad),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EleganBurgundy)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Categorías",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = if (isCompact) 15.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontFamily = FontFamily.Serif
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Ver menú completo",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    color = EleganBurgundy,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onNavigateToMenu() }
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .testTag("category_stories_row"),
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ) {
            items(
                items = categories,
                key = { it.id }
            ) { cat ->
                CategoryStoryCard(
                    category = cat,
                    cardWidth = cardWidth,
                    imageUrl = coverByCategoryId[cat.id],
                    onClick = { onNavigateToCatalogo(cat.id) }
                )
            }
        }
    }
}
