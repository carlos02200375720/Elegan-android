package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryItem
import com.example.data.model.Product
import com.example.data.model.productShippingMethods
import com.example.data.model.productCategoryDisplayName
import com.example.data.model.productColors
import com.example.data.model.productId
import com.example.data.model.productSizes
import com.example.ui.components.LocalEleganNotifier
import kotlinx.coroutines.launch

@Composable
fun ProductoScreen(
    product: Product,
    cartItemCount: Long,
    isSaved: Boolean = false,
    onToggleSave: ((Result<Boolean>) -> Unit) -> Unit = {},
    onAddToCart: (Product, String, String, Long, (Result<Unit>) -> Unit) -> Unit,
    onBuyNow: (Product, String, String, Long, (Result<Unit>) -> Unit) -> Unit,
    onNavigateToCart: () -> Unit,
    onBack: () -> Unit,
    categories: List<CategoryItem> = emptyList(),
    recommendedProducts: List<Product> = emptyList(),
    savedProductIds: Set<String> = emptySet(),
    onSelectRecommendedProduct: (Product) -> Unit = {},
    onToggleSaveRecommended: (String, (Result<Boolean>) -> Unit) -> Unit = { _, _ -> }
) {
    BackHandler { onBack() }

    val productId = product.productId()
    val defaultSize = product.productSizes().firstOrNull() ?: "Estándar"
    val shippingMethods = remember(product) { product.productShippingMethods() }
    var selectedShippingId by remember(productId, shippingMethods) {
        mutableStateOf(shippingMethods.firstOrNull()?.id.orEmpty())
    }
    val productForCart = remember(product, selectedShippingId) {
        product.copy(selectedShippingId = selectedShippingId)
    }
    var selectedSize by remember(productId) { mutableStateOf(defaultSize) }
    var selectedColor by remember(productId) { mutableStateOf(product.productColors().firstOrNull().orEmpty()) }
    var quantity by remember(productId) { mutableLongStateOf(1L) }
    var showOptionsSheet by remember(productId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current

    val listState = rememberLazyListState()
    val mediaItems = remember(product) { getProductMediaItems(product) }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    LaunchedEffect(productId) {
        listState.scrollToItem(0)
        if (pagerState.currentPage != 0 && mediaItems.isNotEmpty()) {
            pagerState.scrollToPage(0)
        }
    }

    fun notifyMessage(message: String) {
        if (notifier != null) {
            notifier.show(message)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    fun confirmAddToCart() {
        showOptionsSheet = false
        onAddToCart(productForCart, selectedSize, selectedColor, quantity) { result ->
            if (result.isSuccess) {
                notifyMessage("¡Añadido al carrito con éxito!")
            } else {
                notifyMessage(
                    result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "No se pudo agregar al carrito. Verifica tu conexión."
                )
            }
        }
    }

    fun confirmBuyNow() {
        showOptionsSheet = false
        onBuyNow(productForCart, selectedSize, selectedColor, quantity) { result ->
            if (result.isFailure) {
                notifyMessage(
                    result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }
                        ?: "No se pudo sincronizar el carrito con el servidor."
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BarraInferiorCompraProducto(
                onChooseOption = { showOptionsSheet = true }
            )
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("producto_screen")
            ) {
                item {
                    VisorGaleriaProducto(
                        product = product,
                        mediaItems = mediaItems,
                        pagerState = pagerState,
                        categoryLabel = product.productCategoryDisplayName(categories)
                    )
                }

                item {
                    BloqueDetalleProducto(product = product)
                }

                if (recommendedProducts.isNotEmpty()) {
                    item(key = "recommended_products_carousel") {
                        CarruselProductosRecomendados(
                            recommendedProducts = recommendedProducts,
                            categories = categories,
                            savedProductIds = savedProductIds,
                            onProductClick = onSelectRecommendedProduct,
                            onToggleSaveProduct = { recommendedId, willSave ->
                                onToggleSaveRecommended(recommendedId) { result ->
                                    notifyMessage(
                                        if (result.isSuccess) {
                                            if (willSave) "Guardado en tu colección"
                                            else "Eliminado de guardados"
                                        } else {
                                            "No se pudo actualizar la publicación guardada"
                                        }
                                    )
                                }
                            }
                        )
                    }
                }

                item {
                    TarjetaGarantiaProducto(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            CabeceraFlotanteProducto(
                cartItemCount = cartItemCount,
                isSaved = isSaved,
                onBack = onBack,
                onToggleSave = {
                    val willSave = !isSaved
                    onToggleSave { result ->
                        notifier?.show(
                            if (result.isSuccess) {
                                if (willSave) "Publicación guardada en tu perfil"
                                else "Publicación eliminada de guardados"
                            } else {
                                "No se pudo actualizar la publicación guardada"
                            }
                        )
                    }
                },
                onNavigateToCart = onNavigateToCart,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    if (showOptionsSheet) {
        PanelVariantesCompraProducto(
            product = product,
            selectedSize = selectedSize,
            onSelectSize = { selectedSize = it },
            selectedColor = selectedColor,
            onSelectColor = { selectedColor = it },
            shippingMethods = shippingMethods,
            selectedShippingId = selectedShippingId,
            onSelectShipping = { selectedShippingId = it },
            quantity = quantity,
            onDecreaseQuantity = { if (quantity > 1) quantity-- },
            onIncreaseQuantity = { quantity++ },
            onAddToCart = ::confirmAddToCart,
            onBuyNow = ::confirmBuyNow,
            onDismiss = { showOptionsSheet = false }
        )
    }
}
