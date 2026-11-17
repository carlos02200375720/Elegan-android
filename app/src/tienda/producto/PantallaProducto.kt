package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
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
import com.example.data.model.ProductJson
import com.example.data.model.productColors
import com.example.data.model.productId
import com.example.data.model.productSizes
import com.example.ui.components.LocalEleganNotifier
import kotlinx.coroutines.launch

@Composable
fun ProductoScreen(
    product: ProductJson,
    cartItemCount: Long,
    isSaved: Boolean = false,
    onToggleSave: ((Result<Boolean>) -> Unit) -> Unit = {},
    onAddToCart: (ProductJson, String, String, Long) -> Unit,
    onBuyNow: (ProductJson, String, String, Long) -> Unit,
    onNavigateToCart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val productId = product.productId()
    val defaultSize = product.productSizes().firstOrNull() ?: "Estándar"
    var selectedSize by remember(productId) { mutableStateOf(defaultSize) }
    var selectedColor by remember(productId) { mutableStateOf(product.productColors().firstOrNull().orEmpty()) }
    var quantity by remember { mutableLongStateOf(1L) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current

    val mediaItems = remember(product) { getProductMediaItems(product) }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            BarraInferiorCompraProducto(
                onAddToCart = {
                    onAddToCart(product, selectedSize, selectedColor, quantity)
                    if (notifier != null) {
                        notifier.show("¡Añadido al carrito con éxito!")
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("¡Añadido al carrito con éxito!")
                        }
                    }
                },
                onBuyNow = {
                    onBuyNow(product, selectedSize, selectedColor, quantity)
                }
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
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("producto_screen")
            ) {
                item {
                    VisorGaleriaProducto(
                        product = product,
                        mediaItems = mediaItems,
                        pagerState = pagerState
                    )
                }

                item {
                    BloqueDetalleProducto(
                        product = product,
                        selectedSize = selectedSize,
                        onSelectSize = { selectedSize = it },
                        selectedColor = selectedColor,
                        onSelectColor = { selectedColor = it },
                        quantity = quantity,
                        onDecreaseQuantity = { if (quantity > 1) quantity-- },
                        onIncreaseQuantity = { quantity++ }
                    )
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
}
