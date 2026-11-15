package com.example.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun BarraEntradaMensajeChat(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    pendingMediaUri: Uri?,
    pendingMediaType: String,
    isKeyboardOpen: Boolean,
    onOpenMediaPicker: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        EleganBottomBarShadow()
        Surface(
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val isCompactFooter = maxWidth < 350.dp
                val isWideFooter = maxWidth >= 600.dp
                val footerHorizontalPad = when {
                    isCompactFooter -> 10.dp
                    isWideFooter -> 24.dp
                    else -> 14.dp
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 760.dp)
                        .then(
                            if (!isKeyboardOpen) {
                                Modifier.navigationBarsPadding()
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (pendingMediaUri != null) {
                        VistaPreviaAdjuntoChat(
                            pendingMediaUri = pendingMediaUri,
                            pendingMediaType = pendingMediaType,
                            horizontalPad = footerHorizontalPad,
                            onRemoveAttachment = onRemoveAttachment
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isKeyboardOpen) {
                                    Modifier
                                        .height(54.dp)
                                        .padding(
                                            start = footerHorizontalPad,
                                            end = footerHorizontalPad,
                                            top = 6.dp,
                                            bottom = 6.dp
                                        )
                                } else {
                                    Modifier
                                        .height(62.dp)
                                        .padding(horizontal = footerHorizontalPad, vertical = 10.dp)
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(21.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFD6CDC4), RoundedCornerShape(21.dp))
                                .padding(start = 4.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenMediaPicker,
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("chat_attach_media_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Adjuntar imagen o video de la galería",
                                    tint = EleganBurgundy,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            BasicTextField(
                                value = inputText,
                                onValueChange = onInputTextChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_input_field"),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 14.sp,
                                    color = EleganCharcoal
                                ),
                                cursorBrush = SolidColor(EleganBurgundy),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (inputText.isEmpty()) {
                                            Text(
                                                text = "Escribe tu consulta aquí...",
                                                fontSize = 13.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EleganBurgundy)
                                .clickable(onClick = onSend)
                                .testTag("chat_send_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
