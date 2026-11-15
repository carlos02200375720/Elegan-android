package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun SeccionResumenArticulos(
    cartItems: List<CartItem>,
    subtotal: Double,
    discount: Double,
    shipping: Double,
    total: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null, tint = EleganBurgundy)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "2. Resumen de Artículos (${cartItems.sumOf { it.quantity }})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        for (item in cartItems) {
            FilaArticuloResumen(item = item)
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color(0xFFE5DDD5))
        Spacer(modifier = Modifier.height(10.dp))

        SummaryRow(label = "Subtotal", value = "$${String.format("%.2f", subtotal)}")
        if (discount > 0.0) {
            SummaryRow(
                label = "Descuento",
                value = "-$${String.format("%.2f", discount)}",
                valueColor = Color(0xFF1E7E34)
            )
        }
        SummaryRow(
            label = "Envío",
            value = if (shipping == 0.0) "GRATIS" else "$${String.format("%.2f", shipping)}"
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Total Final", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(
                text = "$${String.format("%.2f", total)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = EleganBurgundyDark
                )
            )
        }
    }
}

@Composable
private fun FilaArticuloResumen(item: CartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF3ECE4)),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = item.name,
                    tint = EleganBurgundy.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = EleganCharcoal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val details = buildList {
                add("Cant: ${item.quantity}")
                if (item.selectedSize.isNotBlank()) add("Talla: ${item.selectedSize}")
                if (item.selectedColor.isNotBlank()) add(item.selectedColor)
            }.joinToString(" • ")
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "$${String.format("%.2f", item.price * item.quantity)}",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganBurgundyDark
            )
        )
    }
}
