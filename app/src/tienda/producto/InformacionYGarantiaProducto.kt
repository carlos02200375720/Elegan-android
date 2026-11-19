package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductJson
import com.example.data.model.ProductShippingMethod
import com.example.data.model.productColors
import com.example.data.model.productDescription
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.data.model.productSizes
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun BloqueDetalleProducto(
    product: ProductJson,
    selectedSize: String,
    onSelectSize: (String) -> Unit,
    selectedColor: String,
    onSelectColor: (String) -> Unit,
    quantity: Long,
    onDecreaseQuantity: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    shippingMethods: List<ProductShippingMethod> = emptyList(),
    selectedShippingId: String = "",
    onSelectShipping: (String) -> Unit = {}
) {
    val sizes = product.productSizes()
    val colors = product.productColors()

    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = product.productName(),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$${String.format("%.2f", product.productPrice())}",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = EleganBurgundy,
                fontSize = 20.sp
            ),
            modifier = Modifier.testTag("producto_price_text")
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFFE5DDD5))
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Descripción",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = product.productDescription(),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = Color(0xFF4A4A4A),
                lineHeight = 22.sp
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (sizes.isNotEmpty()) {
            SelectorOpcionesProducto(
                title = "Seleccionar Talla / Medida:",
                options = sizes,
                selectedOption = selectedSize,
                onSelect = onSelectSize,
                rowTestTag = "size_selector_row",
                chipTagPrefix = "size_chip_"
            )
        }

        if (colors.isNotEmpty()) {
            SelectorOpcionesProducto(
                title = "Seleccionar Color:",
                options = colors,
                selectedOption = selectedColor,
                onSelect = onSelectColor,
                rowTestTag = "color_selector_row",
                chipTagPrefix = "color_chip_"
            )
        }


        if (shippingMethods.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
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
                        .testTag("shipping_method_${method.id}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = method.name,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        )
                        Text(
                            text = if (method.price == 0.0) "GRATIS" else "$${String.format("%.2f", method.price)}",
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

        SelectorCantidadProducto(
            quantity = quantity,
            onDecrease = onDecreaseQuantity,
            onIncrease = onIncreaseQuantity
        )

        Spacer(modifier = Modifier.height(24.dp))
        TarjetaGarantiaProducto()
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun TarjetaGarantiaProducto() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Envío y embalaje confidencial de lujo",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EleganCharcoal
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Diamond,
                    contentDescription = null,
                    tint = EleganGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Autenticidad y confección artesanal certificada",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EleganCharcoal
                    )
                )
            }
        }
    }
}
