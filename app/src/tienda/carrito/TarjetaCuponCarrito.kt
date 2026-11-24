package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun TarjetaCuponCarrito(
    couponCode: String,
    onCouponCodeChange: (String) -> Unit,
    discountAmount: Double,
    isCompact: Boolean,
    isApplying: Boolean = false,
    onApplyCoupon: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isUltraCompact = maxWidth < 310.dp
            val cardPad = when {
                isUltraCompact -> 10.dp
                isCompact -> 12.dp
                else -> 16.dp
            }

            Column(modifier = Modifier.padding(cardPad)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        tint = EleganGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Cupón de Descuento",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = couponCode,
                        onValueChange = { onCouponCodeChange(it.uppercase()) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("coupon_input"),
                        placeholder = {
                            Text(
                                text = "Ej: ELEGAN10",
                                fontSize = if (isUltraCompact) 12.sp else 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            cursorColor = EleganBurgundy,
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE),
                            focusedPlaceholderColor = Color(0xFF7A746E),
                            unfocusedPlaceholderColor = Color(0xFF7A746E),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
                    Button(
                        onClick = onApplyCoupon,
                        enabled = !isApplying,
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(
                            horizontal = when {
                                isUltraCompact -> 10.dp
                                isCompact -> 12.dp
                                else -> 16.dp
                            },
                            vertical = 10.dp
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("apply_coupon_btn")
                    ) {
                        Text(
                            text = if (isApplying) "Validando..." else "Aplicar",
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isUltraCompact) 13.sp else 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (discountAmount > 0.0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "✓ Descuento de ${formatearPrecioRd(discountAmount)} aplicado",
                        color = Color(0xFF1E7E34),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
