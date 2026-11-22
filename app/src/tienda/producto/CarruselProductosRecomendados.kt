package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryItem
import com.example.data.model.ProductJson
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productId
import com.example.ui.theme.EleganCharcoal

@Composable
fun CarruselProductosRecomendados(
    recommendedProducts: List<ProductJson>,
    categories: List<CategoryItem>,
    savedProductIds: Set<String>,
    onProductClick: (ProductJson) -> Unit,
    onToggleSaveProduct: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (recommendedProducts.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("recommended_products_section")
    ) {
        HorizontalDivider(
            color = Color(0xFFE5DDD5),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.producto_recommended_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            ),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("recommended_products_carousel")
        ) {
            items(
                items = recommendedProducts,
                key = { it.productId() }
            ) { item ->
                val prodId = item.productId()
                val isSaved = savedProductIds.contains(prodId)
                ProductGridCard(
                    product = item,
                    isSaved = isSaved,
                    onClick = { onProductClick(item) },
                    onToggleSave = { onToggleSaveProduct(prodId, !isSaved) },
                    categoryLabel = item.productCategoryDisplayName(categories),
                    modifier = Modifier.width(168.dp),
                    cardTestTag = "recommended_item_$prodId",
                    saveTestTag = "recommended_save_$prodId"
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
