package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun BoxScope.CabeceraSuperiorPerfil(
    cartItemCount: Long,
    onNavigateToCategoryMenu: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(Color.White)
    )

    Row(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .height(48.dp)
            .background(Color.Transparent)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onNavigateToCategoryMenu,
            modifier = Modifier.testTag("perfil_menu_categories_button")
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Menú de Categorías",
                tint = EleganCharcoal
            )
        }

        IconButton(
            onClick = onNavigateToCart,
            modifier = Modifier.testTag("perfil_cart_button")
        ) {
            if (cartItemCount > 0L) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = EleganBurgundy,
                            contentColor = Color.White
                        ) {
                            Text("$cartItemCount", fontSize = 10.sp)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = "Carrito",
                        tint = EleganCharcoal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Carrito",
                    tint = EleganCharcoal,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
