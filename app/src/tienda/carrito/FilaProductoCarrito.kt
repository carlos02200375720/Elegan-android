package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .testTag("cart_item_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactCard = maxWidth < 330.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (isCompactCard) 10.dp else 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCompactCard) 68.dp else 80.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3ECE4))
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(if (isCompactCard) 10.dp else 14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = EleganCharcoal,
                            fontSize = if (isCompactCard) 13.sp else 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (item.selectedSize.isNotBlank()) {
                        Text(
                            text = "Talla: ${item.selectedSize}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.selectedColor.isNotBlank()) {
                        Text(
                            text = "Color: ${item.selectedColor}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = formatearPrecioRd(item.price),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganBurgundyDark,
                            fontSize = if (isCompactCard) 15.sp else 16.sp
                        )
                    )

                    if (item.shippingMethodName.isNotBlank()) {
                        Text(
                            text = "Envío: ${item.shippingMethodName}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Costo de envío: ${formatearEnvioRd(item.shippingPrice)}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                        if (item.shippingDeliveryTime.isNotBlank()) {
                            Text(
                                text = "Entrega: ${item.shippingDeliveryTime}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                        if (item.shippingOrigin.isNotBlank()) {
                            Text(
                                text = "Origen: ${item.shippingOrigin}",
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF6EFE9))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = onDecrease,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", tint = EleganBurgundy, modifier = Modifier.size(16.dp))
                        }

                        Text(
                            text = "${item.quantity}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        IconButton(
                            onClick = onIncrease,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", tint = EleganBurgundy, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(if (isCompactCard) 40.dp else 48.dp)
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
