package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.ProductJson
import com.example.data.model.ProductShippingMethod
import com.example.data.model.productColors
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productSizes
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanelVariantesCompraProducto(
    product: ProductJson,
    selectedSize: String,
    onSelectSize: (String) -> Unit,
    selectedColor: String,
    onSelectColor: (String) -> Unit,
    shippingMethods: List<ProductShippingMethod>,
    selectedShippingId: String,
    onSelectShipping: (String) -> Unit,
    quantity: Long,
    onDecreaseQuantity: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sizes = remember(product) { product.productSizes() }
    val colors = remember(product) { product.productColors() }
    val thumbnailUrl = remember(product) { getProductGalleryImages(product).firstOrNull().orEmpty() }
    val selectedShipping = remember(shippingMethods, selectedShippingId) {
        shippingMethods.firstOrNull { it.id == selectedShippingId }
    }
    val unitPrice = product.productPrice()
    val shippingPrice = selectedShipping?.price ?: 0.0
    val estimatedTotal = (unitPrice * quantity) + shippingPrice

    val summaryParts = buildList {
        if (sizes.isNotEmpty() && selectedSize.isNotBlank()) add("Talla: $selectedSize")
        if (colors.isNotEmpty() && selectedColor.isNotBlank()) add("Color: $selectedColor")
        add("Cant.: $quantity")
    }.joinToString(" • ")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD5CCC3))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_options_bottom_sheet")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF0EBE3)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailUrl.isNotBlank()) {
                        AsyncImage(
                            model = thumbnailUrl,
                            contentDescription = product.productName(),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Checkroom,
                            contentDescription = product.productName(),
                            tint = Color(0xFFB5ABA0),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.productName(),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatearPrecioRd(unitPrice),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EleganBurgundy,
                                fontSize = 17.sp
                            )
                        )
                        if (quantity > 1L || shippingPrice > 0.0) {
                            Text(
                                text = stringResource(
                                    R.string.producto_panel_estimated_total,
                                    formatearPrecioRd(estimatedTotal)
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF5D534A)
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = summaryParts,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF6B6259)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("panel_selection_summary")
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("panel_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.producto_panel_close_cd),
                        tint = EleganCharcoal
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEAE2DA))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                if (sizes.isNotEmpty()) {
                    SelectorOpcionesProducto(
                        title = "Seleccionar Talla / Medida:",
                        options = sizes,
                        selectedOption = selectedSize,
                        onSelect = onSelectSize,
                        rowTestTag = "panel_size_selector_row",
                        chipTagPrefix = "panel_size_chip_"
                    )
                }

                if (colors.isNotEmpty()) {
                    SelectorOpcionesProducto(
                        title = "Seleccionar Color:",
                        options = colors,
                        selectedOption = selectedColor,
                        onSelect = onSelectColor,
                        rowTestTag = "panel_color_selector_row",
                        chipTagPrefix = "panel_color_chip_"
                    )
                }

                SelectorEnvioProducto(
                    shippingMethods = shippingMethods,
                    selectedShippingId = selectedShippingId,
                    onSelectShipping = onSelectShipping,
                    testTagPrefix = "panel_shipping_method_"
                )

                SelectorCantidadProducto(
                    quantity = quantity,
                    onDecrease = onDecreaseQuantity,
                    onIncrease = onIncreaseQuantity,
                    testTagPrefix = "panel_quantity_"
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            EleganBottomBarShadow()
            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onAddToCart,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("add_to_cart_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EleganBurgundy
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                            brush = SolidColor(EleganBurgundy)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.producto_panel_add_to_cart),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onBuyNow,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 48.dp)
                            .testTag("buy_now_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EleganBurgundy,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.producto_panel_buy_now),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
