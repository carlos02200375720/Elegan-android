package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun CatalogoCategoryStoriesSection(
    categories: List<CategoryItem>,
    selectedCategory: String?,
    categoryListState: LazyListState,
    onCategoryChange: (String?) -> Unit,
    products: List<Product> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (categories.isEmpty()) return
    val coverByCategoryId = remember(categories, products) {
        resolveCategoryCoverImages(categories, products)
    }
    LaunchedEffect(selectedCategory, categories) {
        val index = if (selectedCategory == null) -1 else categories.indexOfFirst { it.id == selectedCategory }
        if (index >= 0) categoryListState.animateScrollToItem(index)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
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
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontFamily = FontFamily.Serif
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (selectedCategory != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EleganBurgundy.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.35f)),
                    modifier = Modifier.clickable { onCategoryChange(null) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ver todo",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganBurgundy,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar filtro",
                            tint = EleganBurgundy,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val spacing = if (maxWidth < 350.dp) 6.dp else 8.dp
            val visibleCards = if (maxWidth >= 600.dp) 6f else 4f
            val cardWidth = ((maxWidth - (spacing * (visibleCards - 1f))) / visibleCards)
                .coerceIn(66.dp, 118.dp)

            LazyRow(
                state = categoryListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("catalogo_category_stories_row"),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val isSelected = selectedCategory == cat.id
                    CatalogoCategoryStoryCard(
                        category = cat,
                        cardWidth = cardWidth,
                        isSelected = isSelected,
                        imageUrl = coverByCategoryId[cat.id],
                        onClick = { onCategoryChange(cat.id) }
                    )
                }
            }
        }
    }
}
