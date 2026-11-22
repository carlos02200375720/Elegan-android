package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductShippingMethod
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun SelectorOpcionesProducto(
    title: String,
    options: List<String>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    rowTestTag: String,
    chipTagPrefix: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = EleganCharcoal
        )
    )
    Spacer(modifier = Modifier.height(10.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(rowTestTag)
    ) {
        items(options) { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) EleganBurgundy else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) EleganBurgundy else Color(0xFFD3CAC0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("$chipTagPrefix$option"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isSelected) Color.White else EleganCharcoal,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
}

@Composable
fun SelectorEnvioProducto(
    shippingMethods: List<ProductShippingMethod>,
    selectedShippingId: String,
    onSelectShipping: (String) -> Unit,
    testTagPrefix: String = "shipping_method_"
) {
    if (shippingMethods.isEmpty()) return

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Selecciona el método de envío",
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = EleganCharcoal
        )
    )
    Spacer(modifier = Modifier.height(10.dp))
    shippingMethods.forEach { method ->
        val selected = method.id == selectedShippingId
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) Color(0xFFFFF7F7) else Color.White)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) EleganBurgundy else Color(0xFFE5DDD5),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelectShipping(method.id) }
                .padding(12.dp)
                .testTag("$testTagPrefix${method.id}")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = method.name,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                )
                Text(
                    text = formatearEnvioRd(method.price),
                    fontWeight = FontWeight.Bold,
                    color = EleganBurgundy
                )
            }
            Text(
                text = "Entrega: ${method.deliveryTime}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
            Text(
                text = "Origen: ${method.origin}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun SelectorCantidadProducto(
    quantity: Long,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    testTagPrefix: String = "quantity_"
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Cantidad:",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD3CAC0), RoundedCornerShape(8.dp))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            IconButton(
                onClick = onDecrease,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTagPrefix}decrease_btn")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Menos", tint = EleganBurgundy)
            }
            Text(
                text = "$quantity",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .testTag("${testTagPrefix}value_text")
            )
            IconButton(
                onClick = onIncrease,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("${testTagPrefix}increase_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Más", tint = EleganBurgundy)
            }
        }
    }
}
