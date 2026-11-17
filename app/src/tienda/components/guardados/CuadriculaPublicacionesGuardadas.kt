package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductJson

/**
 * Cuadrícula de 3 columnas para mostrar a los clientes todas sus publicaciones guardadas
 * en su perfil. Permite acceder de forma rápida y directa a cualquier publicación guardada.
 */
@Composable
fun SavedPublicationsGrid(
    savedProducts: List<ProductJson>,
    onProductClick: (ProductJson) -> Unit,
    onExploreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("saved_publications_grid_card")
            .padding(0.dp)
    ) {
        CabeceraPublicacionesGuardadas(savedCount = savedProducts.size)

        Spacer(modifier = Modifier.height(14.dp))

        if (savedProducts.isEmpty()) {
            EstadoVacioPublicacionesGuardadas(onExploreClick = onExploreClick)
        } else {
            val rows = savedProducts.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (colIndex in 0 until 3) {
                            val product = rowItems.getOrNull(colIndex)
                            if (product != null) {
                                SavedPublicationGridItem(
                                    product = product,
                                    onClick = { onProductClick(product) },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
