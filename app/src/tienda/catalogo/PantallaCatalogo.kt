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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.productCategory
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productDescription
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.ui.components.BannerCatalogoSinConexion
import com.example.ui.components.ErrorCargaSiguientePagina
import com.example.ui.components.FinCatalogoPaginado
import com.example.ui.components.IndicadorCargaSiguientePagina
import com.example.ui.components.IndicadorSincronizacionEnSegundoPlano
import com.example.ui.components.LocalEleganNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun filtrarProductosCatalogo(
    products: List<Product>,
    categoryTitleById: Map<String, String>,
    selectedCategory: String?,
    query: String
): List<Product> {
    val cleanQuery = query.trim()
    if (selectedCategory == null && cleanQuery.isEmpty()) return products
    return products.filter { prod ->
        val categoryId = prod.productCategory()
        val matchesCat = selectedCategory == null || categoryId == selectedCategory
        if (!matchesCat) return@filter false
        if (cleanQuery.isEmpty()) return@filter true
        val categoryTitle = prod.productCategoryDisplayName(categoryTitleById)
        prod.productName().contains(cleanQuery, ignoreCase = true) ||
            prod.productDescription().contains(cleanQuery, ignoreCase = true) ||
            categoryTitle.contains(cleanQuery, ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogoScreen(
    products: List<Product>,
    categories: List<CategoryItem> = emptyList(),
    categoryTitleById: Map<String, String> = emptyMap(),
    selectedCategory: String?,
    onCategoryChange: (String?) -> Unit,
    onNavigateToMenu: () -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit = {},
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    savedProductIds: Set<String> = emptySet(),
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onToggleSaveProduct: (String) -> Unit = {},
    isLoadingProducts: Boolean = false,
    productsError: String? = null,
    onRefreshProducts: () -> Unit = {},
    pagedProducts: LazyPagingItems<Product>? = null,
    searchQuery: String? = null,
    onSearchQueryChange: ((String) -> Unit)? = null
) {
    var localSearchQuery by remember { mutableStateOf("") }
    val activeSearchQuery = searchQuery ?: localSearchQuery
    val handleSearchQueryChange: (String) -> Unit = { newQuery ->
        if (onSearchQueryChange != null) {
            onSearchQueryChange(newQuery)
        } else {
            localSearchQuery = newQuery
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current
    val categoryListState = rememberLazyListState()
    val gridState = rememberLazyGridState()

    val resolvedCategoryTitles = remember(categories, categoryTitleById) {
        if (categoryTitleById.isNotEmpty() || categories.isEmpty()) {
            categoryTitleById
        } else {
            categories.associate { it.id to it.title.trim() }
        }
    }

    val filteredProducts by produceState(
        initialValue = if (pagedProducts != null) {
            emptyList()
        } else if (activeSearchQuery.isBlank() && selectedCategory == null) {
            products
        } else if (activeSearchQuery.isBlank() && products.size <= 200) {
            filtrarProductosCatalogo(products, resolvedCategoryTitles, selectedCategory, "")
        } else {
            products
        },
        key1 = pagedProducts != null,
        key2 = products to resolvedCategoryTitles,
        key3 = selectedCategory to activeSearchQuery
    ) {
        if (pagedProducts != null) {
            value = emptyList()
            return@produceState
        }
        val cleanQuery = activeSearchQuery.trim()
        if (cleanQuery.isEmpty() && selectedCategory == null) {
            value = products
            return@produceState
        }
        if (cleanQuery.isNotEmpty()) {
            delay(220L)
        }
        value = withContext(Dispatchers.Default) {
            filtrarProductosCatalogo(
                products = products,
                categoryTitleById = resolvedCategoryTitles,
                selectedCategory = selectedCategory,
                query = cleanQuery
            )
        }
    }

    val coverSourceProducts = remember(pagedProducts?.itemSnapshotList, products) {
        val pagedSnapshot = pagedProducts?.itemSnapshotList?.items.orEmpty()
        if (pagedSnapshot.isNotEmpty()) pagedSnapshot else products
    }
    val prefetchProducts = if (pagedProducts != null) {
        pagedProducts.itemSnapshotList.items
    } else {
        filteredProducts
    }
    val gridHeaderItemCount = remember(categories.isNotEmpty()) {
        (if (categories.isNotEmpty()) 1 else 0) + 1
    }

    EfectoPrecargaMiniaturasCatalogo(
        gridState = gridState,
        products = prefetchProducts,
        headerItemCount = gridHeaderItemCount
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CabeceraBusquedaCatalogo(
                searchQuery = activeSearchQuery,
                onSearchQueryChange = handleSearchQueryChange,
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
                state = gridState,
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
                if (categories.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        CatalogoCategoryStoriesSection(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            categoryListState = categoryListState,
                            onCategoryChange = onCategoryChange,
                            products = coverSourceProducts
                        )
                    }
                }

                val refreshState = pagedProducts?.loadState?.refresh
                val appendState = pagedProducts?.loadState?.append
                val visibleCount = pagedProducts?.itemCount ?: filteredProducts.size
                val isInitialLoading = if (pagedProducts != null) {
                    refreshState is LoadState.Loading && pagedProducts.itemCount == 0
                } else {
                    isLoadingProducts && products.isEmpty()
                }
                val initialErrorMessage = if (pagedProducts != null) {
                    (refreshState as? LoadState.Error)?.let { err ->
                        err.error.localizedMessage?.takeIf { it.isNotBlank() }
                            ?: "No se pudo cargar el catálogo desde el servidor."
                    }
                } else {
                    if (products.isEmpty()) productsError else null
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    val countText = when {
                        isInitialLoading -> "Cargando prendas..."
                        pagedProducts != null && appendState != null && !appendState.endOfPaginationReached ->
                            "$visibleCount+ prendas encontradas"
                        else -> "$visibleCount prendas encontradas"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = if (isCompact) 2.dp else 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = countText,
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

                if (visibleCount == 0) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EstadoVacioCatalogo(
                            isCompact = isCompact,
                            isLoading = isInitialLoading,
                            errorMessage = initialErrorMessage,
                            hasActiveFilters = activeSearchQuery.isNotBlank() || selectedCategory != null,
                            onRetry = {
                                pagedProducts?.retry()
                                onRefreshProducts()
                            },
                            onResetFilters = {
                                handleSearchQueryChange("")
                                onCategoryChange(null)
                            }
                        )
                    }
                }

                if (visibleCount > 0) {
                    val isBackgroundSyncing = if (pagedProducts != null) {
                        refreshState is LoadState.Loading
                    } else {
                        isLoadingProducts
                    }
                    val hasOfflineRefreshError = if (pagedProducts != null) {
                        refreshState is LoadState.Error
                    } else {
                        !productsError.isNullOrBlank()
                    }
                    if (isBackgroundSyncing) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            IndicadorSincronizacionEnSegundoPlano()
                        }
                    } else if (hasOfflineRefreshError) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            BannerCatalogoSinConexion(
                                onRetry = {
                                    pagedProducts?.refresh()
                                    onRefreshProducts()
                                }
                            )
                        }
                    }
                }

                if (pagedProducts != null) {
                    items(
                        count = pagedProducts.itemCount,
                        key = pagedProducts.itemKey { it.productId() }
                    ) { index ->
                        val product = pagedProducts[index] ?: return@items
                        val prodId = product.productId()
                        val isSaved = savedProductIds.contains(prodId)
                        val categoryLabel = product.productCategoryDisplayName(resolvedCategoryTitles)
                        ProductGridCard(
                            product = product,
                            isSaved = isSaved,
                            onClick = { onProductClick(product) },
                            categoryLabel = categoryLabel,
                            onToggleSave = {
                                val willSave = !isSaved
                                val notifySaved = {
                                    val msg = if (willSave) "Guardado en tu colección" else "Eliminado de guardados"
                                    if (notifier != null) {
                                        notifier.show(msg)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                }
                                if (!isAuthenticated) {
                                    onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                                        onToggleSaveProduct(prodId)
                                        notifySaved()
                                    }
                                } else {
                                    onToggleSaveProduct(prodId)
                                    notifySaved()
                                }
                            }
                        )
                    }

                    when (appendState) {
                        is LoadState.Loading -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                IndicadorCargaSiguientePagina()
                            }
                        }
                        is LoadState.Error -> {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                ErrorCargaSiguientePagina(
                                    message = appendState.error.localizedMessage,
                                    onRetry = { pagedProducts.retry() }
                                )
                            }
                        }
                        is LoadState.NotLoading -> {
                            if (appendState.endOfPaginationReached && pagedProducts.itemCount > 0) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    FinCatalogoPaginado()
                                }
                            }
                        }
                        null -> Unit
                    }
                } else {
                    items(filteredProducts, key = { it.productId() }) { product ->
                        val prodId = product.productId()
                        val isSaved = savedProductIds.contains(prodId)
                        val categoryLabel = product.productCategoryDisplayName(resolvedCategoryTitles)
                        ProductGridCard(
                            product = product,
                            isSaved = isSaved,
                            onClick = { onProductClick(product) },
                            categoryLabel = categoryLabel,
                            onToggleSave = {
                                val willSave = !isSaved
                                val notifySaved = {
                                    val msg = if (willSave) "Guardado en tu colección" else "Eliminado de guardados"
                                    if (notifier != null) {
                                        notifier.show(msg)
                                    } else {
                                        scope.launch { snackbarHostState.showSnackbar(msg) }
                                    }
                                }
                                if (!isAuthenticated) {
                                    onRequireAuth("Regístrate para guardar prendas en tu colección.") {
                                        onToggleSaveProduct(prodId)
                                        notifySaved()
                                    }
                                } else {
                                    onToggleSaveProduct(prodId)
                                    notifySaved()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
