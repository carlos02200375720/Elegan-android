package com.example.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun SelectorSeccionesPerfil(
    selectedSection: String,
    onSelectSection: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ProfileMenuItem(
            label = "Guardados",
            icon = Icons.Default.Bookmark,
            testTag = "profile_saved_menu",
            modifier = Modifier.weight(1f),
            onClick = { onSelectSection("saved") },
            selected = selectedSection == "saved"
        )
        ProfileMenuItem(
            label = "Pedidos",
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
            testTag = "profile_orders_menu",
            modifier = Modifier.weight(1f),
            onClick = { onSelectSection("orders") },
            selected = selectedSection == "orders"
        )
        ProfileMenuItem(
            label = "Entrega",
            icon = Icons.Default.LocationOn,
            testTag = "profile_delivery_menu",
            modifier = Modifier.weight(1f),
            onClick = { onSelectSection("delivery") },
            selected = selectedSection == "delivery"
        )
    }
}

@Composable
private fun ProfileMenuItem(
    label: String,
    icon: ImageVector,
    testTag: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) EleganBurgundy else Color(0xFF8A817A),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (selected) EleganBurgundy else EleganCharcoal,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        )
        if (selected) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.dp)
                    .background(EleganBurgundy, RoundedCornerShape(2.dp))
            )
        }
    }
}
