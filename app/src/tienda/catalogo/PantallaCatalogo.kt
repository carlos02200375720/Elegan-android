package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductJson
import com.example.data.model.productCategory
import com.example.data.model.productDescription
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.ui.components.LocalEleganNotifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    products: List<ProductJson>,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    onNavigateToMenu: () -> Unit,
    onProductClick: (ProductJson) -> Unit,
    onQuickAddToCart: (ProductJson) -> Unit = {},
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    savedProductIds: Set<String> = emptySet(),
    onToggleSaveProduct: (String) -> Unit = {},
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current
    val categoryListState = rememberLazyListState()

    val filteredProducts = remember(products, selectedCategory, searchQuery) {
        products.filter { prod ->
            val category = prod.productCategory()
            val matchesCat = selectedCategory == "todo" || category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                prod.productName().contains(searchQuery, ignoreCase = true) ||
                prod.productDescription().contains(searchQuery, ignoreCase = true) ||
                category.contains(searchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CabeceraBusquedaCatalogo(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                cartItemCount = cartItemCount,
                onNavigateToMenu = onNavigateToMenu,
                onNavigateToCart = onNavigateToCart
            )
        },
        containerColor = Color.White
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isMedium = maxWidth >= 600.dp && maxWidth < 840.dp
            val isExpanded = maxWidth >= 840.dp

            val columnsCount = when {
                isExpanded -> 4
                isMedium -> 3
                else -> 2
            }
            val horizontalPad = when {
                isCompact -> 10.dp
                isMedium || isExpanded -> 20.dp
                else -> 16.dp
            }
            val columnGap = if (isCompact) 8.dp else 12.dp
            val rowGap = if (isCompact) 10.dp else 14.dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1120.dp)
                    .testTag("catalogo_screen"),
                contentPadding = PaddingValues(
                    start = horizontalPad,
                    end = horizontalPad,
                    bottom = if (isCompact) 12.dp else 16.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(columnGap),
                verticalArrangement = Arrangement.spacedBy(rowGap)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CatalogoCategoryStoriesSection(
                        selectedCategory = selectedCategory,
                        categoryListState = categoryListState,
                        onCategoryChange = onCategoryChange,
                        products = products
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredProducts.size} prendas encontradas",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium,
                                fontSize = if (isCompact) 11.5.sp else 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (filteredProducts.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EstadoVacioCatalogo(
                            isCompact = isCompact,
                            isLoading = isLoadingProducts && products.isEmpty(),
                            errorMessage = if (products.isEmpty()) productsError else null,
                            hasActiveFilters = searchQuery.isNotBlank() || selectedCategory != "todo",
                            onRetry = onRefreshProducts,
                            onResetFilters = {
                                searchQuery = ""
                                onCategoryChange("todo")
                            }
                        )
                    }
                }

                items(filteredProducts, key = { it.productId() }) { product ->
                    val prodId = product.productId()
                    val isSaved = savedProductIds.contains(prodId)
                    ProductGridCard(
                        product = product,
                        isSaved = isSaved,
                        onClick = { onProductClick(product) },
                        onToggleSave = {
                            val willSave = !isSaved
                            onToggleSaveProduct(prodId)
                            val msg = if (willSave) "Guardado en tu colección" else "Eliminado de guardados"
                            if (notifier != null) {
                                notifier.show(msg)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        }
                    )
                }
            }
        }
    }
}
