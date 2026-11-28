package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.example.data.model.Product
import com.example.data.model.ProductShippingMethod
import com.example.data.model.productColors
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productSizes
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PanelVariantesCompraProducto(
    product: Product,
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
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("product_options_bottom_sheet")
        ) {
            val isUltraCompact = maxWidth < 340.dp
            val isCompact = maxWidth < 385.dp

            val headerStartPad = when {
                isUltraCompact -> 12.dp
                isCompact -> 14.dp
                else -> 16.dp
            }
            val headerEndPad = when {
                isUltraCompact -> 2.dp
                isCompact -> 4.dp
                else -> 8.dp
            }
            val thumbnailSize = when {
                isUltraCompact -> 52.dp
                isCompact -> 58.dp
                else -> 64.dp
            }
            val headerImageGap = if (isCompact) 10.dp else 12.dp
            val bodyHorizontalPad = when {
                isUltraCompact -> 14.dp
                isCompact -> 16.dp
                else -> 20.dp
            }
            val bodyVerticalPad = when {
                isUltraCompact -> 12.dp
                isCompact -> 14.dp
                else -> 16.dp
            }
            val footerHorizontalPad = when {
                isUltraCompact -> 12.dp
                isCompact -> 14.dp
                else -> 16.dp
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = headerStartPad,
                            end = headerEndPad,
                            top = 4.dp,
                            bottom = if (isUltraCompact) 10.dp else 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(thumbnailSize)
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
                                modifier = Modifier.size(if (isUltraCompact) 24.dp else 28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(headerImageGap))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.productName(),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = when {
                                    isUltraCompact -> 13.5.sp
                                    isCompact -> 14.sp
                                    else -> 15.sp
                                },
                                lineHeight = when {
                                    isUltraCompact -> 17.sp
                                    isCompact -> 18.sp
                                    else -> 20.sp
                                }
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(if (isCompact) 6.dp else 8.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = formatearPrecioRd(unitPrice),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EleganBurgundy,
                                    fontSize = when {
                                        isUltraCompact -> 14.5.sp
                                        isCompact -> 15.5.sp
                                        else -> 17.sp
                                    }
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                            if (quantity > 1L || shippingPrice > 0.0) {
                                Text(
                                    text = stringResource(
                                        R.string.producto_panel_estimated_total,
                                        formatearPrecioRd(estimatedTotal)
                                    ),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF5D534A),
                                        fontSize = when {
                                            isUltraCompact -> 11.sp
                                            isCompact -> 11.5.sp
                                            else -> 12.sp
                                        }
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summaryParts,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF6B6259),
                                fontSize = if (isUltraCompact) 11.sp else 12.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("panel_selection_summary")
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("panel_close_btn")
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
                        .padding(horizontal = bodyHorizontalPad, vertical = bodyVerticalPad)
                ) {
                    if (sizes.isNotEmpty()) {
                        SelectorOpcionesProducto(
                            title = "Seleccionar Talla / Medida:",
                            options = sizes,
                            selectedOption = selectedSize,
                            onSelect = onSelectSize,
                            rowTestTag = "panel_size_selector_row",
                            chipTagPrefix = "panel_size_chip_",
                            isCompact = isCompact
                        )
                    }

                    if (colors.isNotEmpty()) {
                        SelectorOpcionesProducto(
                            title = "Seleccionar Color:",
                            options = colors,
                            selectedOption = selectedColor,
                            onSelect = onSelectColor,
                            rowTestTag = "panel_color_selector_row",
                            chipTagPrefix = "panel_color_chip_",
                            isCompact = isCompact
                        )
                    }

                    SelectorEnvioProducto(
                        shippingMethods = shippingMethods,
                        selectedShippingId = selectedShippingId,
                        onSelectShipping = onSelectShipping,
                        testTagPrefix = "panel_shipping_method_",
                        isCompact = isCompact
                    )

                    SelectorCantidadProducto(
                        quantity = quantity,
                        onDecrease = onDecreaseQuantity,
                        onIncrease = onIncreaseQuantity,
                        testTagPrefix = "panel_quantity_",
                        isCompact = isCompact
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
                            .padding(
                                horizontal = footerHorizontalPad,
                                vertical = if (isUltraCompact) 10.dp else 12.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 8.dp else 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onAddToCart,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("add_to_cart_btn"),
                            contentPadding = PaddingValues(
                                horizontal = if (isUltraCompact) 6.dp else 8.dp,
                                vertical = 10.dp
                            ),
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
                                modifier = Modifier.size(if (isUltraCompact) 15.dp else 16.dp)
                            )
                            Spacer(modifier = Modifier.width(if (isUltraCompact) 4.dp else 6.dp))
                            Text(
                                text = stringResource(R.string.producto_panel_add_to_cart),
                                fontSize = if (isUltraCompact) 12.5.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Button(
                            onClick = onBuyNow,
                            modifier = Modifier
                                .weight(1.2f)
                                .heightIn(min = 48.dp)
                                .testTag("buy_now_btn"),
                            contentPadding = PaddingValues(
                                horizontal = if (isUltraCompact) 6.dp else 8.dp,
                                vertical = 10.dp
                            ),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.producto_panel_buy_now),
                                fontSize = if (isUltraCompact) 12.5.sp else 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
