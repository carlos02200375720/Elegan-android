package com.example.ui

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import com.example.data.media.ConfiguracionCacheImagenesCoil
import com.example.data.model.Product
import com.example.data.model.esUrlDeVideo
import com.example.data.model.productImageUrl
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

private const val MAX_PREFETCHED_CATALOG_THUMBNAILS = 300
private const val DEFAULT_CATALOG_LOOKAHEAD_ITEMS = 12

internal fun calcularUrlsMiniaturasAPrecargar(
    products: List<Product>,
    firstVisibleItemIndex: Int,
    visibleItemCount: Int,
    headerItemCount: Int,
    lookaheadCount: Int = DEFAULT_CATALOG_LOOKAHEAD_ITEMS
): List<String> {
    if (products.isEmpty() || lookaheadCount <= 0) return emptyList()
    val startProductIndex = (firstVisibleItemIndex - headerItemCount).coerceAtLeast(0)
    val span = visibleItemCount.coerceAtLeast(1) + lookaheadCount
    val endProductIndex = (startProductIndex + span).coerceAtMost(products.size)
    if (startProductIndex >= endProductIndex) return emptyList()

    val result = ArrayList<String>(endProductIndex - startProductIndex)
    for (index in startProductIndex until endProductIndex) {
        val url = products[index].productImageUrl().trim()
        if (url.isNotBlank() && !esUrlDeVideo(url)) {
            result.add(url)
        }
    }
    return result
}

/**
 * Precarga en memoria y caché de disco de Coil las miniaturas de los próximos productos
 * mientras el usuario navega por la cuadrícula paginada del catálogo.
 */
@Composable
internal fun EfectoPrecargaMiniaturasCatalogo(
    gridState: LazyGridState,
    products: List<Product>,
    headerItemCount: Int
) {
    val context = LocalContext.current
    val imageLoader = remember(context) { context.imageLoader }
    val prefetchedKeys = remember { LinkedHashSet<String>(64) }

    LaunchedEffect(gridState, products, headerItemCount, imageLoader) {
        if (products.isEmpty()) return@LaunchedEffect
        snapshotFlow {
            gridState.firstVisibleItemIndex to gridState.layoutInfo.visibleItemsInfo.size
        }
            .distinctUntilChanged()
            .collectLatest { (firstVisibleIndex, visibleCount) ->
                val urls = calcularUrlsMiniaturasAPrecargar(
                    products = products,
                    firstVisibleItemIndex = firstVisibleIndex,
                    visibleItemCount = visibleCount,
                    headerItemCount = headerItemCount
                )
                for (url in urls) {
                    val canonicalKey = ConfiguracionCacheImagenesCoil.claveCacheDisco(url)
                    if (registrarClavePrecargadaAcotada(prefetchedKeys, canonicalKey)) {
                        imageLoader.enqueue(
                            ConfiguracionCacheImagenesCoil.peticionMiniaturaProducto(
                                context = context,
                                imageUrl = url
                            )
                        )
                    }
                }
            }
    }
}

private fun registrarClavePrecargadaAcotada(
    cache: LinkedHashSet<String>,
    key: String
): Boolean {
    if (key.isBlank() || !cache.add(key)) return false
    if (cache.size > MAX_PREFETCHED_CATALOG_THUMBNAILS) {
        val iterator = cache.iterator()
        if (iterator.hasNext()) {
            iterator.next()
            iterator.remove()
        }
    }
    return true
}
