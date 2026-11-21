package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Precision
import com.example.data.model.AnnouncementJson
import com.example.data.model.announcementBadge
import com.example.data.model.announcementId
import com.example.data.model.announcementImageUrl
import com.example.data.model.announcementMessage
import com.example.data.model.announcementTitle
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
internal fun SeccionAnunciosInicio(
    announcements: List<AnnouncementJson>,
    onAnnouncementClick: (AnnouncementJson) -> Unit,
    modifier: Modifier = Modifier
) {
    if (announcements.isEmpty()) return

    val context = LocalContext.current
    val cardShape = remember { RoundedCornerShape(16.dp) }
    val overlayShadow = remember {
        Shadow(
            color = Color.Black.copy(alpha = 0.6f),
            offset = Offset(0f, 1.5f),
            blurRadius = 5f
        )
    }
    val fallbackBrush = remember {
        Brush.verticalGradient(colors = listOf(EleganBurgundyDark, EleganCharcoal))
    }
    val scrimBrush = remember {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.42f to Color.Transparent,
            0.74f to Color.Black.copy(alpha = 0.44f),
            1f to Color.Black.copy(alpha = 0.80f)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .testTag("inicio_announcements_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Campaign,
                contentDescription = "Anuncios de la boutique",
                tint = EleganBurgundy,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Anuncios y Novedades",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = EleganCharcoal,
                    fontFamily = FontFamily.Serif
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            modifier = Modifier.testTag("inicio_announcements_row"),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ) {
            itemsIndexed(
                items = announcements,
                key = { idx, item -> item.announcementId().ifBlank { "ann_$idx" } }
            ) { idx, item ->
                val title = item.announcementTitle()
                val message = item.announcementMessage()
                val imageUrl = item.announcementImageUrl()
                val badge = item.announcementBadge().ifBlank { "COMUNICADO" }
                val imageRequest = remember(imageUrl, context) {
                    imageUrl.takeIf { it.isNotBlank() }?.let { url ->
                        ImageRequest.Builder(context)
                            .data(url)
                            .size(720, 520)
                            .precision(Precision.INEXACT)
                            .allowRgb565(true)
                            .crossfade(false)
                            .build()
                    }
                }

                Card(
                    modifier = Modifier
                        .width(292.dp)
                        .height(206.dp)
                        .clip(cardShape)
                        .clickable { onAnnouncementClick(item) }
                        .testTag("announcement_card_$idx"),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, Color(0xFFE7DFD5)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(fallbackBrush)
                    ) {
                        if (imageRequest != null) {
                            AsyncImage(
                                model = imageRequest,
                                contentDescription = title.ifBlank { "Anuncio Elegan" },
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("announcement_image_$idx")
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scrimBrush)
                        )

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EleganBurgundy.copy(alpha = 0.86f),
                            border = BorderStroke(0.8.dp, EleganGold.copy(alpha = 0.7f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = badge.uppercase(),
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.9.sp,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .background(Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("announcement_text_container_$idx")
                        ) {
                            if (title.isNotBlank()) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        shadow = overlayShadow
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (message.isNotBlank()) {
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color.White.copy(alpha = 0.92f),
                                        lineHeight = 16.sp,
                                        shadow = overlayShadow
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
