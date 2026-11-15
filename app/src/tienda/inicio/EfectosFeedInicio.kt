package com.example.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.remote.MobileSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun EfectoFocoVideoInicio(
    listState: LazyListState,
    videoProductIds: Set<String>,
    onFocusedProductChange: (String?) -> Unit
) {
    // Activa el foco de video únicamente cuando el usuario detiene el scroll para evitar bloqueos por tarjeta.
    LaunchedEffect(listState, videoProductIds) {
        if (videoProductIds.isEmpty()) {
            onFocusedProductChange(null)
            return@LaunchedEffect
        }
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collectLatest { scrolling ->
                if (scrolling) {
                    onFocusedProductChange(null)
                } else {
                    delay(220L)
                    onFocusedProductChange(calcularProductoVideoEnFoco(listState, videoProductIds))
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
    featuredProducts: List<Product>
) {
    // Precarga las portadas de las próximas tarjetas antes de que entren en pantalla.
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    LaunchedEffect(listState, featuredProducts, imageLoader) {
        if (featuredProducts.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collectLatest { lastVisibleIndex ->
                val startProductIndex = (lastVisibleIndex - 3 + 1).coerceAtLeast(0)
                val endProductIndex = (startProductIndex + 4).coerceAtMost(featuredProducts.size)
                for (i in startProductIndex until endProductIndex) {
                    val p = featuredProducts[i]
                    val coverUrl = p.imageUrl.takeIf {
                        it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true)
                    } ?: p.images.firstOrNull {
                        it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true)
                    }
                    if (!coverUrl.isNullOrBlank()) {
                        val request = ImageRequest.Builder(context)
                            .data(coverUrl)
                            .size(900, 900)
                            .precision(Precision.INEXACT)
                            .allowRgb565(true)
                            .build()
                        imageLoader.enqueue(request)
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
