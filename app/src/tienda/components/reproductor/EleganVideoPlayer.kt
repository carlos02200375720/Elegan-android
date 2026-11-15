package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.EleganGold

/**
 * Reproductor de video optimizado para la galería de Elegan.
 * Escala el video con Matrix (Center-Crop) para ocupar el 100% del ancho y alto
 * del contenedor sin dejar espacios vacíos, y mantiene la portada visible hasta
 * que el primer fotograma real del video sea dibujado para evitar fondos negros al inicio.
 */
@Composable
fun EleganVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    posterUrl: String? = null,
    aspectRatio: Float = 1f,
    autoPlay: Boolean = false,
    isFocused: Boolean = false,
    showControls: Boolean = true,
    testTagPrefix: String = "elegan_video"
) {
    val context = LocalContext.current
    val state = rememberEstadoReproductorVideo(
        videoUrl = videoUrl,
        autoPlay = autoPlay,
        isFocused = isFocused
    )

    EfectoFocoYControlesVideo(
        state = state,
        isFocused = isFocused,
        videoUrl = videoUrl,
        posterUrl = posterUrl
    )

    CicloVidaMediaPlayer(
        videoUrl = videoUrl,
        isFocused = isFocused,
        state = state
    )

    val effectivePoster = posterUrl?.takeIf { it.isNotBlank() } ?: state.extractedPosterUrl
    val posterRequest = remember(effectivePoster, context) {
        construirPeticionPortadaVideo(context, effectivePoster)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RectangleShape)
            .background(Color(0xFFF0EBE3))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { state.alPulsarContenedor() }
            .testTag("${testTagPrefix}_container"),
        contentAlignment = Alignment.Center
    ) {
        if (state.shouldLoadPlayer && videoUrl.isNotBlank() && !state.hasError) {
            SuperficieTextureVideo(
                state = state,
                isFocused = isFocused
            )
        }

        if (!state.isFirstFrameRendered && !state.hasError && posterRequest != null) {
            AsyncImage(
                model = posterRequest,
                contentDescription = "Portada del video",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (state.isPlaying && !state.isPrepared && !state.hasError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = EleganGold,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        BotonesControlVideo(
            isPlaying = state.isPlaying,
            showOverlayControls = state.showOverlayControls,
            isCompleted = state.isCompleted,
            testTagPrefix = testTagPrefix,
            onReplay = { state.repetirVideo() },
            onPlay = { state.iniciarReproduccion() },
            onPause = { state.pausarReproduccion() },
            modifier = Modifier.align(Alignment.Center)
        )

        if (state.hasError) {
            VistaErrorVideo()
        }
    }
}
