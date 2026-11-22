package com.example.ui

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductJson
import com.example.data.model.productDescription
import com.example.data.model.productName
import com.example.data.model.productPrice
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun BloqueDetalleProducto(
    product: ProductJson
) {

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
            text = formatearPrecioRd(product.productPrice()),
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
    }
}

@Composable
fun TarjetaGarantiaProducto(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
