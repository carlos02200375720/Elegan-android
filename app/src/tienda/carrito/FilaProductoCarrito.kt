package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganCharcoalLight
import com.example.ui.theme.EleganMuted

@Composable
fun CartItemRow(
    item: CartItem,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_${item.id}"),
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = null
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isUltraCompact = maxWidth < 310.dp
            val isCompactCard = maxWidth < 350.dp
            val isExpandedCard = maxWidth >= 500.dp

            val startPad = when {
                isUltraCompact -> 10.dp
                isCompactCard -> 12.dp
                else -> 16.dp
            }
            val endIconPad = when {
                isCompactCard -> 0.dp
                else -> 2.dp
            }
            val endTextPad = (startPad - endIconPad).coerceAtLeast(8.dp)
            val verticalPad = when {
                isUltraCompact -> 8.dp
                isCompactCard -> 10.dp
                else -> 12.dp
            }
            val imageWidth = when {
                isUltraCompact -> 68.dp
                isCompactCard -> 78.dp
                isExpandedCard -> 96.dp
                else -> 88.dp
            }
            val imageHeight = when {
                isUltraCompact -> 88.dp
                isCompactCard -> 98.dp
                isExpandedCard -> 116.dp
                else -> 108.dp
            }
            val imageGap = when {
                isUltraCompact -> 8.dp
                isCompactCard -> 12.dp
                isExpandedCard -> 16.dp
                else -> 14.dp
            }
            val stepperBtnSize = if (isUltraCompact) 28.dp else 32.dp
            val stepperCountPad = if (isUltraCompact) 6.dp else 10.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPad, end = endIconPad),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = verticalPad, bottom = verticalPad)
                        .size(width = imageWidth, height = imageHeight)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF3ECE4))
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(imageGap))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = imageHeight + (verticalPad * 2)),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Top band: Product title, variants & price on left; selector checkbox in top-right corner
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(top = verticalPad)
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = EleganCharcoal,
                                        fontSize = when {
                                            isUltraCompact -> 13.sp
                                            isCompactCard -> 13.5.sp
                                            else -> 14.5.sp
                                        },
                                        lineHeight = when {
                                            isUltraCompact -> 17.sp
                                            isCompactCard -> 18.sp
                                            else -> 19.sp
                                        }
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                val variantParts = buildList {
                                    if (item.selectedSize.isNotBlank()) add("Talla: ${item.selectedSize}")
                                    if (item.selectedColor.isNotBlank()) add("Color: ${item.selectedColor}")
                                }
                                if (variantParts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = variantParts.joinToString("  •  "),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = EleganMuted,
                                            fontSize = if (isUltraCompact) 11.5.sp else 12.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = formatearPrecioRd(item.price),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundyDark,
                                        fontSize = when {
                                            isUltraCompact -> 14.5.sp
                                            isCompactCard -> 15.sp
                                            else -> 16.sp
                                        }
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = onSelectedChange,
                                colors = CheckboxDefaults.colors(
                                    checkedColor = EleganBurgundy,
                                    uncheckedColor = EleganMuted,
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("select_item_${item.id}")
                            )
                        }

                        // Middle band: Shipping details using full content column width (never collides with corners)
                        if (item.shippingMethodName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = endTextPad),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                CartMetadataLine(
                                    label = "Envío:",
                                    value = item.shippingMethodName,
                                    isUltraCompact = isUltraCompact
                                )
                                CartMetadataLine(
                                    label = "Costo de envío:",
                                    value = formatearEnvioRd(item.shippingPrice),
                                    isUltraCompact = isUltraCompact
                                )
                                if (item.shippingDeliveryTime.isNotBlank()) {
                                    CartMetadataLine(
                                        label = "Entrega:",
                                        value = item.shippingDeliveryTime,
                                        isUltraCompact = isUltraCompact
                                    )
                                }
                                if (item.shippingOrigin.isNotBlank()) {
                                    CartMetadataLine(
                                        label = "Origen:",
                                        value = item.shippingOrigin,
                                        isUltraCompact = isUltraCompact
                                    )
                                }
                            }
                        }
                    }

                    // Bottom band: Quantity stepper on left; delete button in bottom-right corner aligned with checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                top = 6.dp,
                                bottom = (verticalPad - 6.dp).coerceAtLeast(2.dp)
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF6EFE9))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            IconButton(
                                onClick = onDecrease,
                                modifier = Modifier
                                    .size(stepperBtnSize)
                                    .testTag("decrease_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Menos",
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "${item.quantity}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    fontSize = if (isUltraCompact) 13.sp else 14.sp
                                ),
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = stepperCountPad)
                            )

                            IconButton(
                                onClick = onIncrease,
                                modifier = Modifier
                                    .size(stepperBtnSize)
                                    .testTag("increase_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Más",
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("remove_item_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Eliminar",
                                tint = Color(0xFFB00020)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartMetadataLine(
    label: String,
    value: String,
    isUltraCompact: Boolean
) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = EleganMuted, fontWeight = FontWeight.Normal)) {
                append("$label ")
            }
            withStyle(SpanStyle(color = EleganCharcoalLight, fontWeight = FontWeight.Medium)) {
                append(value)
            }
        },
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = if (isUltraCompact) 11.5.sp else 12.sp,
            lineHeight = if (isUltraCompact) 15.sp else 16.sp
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
