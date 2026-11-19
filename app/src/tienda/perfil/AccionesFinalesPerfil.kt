package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganGold

@Composable
internal fun TarjetaSoporteVipPerfil(
    horizontalPad: Dp,
    isCompact: Boolean,
    onNavigateToChats: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = horizontalPad),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF26292E)),
        onClick = onNavigateToChats
    ) {
        Row(
            modifier = Modifier.padding(if (isCompact) 14.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = EleganGold)
            Spacer(modifier = Modifier.width(if (isCompact) 10.dp else 14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Soporte & Asesoría VIP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Chatea en directo con Elegan Concierge",
                    color = Color(0xFFBBBBBB),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = EleganGold)
        }
    }
}

@Composable
internal fun BotonCerrarSesionPerfil(
    horizontalPad: Dp,
    isAuthenticated: Boolean = true,
    onRequireAuth: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Spacer(modifier = Modifier.height(10.dp))
    TextButton(
        onClick = if (isAuthenticated) onSignOut else onRequireAuth,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .padding(horizontal = horizontalPad)
            .height(48.dp)
            .testTag(if (isAuthenticated) "sign_out_button" else "guest_register_button"),
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (isAuthenticated) Color(0xFFB00020) else EleganBurgundy
        )
    ) {
        Icon(
            imageVector = if (isAuthenticated) {
                Icons.AutoMirrored.Filled.ExitToApp
            } else {
                Icons.Default.PersonAdd
            },
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isAuthenticated) "Cerrar Sesión" else "Crear Cuenta o Iniciar Sesión",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
    Spacer(modifier = Modifier.height(30.dp))
}
