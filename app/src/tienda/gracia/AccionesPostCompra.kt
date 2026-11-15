package com.example.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun AccionesPostCompra(
    isCompact: Boolean,
    onNavigateToInicio: () -> Unit,
    onNavigateToPerfil: () -> Unit,
    onNavigateToChats: () -> Unit
) {
    Button(
        onClick = onNavigateToInicio,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 48.dp else 52.dp)
            .testTag("gracia_keep_shopping_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EleganBurgundy,
            contentColor = Color.White
        )
    ) {
        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Seguir Comprando",
            fontWeight = FontWeight.Bold,
            fontSize = if (isCompact) 14.sp else 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onNavigateToPerfil,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 46.dp else 50.dp)
            .testTag("gracia_view_orders_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EleganCharcoal
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = SolidColor(Color(0xFFCCC3BA))
        )
    ) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Ver mis Pedidos",
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onNavigateToChats,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 46.dp else 50.dp)
            .testTag("gracia_contact_support_btn"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = EleganBurgundy
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
            brush = SolidColor(EleganBurgundy)
        )
    ) {
        Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Consultar con Elegan Concierge",
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
}
