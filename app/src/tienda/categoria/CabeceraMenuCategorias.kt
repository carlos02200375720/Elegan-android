package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun CabeceraMenuCategorias(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isCompactHeader = maxWidth < 360.dp
                val isWideHeader = maxWidth >= 600.dp

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (isCompactHeader) 2.dp else 4.dp,
                            end = if (isWideHeader) 20.dp else 12.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("menu_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = EleganCharcoal
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isCompactHeader) 2.dp else 4.dp))

                    Box(
                        modifier = Modifier
                            .size(if (isCompactHeader) 34.dp else 38.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_elegan_gentleman),
                            contentDescription = "Logo Elegan",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(if (isCompactHeader) 6.dp else 8.dp))

                    Text(
                        text = "Categorías",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal,
                            fontFamily = FontFamily.Serif,
                            fontSize = if (isCompactHeader) 16.sp else 18.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        EleganTopBarShadow()
    }
}
