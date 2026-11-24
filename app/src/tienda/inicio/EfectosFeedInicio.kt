package com.example.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import com.example.data.model.ProductJson
import com.example.data.model.UserProfile
import com.example.data.model.esUrlDeVideo
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import com.example.data.remote.MobileSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun EfectoFocoVideoInicio(
    listState: LazyListState,
    videoProductIds: Set<String>,
    focusedProductIdState: MutableState<String?>
) {
    // Actualiza el foco de video solo cuando el scroll se detiene para no interrumpir la fluidez del desplazamiento.
    LaunchedEffect(listState, videoProductIds, focusedProductIdState) {
        if (videoProductIds.isEmpty()) {
            focusedProductIdState.value = null
            return@LaunchedEffect
        }
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { scrolling ->
                if (!scrolling) {
                    delay(220L)
                    focusedProductIdState.value = calcularProductoVideoEnFoco(listState, videoProductIds)
                }
            }
    }
}

private fun calcularProductoVideoEnFoco(
    listState: LazyListState,
    videoProductIds: Set<String>
): String? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null

    val vpStart = layoutInfo.viewportStartOffset
    val vpEnd = layoutInfo.viewportEndOffset
    val vpCenter = (vpStart + vpEnd) / 2
    val visibleVideoProducts = visibleItems.filter { it.key in videoProductIds }
    if (visibleVideoProducts.isEmpty()) return null

    val centerProduct = visibleVideoProducts.find { item ->
        item.offset <= vpCenter && (item.offset + item.size) >= vpCenter
    }
    return if (centerProduct != null) {
        val visibleTop = maxOf(centerProduct.offset, vpStart)
        val visibleBottom = minOf(centerProduct.offset + centerProduct.size, vpEnd)
        if ((visibleBottom - visibleTop) >= 100) centerProduct.key as? String else null
    } else {
        val maxVisible = visibleVideoProducts.maxByOrNull { item ->
            val visibleTop = maxOf(item.offset, vpStart)
            val visibleBottom = minOf(item.offset + item.size, vpEnd)
            (visibleBottom - visibleTop).coerceAtLeast(0)
        }
        if (maxVisible != null) {
            val visibleTop = maxOf(maxVisible.offset, vpStart)
            val visibleBottom = minOf(maxVisible.offset + maxVisible.size, vpEnd)
            if ((visibleBottom - visibleTop) >= 140) maxVisible.key as? String else null
        } else {
            null
        }
    }
}

@Composable
internal fun EfectoPrecargaImagenesInicio(
    listState: LazyListState,
    featuredProducts: List<ProductJson>,
    headerItemCount: Int = 2
) {
    // Precarga las primeras imágenes de las próximas tarjetas compensando las secciones fijas del feed.
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    val productPreviewUrls = remember(featuredProducts) {
        featuredProducts.map { product ->
            val cover = product.productImageUrl().takeIf { it.isNotBlank() && !esUrlDeVideo(it) }
            val gallery = product.productImages().filter { it.isNotBlank() && !esUrlDeVideo(it) }
            (listOfNotNull(cover) + gallery).distinct().take(2)
        }
    }
    val prefetchedUrls = remember(productPreviewUrls) { HashSet<String>(productPreviewUrls.size * 2) }

    LaunchedEffect(listState, productPreviewUrls, headerItemCount, imageLoader) {
        if (productPreviewUrls.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { firstVisibleIndex ->
                val startProductIndex = (firstVisibleIndex - headerItemCount).coerceAtLeast(0)
                val endProductIndex = (startProductIndex + 5).coerceAtMost(productPreviewUrls.size)
                for (i in startProductIndex until endProductIndex) {
                    productPreviewUrls[i].forEach { url ->
                        if (url.isNotBlank() && prefetchedUrls.add(url)) {
                            imageLoader.enqueue(
                                construirPeticionImagenPublicacion(context, url)
                            )
                        }
                    }
                }
            }
    }
}

internal fun resolverNombreSaludoInicio(
    context: Context,
    userProfile: UserProfile?
): String {
    val currentUser = MobileSession.user(context)
    val rawName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.name?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Bienvenido"
    return rawName.trim()
}
