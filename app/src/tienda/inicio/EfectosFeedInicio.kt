package com.example.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.esUrlDeVideo
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productImages
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val MAX_PREFETCHED_URLS_CACHE = 250

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

internal fun calcularVentanaIdsVisibles(
    featuredProducts: List<Product>,
    firstVisibleItemIndex: Int,
    visibleItemCount: Int,
    headerItemCount: Int,
    lookaheadCount: Int = 6,
    maxWindowSize: Int = 20
): List<String> {
    if (featuredProducts.isEmpty() || maxWindowSize <= 0) return emptyList()
    val startIndex = (firstVisibleItemIndex - headerItemCount).coerceAtLeast(0)
    val span = (visibleItemCount.coerceAtLeast(1) + lookaheadCount.coerceAtLeast(0))
        .coerceAtMost(maxWindowSize)
    val endIndex = (startIndex + span).coerceAtMost(featuredProducts.size)
    if (startIndex >= endIndex) return emptyList()

    val result = ArrayList<String>(endIndex - startIndex)
    for (i in startIndex until endIndex) {
        val id = featuredProducts[i].productId().trim()
        if (id.isNotBlank()) {
            result.add(id)
        }
    }
    return result.distinct().take(maxWindowSize)
}

@Composable
internal fun EfectoConteosVisiblesInicio(
    listState: LazyListState,
    featuredProducts: List<Product>,
    headerItemCount: Int = 2,
    onVisibleProductsChanged: (List<String>) -> Unit
) {
    // Solicita conteos remotos únicamente para la ventana visible + próximas tarjetas cuando el scroll descansa.
    LaunchedEffect(listState, featuredProducts, headerItemCount, onVisibleProductsChanged) {
        if (featuredProducts.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            Triple(
                listState.isScrollInProgress,
                listState.firstVisibleItemIndex,
                listState.layoutInfo.visibleItemsInfo.size
            )
        }
            .distinctUntilChanged()
            .collectLatest { (scrolling, firstVisibleIndex, visibleCount) ->
                if (!scrolling) {
                    delay(180L)
                    val windowIds = calcularVentanaIdsVisibles(
                        featuredProducts = featuredProducts,
                        firstVisibleItemIndex = firstVisibleIndex,
                        visibleItemCount = visibleCount,
                        headerItemCount = headerItemCount
                    )
                    if (windowIds.isNotEmpty()) {
                        onVisibleProductsChanged(windowIds)
                    }
                }
            }
    }
}

@Composable
internal fun EfectoPrecargaImagenesInicio(
    listState: LazyListState,
    featuredProducts: List<Product>,
    headerItemCount: Int = 2
) {
    // Precarga las primeras imágenes de las próximas tarjetas bajo demanda sin mapear todo el catálogo en UI.
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    val prefetchedUrls = remember(featuredProducts) { LinkedHashSet<String>(64) }

    LaunchedEffect(listState, featuredProducts, headerItemCount, imageLoader) {
        if (featuredProducts.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collectLatest { firstVisibleIndex ->
                val startProductIndex = (firstVisibleIndex - headerItemCount).coerceAtLeast(0)
                val endProductIndex = (startProductIndex + 5).coerceAtMost(featuredProducts.size)
                for (i in startProductIndex until endProductIndex) {
                    val product = featuredProducts[i]
                    val cover = product.productImageUrl().takeIf { it.isNotBlank() && !esUrlDeVideo(it) }
                    var enqueuedForProduct = 0
                    if (cover != null) {
                        if (registrarUrlPrecargadaAcotada(prefetchedUrls, cover)) {
                            imageLoader.enqueue(construirPeticionImagenPublicacion(context, cover))
                        }
                        enqueuedForProduct++
                    }
                    if (enqueuedForProduct < 2) {
                        for (galleryUrl in product.productImages()) {
                            if (galleryUrl.isBlank() || galleryUrl == cover || esUrlDeVideo(galleryUrl)) continue
                            if (registrarUrlPrecargadaAcotada(prefetchedUrls, galleryUrl)) {
                                imageLoader.enqueue(construirPeticionImagenPublicacion(context, galleryUrl))
                            }
                            enqueuedForProduct++
                            if (enqueuedForProduct >= 2) break
                        }
                    }
                }
            }
    }
}

private fun registrarUrlPrecargadaAcotada(
    cache: LinkedHashSet<String>,
    url: String
): Boolean {
    if (!cache.add(url)) return false
    if (cache.size > MAX_PREFETCHED_URLS_CACHE) {
        val iterator = cache.iterator()
        if (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }
    return true
}

internal fun resolverNombreSaludoInicio(
    userProfile: UserProfile?,
    sessionUserName: String = "",
    sessionUserEmail: String = ""
): String {
    val rawName = userProfile?.displayName?.takeIf { it.isNotBlank() }
        ?: sessionUserName.takeIf { it.isNotBlank() }
        ?: sessionUserEmail.substringBefore("@").takeIf { it.isNotBlank() }
        ?: "Bienvenido"
    return rawName.trim()
}
