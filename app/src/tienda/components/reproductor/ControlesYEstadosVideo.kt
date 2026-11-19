package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganGold

@Composable
internal fun BotonesControlVideo(
    isPlaying: Boolean,
    showOverlayControls: Boolean,
    isCompleted: Boolean,
    testTagPrefix: String,
    onReplay: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !isPlaying || showOverlayControls || isCompleted,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (isCompleted) {
            IconButton(
                onClick = onReplay,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("${testTagPrefix}_replay_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Repetir video",
                    tint = Color.White,
                    modifier = Modifier.size(42.dp)
                )
            }
        } else if (!isPlaying) {
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("${testTagPrefix}_play_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir video",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else if (showOverlayControls) {
            IconButton(
                onClick = onPause,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("${testTagPrefix}_pause_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = "Pausar video",
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
internal fun VistaErrorVideo(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0EBE3))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Videocam,
            contentDescription = null,
            tint = EleganGold,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Text(
            text = "Video no disponible",
            color = Color.DarkGray,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
        Text(
            text = "El archivo de video local se está procesando.",
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}
