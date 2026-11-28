package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.EleganBottomBarShadow
import com.example.ui.theme.EleganBurgundy

@Composable
internal fun EleganBottomBar(
    currentRoute: RutaApp,
    cartItemCount: Long,
    onNavigateToInicio: () -> Unit,
    onNavigateToCatalogo: () -> Unit,
    onNavigateToCarrito: () -> Unit,
    onNavigateToChats: () -> Unit,
    onNavigateToPerfil: () -> Unit
) {
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = EleganBurgundy,
        unselectedIconColor = Color.Black,
        indicatorColor = Color.Transparent
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        EleganBottomBarShadow()

        Surface(
            color = Color.White,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp),
                    containerColor = Color.White,
                    tonalElevation = 0.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0)
                ) {
                    NavigationBarItem(
                        selected = currentRoute == RutaApp.Inicio,
                        onClick = onNavigateToInicio,
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = stringResource(R.string.nav_inicio),
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_inicio")
                    )

                    NavigationBarItem(
                        selected = currentRoute == RutaApp.Catalogo,
                        onClick = onNavigateToCatalogo,
                        icon = {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = stringResource(R.string.nav_catalogo),
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_catalogo")
                    )

                    NavigationBarItem(
                        selected = currentRoute == RutaApp.Carrito,
                        onClick = onNavigateToCarrito,
                        icon = {
                            if (cartItemCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = EleganBurgundy,
                                            contentColor = Color.White
                                        ) {
                                            Text("$cartItemCount", fontSize = 9.sp)
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingBag,
                                        contentDescription = stringResource(R.string.nav_carrito),
                                        modifier = Modifier.size(25.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.ShoppingBag,
                                    contentDescription = stringResource(R.string.nav_carrito),
                                    modifier = Modifier.size(25.dp)
                                )
                            }
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_carrito")
                    )

                    NavigationBarItem(
                        selected = currentRoute == RutaApp.Chats,
                        onClick = onNavigateToChats,
                        icon = {
                            Icon(
                                Icons.Default.ChatBubbleOutline,
                                contentDescription = stringResource(R.string.nav_soporte),
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_chats")
                    )

                    NavigationBarItem(
                        selected = currentRoute == RutaApp.Perfil,
                        onClick = onNavigateToPerfil,
                        icon = {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = stringResource(R.string.nav_perfil),
                                modifier = Modifier.size(25.dp)
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_perfil")
                    )
                }
            }
        }
    }
}
