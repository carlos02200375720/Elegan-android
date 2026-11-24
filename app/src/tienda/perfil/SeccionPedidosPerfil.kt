package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.ProductJson
import com.example.data.model.productId
import com.example.data.model.productImageUrl
import com.example.data.model.productName
import com.example.ui.theme.EleganBorder
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganCharcoalLight
import com.example.ui.theme.EleganGreen
import com.example.ui.theme.EleganMuted

@Composable
internal fun SeccionPedidosPerfil(
    orders: List<Order>,
    catalogProducts: List<ProductJson> = emptyList()
) {
    val catalogImageById = remember(catalogProducts) {
        catalogProducts.mapNotNull { product ->
            val id = product.productId()
            val img = product.productImageUrl()
            if (id.isNotBlank() && img.isNotBlank()) id to img else null
        }.toMap()
    }
    val catalogImageByName = remember(catalogProducts) {
        catalogProducts.mapNotNull { product ->
            val name = product.productName().lowercase()
            val img = product.productImageUrl()
            if (name.isNotBlank() && img.isNotBlank()) name to img else null
        }.toMap()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = EleganBurgundy)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Mis Pedidos Recientes (${orders.size})",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (orders.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Aún no has realizado ningún pedido en Elegan.",
                style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            orders.forEach { order ->
                OrderHistoryItemCard(
                    order = order,
                    resolveProductImage = { item ->
                        item.imageUrl.ifBlank {
                            catalogImageById[item.productId]
                                ?: catalogImageByName[item.name.trim().lowercase()]
                                ?: ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun OrderHistoryItemCard(
    order: Order,
    modifier: Modifier = Modifier,
    resolveProductImage: (OrderItem) -> String = { it.imageUrl }
) {
    val (statusLabel, statusBg, statusFg) = remember(order.status) {
        formatOrderStatus(order.status)
    }
    val formattedDate = remember(order.createdAt) {
        formatOrderDate(order.createdAt)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_history_card_${order.id}"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, EleganBorder),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompact = maxWidth < 330.dp
            val cardPad = if (isCompact) 12.dp else 14.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPad)
            ) {
                // Header: Order ID + Date + Status pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Pedido #${order.id.takeLast(6).uppercase()}",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganBurgundyDark,
                                fontSize = if (isCompact) 14.sp else 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (formattedDate.isNotBlank()) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = EleganMuted,
                                    fontSize = 11.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusBg
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = if (isCompact) 9.5.sp else 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusFg,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1ECE7))
                Spacer(modifier = Modifier.height(10.dp))

                // Detailed product list (or fallback summary if backend doesn't send items[])
                if (order.items.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        order.items.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                HorizontalDivider(color = Color(0xFFF6F2EE))
                            }
                            OrderHistoryProductRow(
                                item = item,
                                resolvedImageUrl = resolveProductImage(item),
                                isCompact = isCompact
                            )
                        }
                    }
                } else if (order.itemsSummary.isNotBlank()) {
                    Text(
                        text = order.itemsSummary,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = EleganCharcoal,
                            lineHeight = 18.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1ECE7))
                Spacer(modifier = Modifier.height(8.dp))

                // Delivery, shipping summary & payment info
                val fullAddress = listOf(order.address, order.city)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .joinToString(", ")

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    if (fullAddress.isNotBlank()) {
                        OrderDetailMetaRow(
                            icon = Icons.Default.LocationOn,
                            label = "Entrega:",
                            value = fullAddress
                        )
                    }

                    val hasPerItemShipping = order.items.any {
                        it.shippingMethodName.isNotBlank() || it.shippingDeliveryTime.isNotBlank()
                    }
                    if (!hasPerItemShipping) {
                        if (order.shippingMethodName.isNotBlank()) {
                            val shippingLabel = if (order.shippingCost > 0.0) {
                                "${order.shippingMethodName} (${formatearEnvioRd(order.shippingCost)})"
                            } else {
                                order.shippingMethodName
                            }
                            OrderDetailMetaRow(
                                icon = Icons.Default.LocalShipping,
                                label = "Envío:",
                                value = shippingLabel
                            )
                        }
                        if (order.shippingDeliveryTime.isNotBlank()) {
                            OrderDetailMetaRow(
                                icon = Icons.Default.Schedule,
                                label = "Tiempo de entrega:",
                                value = order.shippingDeliveryTime
                            )
                        }
                    }

                    OrderDetailMetaRow(
                        icon = Icons.Default.Payments,
                        label = "Pago:",
                        value = order.paymentMethod
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFF1ECE7))
                Spacer(modifier = Modifier.height(8.dp))

                // Financial breakdown
                val showBreakdown = order.subtotal > 0.0 || order.shippingCost > 0.0 || order.discount > 0.0
                if (showBreakdown) {
                    val effectiveSubtotal = if (order.subtotal > 0.0) {
                        order.subtotal
                    } else {
                        (order.total - order.shippingCost + order.discount).coerceAtLeast(0.0)
                    }
                    OrderAmountRow(
                        label = "Subtotal",
                        value = formatearPrecioRd(effectiveSubtotal)
                    )
                    if (order.discount > 0.0) {
                        OrderAmountRow(
                            label = "Descuento",
                            value = formatearDescuentoRd(order.discount),
                            valueColor = EleganGreen
                        )
                    }
                    if (order.shippingCost > 0.0 || order.shippingMethodName.isNotBlank()) {
                        OrderAmountRow(
                            label = "Envío",
                            value = formatearEnvioRd(order.shippingCost),
                            valueColor = if (order.shippingCost <= 0.0) EleganGreen else EleganCharcoal
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total (${order.itemCount} ${if (order.itemCount == 1L) "artículo" else "artículos"})",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontSize = if (isCompact) 13.sp else 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatearPrecioRd(order.total),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EleganBurgundy,
                            fontSize = if (isCompact) 15.sp else 16.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderHistoryProductRow(
    item: OrderItem,
    resolvedImageUrl: String,
    isCompact: Boolean
) {
    val thumbWidth = if (isCompact) 52.dp else 60.dp
    val thumbHeight = if (isCompact) 62.dp else 70.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(width = thumbWidth, height = thumbHeight)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3ECE4)),
            contentAlignment = Alignment.Center
        ) {
            if (resolvedImageUrl.isNotBlank()) {
                AsyncImage(
                    model = resolvedImageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = item.name,
                    tint = EleganBurgundy.copy(alpha = 0.55f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EleganCharcoal,
                        fontSize = if (isCompact) 13.sp else 13.5.sp,
                        lineHeight = 17.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (item.price > 0.0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatearPrecioRd(item.price * item.quantity),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundyDark,
                            fontSize = if (isCompact) 13.sp else 13.5.sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            val variantSummary = buildList {
                add("Cant.: ${item.quantity}")
                if (item.price > 0.0 && item.quantity > 1L) {
                    add("${formatearPrecioRd(item.price)} c/u")
                }
                if (item.selectedSize.isNotBlank()) add("Talla: ${item.selectedSize}")
                if (item.selectedColor.isNotBlank()) add("Color: ${item.selectedColor}")
            }.joinToString("  •  ")

            Text(
                text = variantSummary,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = EleganMuted,
                    fontSize = 11.5.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.shippingMethodName.isNotBlank() || item.shippingDeliveryTime.isNotBlank() || item.shippingOrigin.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (item.shippingMethodName.isNotBlank()) {
                        val envioText = if (item.shippingPrice > 0.0) {
                            "${item.shippingMethodName} (${formatearEnvioRd(item.shippingPrice)})"
                        } else {
                            item.shippingMethodName
                        }
                        OrderCompactShippingLine(label = "Envío:", value = envioText)
                    } else if (item.shippingPrice > 0.0) {
                        OrderCompactShippingLine(label = "Envío:", value = formatearEnvioRd(item.shippingPrice))
                    }
                    if (item.shippingDeliveryTime.isNotBlank()) {
                        OrderCompactShippingLine(label = "Entrega:", value = item.shippingDeliveryTime)
                    }
                    if (item.shippingOrigin.isNotBlank()) {
                        OrderCompactShippingLine(label = "Origen:", value = item.shippingOrigin)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCompactShippingLine(
    label: String,
    value: String
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
            fontSize = 11.5.sp,
            lineHeight = 15.sp
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun OrderDetailMetaRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EleganMuted,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = EleganMuted, fontWeight = FontWeight.Normal)) {
                    append("$label ")
                }
                withStyle(SpanStyle(color = EleganCharcoal, fontWeight = FontWeight.Medium)) {
                    append(value)
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                lineHeight = 16.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OrderAmountRow(
    label: String,
    value: String,
    valueColor: Color = EleganCharcoal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EleganMuted,
                fontSize = 12.5.sp
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = valueColor,
                fontSize = 12.5.sp
            ),
            maxLines = 1,
            softWrap = false
        )
    }
}

private fun formatOrderStatus(rawStatus: String): Triple<String, Color, Color> {
    val normalized = rawStatus.trim().lowercase()
    val readable = rawStatus.trim().replace('_', ' ').uppercase().ifBlank { "CONFIRMADO" }
    return when {
        normalized.contains("cancel") || normalized.contains("rechaz") -> Triple(
            readable,
            Color(0xFFFFEBEE),
            Color(0xFFC62828)
        )
        normalized.contains("pendiente") || normalized.contains("revision") || normalized.contains("verific") -> Triple(
            readable,
            Color(0xFFFFF8E1),
            Color(0xFF9A6700)
        )
        else -> Triple(
            readable,
            Color(0xFFE8F5E9),
            Color(0xFF2E7D32)
        )
    }
}

private fun formatOrderDate(rawDate: String?): String {
    val clean = rawDate?.trim().orEmpty()
    if (clean.isBlank()) return ""
    // Handles ISO-8601 dates like 2026-11-24T12:30:00Z -> 24/11/2026
    val datePart = clean.substringBefore('T').substringBefore(' ')
    val parts = datePart.split('-')
    return if (parts.size == 3 && parts[0].length == 4) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } else {
        clean.take(16)
    }
}
