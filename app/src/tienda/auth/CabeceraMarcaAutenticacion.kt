package com.example.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark

@Composable
internal fun CabeceraMarcaAutenticacion(
    logoSize: Dp,
    isCompact: Boolean,
    isShortScreen: Boolean
) {
    Box(
        modifier = Modifier
            .size(logoSize)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_fg),
            contentDescription = "Logo Elegan",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .graphicsLayer(scaleX = 1.35f, scaleY = 1.35f)
        )
    }

    Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 10.dp else 14.dp))

    Text(
        text = "ELEGAN",
        style = MaterialTheme.typography.headlineMedium.copy(
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            fontSize = if (isCompact) 24.sp else 28.sp,
            letterSpacing = if (isCompact) 3.sp else 4.sp,
            color = EleganBurgundyDark
        )
    )

    Text(
        text = "Moda & Accesorios Masculinos de Lujo",
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = if (isCompact) 11.5.sp else 12.sp,
            color = Color(0xFF777777),
            letterSpacing = if (isCompact) 0.5.sp else 1.sp
        )
    )
}

@Composable
internal fun SelectorPestanasAutenticacion(
    selectedTab: Int,
    isCompact: Boolean,
    onSelectTab: (Int) -> Unit
) {
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.White,
        contentColor = EleganBurgundy,
        divider = { HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp) },
        indicator = { tabPositions ->
            if (selectedTab < tabPositions.size) {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = EleganBurgundy,
                    height = 3.dp
                )
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf("Iniciar Sesión", "Crear Cuenta").forEachIndexed { index, title ->
            Tab(
                selected = selectedTab == index,
                onClick = { onSelectTab(index) },
                text = {
                    Text(
                        text = title,
                        fontSize = if (isCompact) 13.sp else 14.sp,
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}
