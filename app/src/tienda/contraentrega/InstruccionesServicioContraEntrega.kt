package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun TarjetaExplicacionContraEntrega(cardPad: Dp) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(cardPad),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(EleganBurgundy.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "¿Cómo funciona el Pago Contra Entrega?",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontFamily = FontFamily.Serif,
                        fontSize = 16.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Con este servicio ordenas hoy sin pagar por adelantado y cancelas el total de tu compra en efectivo al momento en que el mensajero te entregue tu pedido.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF555555),
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

@Composable
fun TarjetaPasosContraEntrega(amountFormatted: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Instrucciones del Servicio",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    )
                )
            }

            HorizontalDivider(color = Color(0xFFF0EAE4))

            FilaPasoInstruccion(
                stepNumber = "1",
                icon = Icons.Default.LocalShipping,
                title = "Preparación y despacho inmediato",
                description = "Al confirmar, reservamos tus prendas, realizamos el control de calidad y preparamos el envío exclusivo hacia tu dirección."
            )

            FilaPasoInstruccion(
                stepNumber = "2",
                icon = Icons.Default.PhoneInTalk,
                title = "Coordinación previa por llamada o mensaje",
                description = "Nuestro mensajero se comunicará contigo antes de llegar para confirmar tu disponibilidad y horario de recepción."
            )

            FilaPasoInstruccion(
                stepNumber = "3",
                icon = Icons.Default.Payments,
                title = "Pago en efectivo al recibir",
                description = "Entrega el monto total de RD\$ $amountFormatted al mensajero al recibir tu paquete. Te recomendamos tener el efectivo exacto listo."
            )
        }
    }
}

@Composable
private fun FilaPasoInstruccion(
    stepNumber: String,
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(EleganBurgundy),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        fontSize = 13.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF5F5F5F),
                    lineHeight = 18.sp
                )
            )
        }
    }
}
