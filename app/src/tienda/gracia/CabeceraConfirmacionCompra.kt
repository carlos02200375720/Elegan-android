package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganGold

@Composable
fun CabeceraConfirmacionCompra(
    isCompact: Boolean,
    isBankTransfer: Boolean
) {
    Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 12.dp))

    Box(
        modifier = Modifier
            .size(if (isCompact) 84.dp else 96.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        EleganGold.copy(alpha = 0.3f),
                        EleganBurgundy.copy(alpha = 0.15f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (isCompact) 62.dp else 72.dp)
                .clip(CircleShape)
                .background(EleganBurgundy),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Éxito",
                tint = Color.White,
                modifier = Modifier.size(if (isCompact) 38.dp else 44.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(if (isCompact) 14.dp else 18.dp))

    Text(
        text = "¡GRACIAS POR TU COMPRA!",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            color = EleganBurgundyDark,
            fontFamily = FontFamily.Serif,
            textAlign = TextAlign.Center,
            fontSize = if (isCompact) 22.sp else 26.sp,
            lineHeight = if (isCompact) 28.sp else 32.sp,
            letterSpacing = 1.sp
        )
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = if (isBankTransfer) {
            "Tu pedido ha sido registrado exitosamente. Su pago está siendo verificado con el comprobante de transferencia bancaria enviado."
        } else {
            "Tu pedido ha sido confirmado y registrado exitosamente en el sistema de Elegan."
        },
        style = MaterialTheme.typography.bodyMedium.copy(
            color = Color(0xFF555555),
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            fontSize = if (isCompact) 13.5.sp else 14.sp
        ),
        modifier = Modifier
            .padding(horizontal = if (isCompact) 4.dp else 8.dp)
            .testTag("gracia_status_message")
    )
}
