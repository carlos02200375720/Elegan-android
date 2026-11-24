package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun EstadoVacioCarrito(
    isCompact: Boolean,
    onExploreCatalog: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(if (isCompact) 20.dp else 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.widthIn(max = 440.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 86.dp else 100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3ECE4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RemoveShoppingCart,
                    contentDescription = null,
                    tint = EleganBurgundy,
                    modifier = Modifier.size(if (isCompact) 40.dp else 48.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Tu Carrito está Vacío",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal,
                    fontFamily = FontFamily.Serif,
                    fontSize = if (isCompact) 20.sp else 22.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Explora nuestro catálogo para descubrir prendas exclusivas, boinas, calzado y fragancias.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = if (isCompact) 8.dp else 24.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onExploreCatalog,
                colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("explore_catalog_from_empty_cart_btn")
            ) {
                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Explorar Catálogo", fontWeight = FontWeight.Bold)
            }
        }
    }
}
