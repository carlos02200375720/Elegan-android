package com.example.ui

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun CargaComprobanteTransferencia(
    paymentProofUri: Uri?,
    isCompact: Boolean,
    onPickPaymentProof: () -> Unit,
    onRemovePaymentProof: () -> Unit
) {
    Text(
        text = "Evidencia del Pago (Comprobante) *",
        style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = FontWeight.Bold,
            color = EleganCharcoal
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (paymentProofUri == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (isCompact) 114.dp else 126.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFAF8F5))
                .border(1.5.dp, EleganBurgundy.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .clickable { onPickPaymentProof() }
                .padding(16.dp)
                .testTag("upload_payment_proof_btn"),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(EleganBurgundy.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Cargar imagen del comprobante",
                        tint = EleganBurgundy,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Subir comprobante",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganBurgundy,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Selecciona la foto de tu comprobante de transferencia para verificar tu pago",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Adjunta la imagen de evidencia del pago para habilitar el botón de confirmación.",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF7A7A7A)
            )
        )
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFFAF8F5),
            border = BorderStroke(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("payment_proof_preview_card")
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                AsyncImage(
                    model = paymentProofUri,
                    contentDescription = "Evidencia de pago cargada",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Comprobante cargado",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onPickPaymentProof,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy),
                            modifier = Modifier.testTag("change_payment_proof_btn")
                        ) {
                            Text("Cambiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = onRemovePaymentProof,
                            modifier = Modifier.testTag("remove_payment_proof_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quitar comprobante",
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
