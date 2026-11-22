package com.example.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CartItem
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun BannerEstadoTransferencia() {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = EleganGold.copy(alpha = 0.14f),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gracia_bank_verification_banner")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.HourglassTop,
                contentDescription = null,
                tint = EleganBurgundy,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Estado del Pago",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
                Text(
                    text = "Su pago está siendo verificado",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundyDark
                    )
                )
            }
        }
    }
}

@Composable
fun TarjetaComprobantePagoConfirmado(paymentProofUri: String) {
    val parsedModel = remember(paymentProofUri) { Uri.parse(paymentProofUri) }
    var showFullScreenProof by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gracia_payment_proof_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.gracia_payment_proof_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 240.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .border(1.dp, Color(0xFFEDE6DF), RoundedCornerShape(10.dp))
                    .clickable { showFullScreenProof = true },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = parsedModel,
                    contentDescription = stringResource(R.string.gracia_payment_proof_cd),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp, max = 240.dp)
                        .testTag("gracia_payment_proof_image")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.gracia_payment_proof_tap_hint),
                style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
            )
        }
    }

    if (showFullScreenProof) {
        Dialog(
            onDismissRequest = { showFullScreenProof = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable { showFullScreenProof = false }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = parsedModel,
                    contentDescription = stringResource(R.string.gracia_payment_proof_cd),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { showFullScreenProof = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("gracia_payment_proof_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.gracia_payment_proof_close_cd),
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ResumenProductosPedidoConfirmado(
    purchasedItems: List<CartItem>,
    totalPaid: Double,
    subtotal: Double = purchasedItems.sumOf { it.price * it.quantity },
    discount: Double = 0.0,
    shippingCost: Double = purchasedItems.sumOf { it.shippingPrice }
) {
    Spacer(modifier = Modifier.height(14.dp))
    HorizontalDivider(color = Color(0xFFF0EAE4))
    Spacer(modifier = Modifier.height(14.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.ShoppingBag,
            contentDescription = null,
            tint = EleganBurgundy,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Resumen del Producto (${purchasedItems.sumOf { it.quantity }})",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    for (item in purchasedItems) {
        FilaProductoConfirmado(item = item)
    }

    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = Color(0xFFF0EAE4))
    Spacer(modifier = Modifier.height(8.dp))

    SummaryRow(label = "Subtotal", value = formatearPrecioRd(subtotal))
    if (discount > 0.0) {
        SummaryRow(
            label = "Descuento",
            value = formatearDescuentoRd(discount),
            valueColor = Color(0xFF1E7E34)
        )
    }
    SummaryRow(
        label = "Envío",
        value = formatearEnvioRd(shippingCost),
        valueColor = if (shippingCost <= 0.0) Color(0xFF1E7E34) else EleganCharcoal
    )

    Spacer(modifier = Modifier.height(6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Total Pagado",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganCharcoal
            )
        )
        Text(
            text = formatearPrecioRd(totalPaid),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = EleganBurgundyDark
            )
        )
    }
}

@Composable
private fun FilaProductoConfirmado(item: CartItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
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
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

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
            val meta = buildList {
                add("Cant: ${item.quantity}")
                if (item.selectedSize.isNotBlank()) add("Talla: ${item.selectedSize}")
                if (item.selectedColor.isNotBlank()) add(item.selectedColor)
            }.joinToString(" • ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
            if (item.shippingMethodName.isNotBlank()) {
                Text(
                    text = "Envío: ${item.shippingMethodName} (${formatearEnvioRd(item.shippingPrice)})",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF555555)),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatearPrecioRd(item.price * item.quantity),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = EleganBurgundyDark
            )
        )
    }
}
