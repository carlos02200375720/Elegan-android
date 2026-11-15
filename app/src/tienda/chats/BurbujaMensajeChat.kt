package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SupportMessage
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun BurbujaMensajeChat(
    msg: SupportMessage,
    isCompact: Boolean,
    maxBubbleWidth: Dp
) {
    val isUser = msg.sender == "user" || msg.sender == "client"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(if (isCompact) 30.dp else 32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E323A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SupportAgent,
                    contentDescription = null,
                    tint = EleganGold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(if (isCompact) 6.dp else 8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = maxBubbleWidth)
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                color = if (isUser) EleganBurgundy else Color.White,
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = if (msg.mediaUrl.isNotBlank() && msg.text.isBlank()) 6.dp else 12.dp,
                        vertical = if (msg.mediaUrl.isNotBlank() && msg.text.isBlank()) 6.dp else 10.dp
                    )
                ) {
                    if (!isUser) {
                        Text(
                            text = msg.senderName.ifBlank { "Concierge Elegan" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    if (msg.mediaUrl.isNotBlank()) {
                        if (msg.mediaType == "video") {
                            EleganVideoPlayer(
                                videoUrl = msg.mediaUrl,
                                aspectRatio = 1.25f,
                                autoPlay = false,
                                isFocused = false,
                                showControls = true,
                                testTagPrefix = "chat_video_${msg.id}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        } else {
                            AsyncImage(
                                model = msg.mediaUrl,
                                contentDescription = "Imagen enviada",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .aspectRatio(1.25f)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        if (msg.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }

                    if (msg.text.isNotBlank()) {
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isUser) Color.White else EleganCharcoal,
                                lineHeight = 20.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
