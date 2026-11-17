package com.example.ui.components

import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Stable
internal class EstadoReproductorVideo(
    initialAutoPlay: Boolean,
    initialFocused: Boolean
) {
    var isPlaying by mutableStateOf(initialAutoPlay || initialFocused)
    var isPrepared by mutableStateOf(false)
    var hasStarted by mutableStateOf(false)
    var isCompleted by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    var hasError by mutableStateOf(false)
    var isDisposed by mutableStateOf(false)
    var showOverlayControls by mutableStateOf(true)
    var isFirstFrameRendered by mutableStateOf(false)
    var shouldLoadPlayer by mutableStateOf(false)

    var videoWidth by mutableIntStateOf(0)
    var videoHeight by mutableIntStateOf(0)

    var textureViewRef by mutableStateOf<TextureView?>(null)
    var surfaceRef by mutableStateOf<Surface?>(null)
    var mediaPlayerRef by mutableStateOf<MediaPlayer?>(null)
    var extractedPosterUrl by mutableStateOf<String?>(null)

    fun alPulsarContenedor() {
        if (isPlaying) {
            showOverlayControls = !showOverlayControls
        } else if (isCompleted) {
            repetirVideo()
        } else {
            iniciarReproduccion()
        }
    }

    fun iniciarSiPreparado() {
        val mp = mediaPlayerRef ?: return
        if (!isPrepared || hasError || isDisposed) return
        try {
            if (!mp.isPlaying) {
                mp.start()
            }
            hasStarted = true
        } catch (_: Exception) {}
    }

    fun reiniciarYReproducirSiPreparado() {
        val mp = mediaPlayerRef ?: return
        if (!isPrepared || hasError || isDisposed) return
        try {
            mp.seekTo(0)
            if (!mp.isPlaying) {
                mp.start()
            }
            hasStarted = true
        } catch (_: Exception) {}
    }

    fun pausarSiEsPosible() {
        val mp = mediaPlayerRef ?: return
        if (!isPrepared || hasError || !hasStarted || isDisposed) return
        try {
            if (mp.isPlaying) {
                mp.pause()
            }
        } catch (_: Exception) {}
    }

    fun repetirVideo() {
        isCompleted = false
        shouldLoadPlayer = true
        isPlaying = true
        showOverlayControls = false
        reiniciarYReproducirSiPreparado()
    }

    fun iniciarReproduccion() {
        shouldLoadPlayer = true
        isPlaying = true
        showOverlayControls = false
        iniciarSiPreparado()
    }

    fun pausarReproduccion() {
        isPlaying = false
        showOverlayControls = true
        pausarSiEsPosible()
    }
}

@Composable
internal fun rememberEstadoReproductorVideo(
    videoUrl: String,
    autoPlay: Boolean,
    isFocused: Boolean
): EstadoReproductorVideo {
    return remember(videoUrl) {
        EstadoReproductorVideo(
            initialAutoPlay = autoPlay,
            initialFocused = isFocused
        )
    }
}

@Composable
internal fun EfectoFocoYControlesVideo(
    state: EstadoReproductorVideo,
    isFocused: Boolean,
    videoUrl: String,
    posterUrl: String?
) {
    val context = LocalContext.current

    LaunchedEffect(isFocused) {
        if (isFocused) {
            state.shouldLoadPlayer = true
            state.isPlaying = true
            state.showOverlayControls = false
            if (state.isPrepared && !state.hasError) {
                if (state.isCompleted) {
                    state.isCompleted = false
                    state.reiniciarYReproducirSiPreparado()
                } else {
                    state.iniciarSiPreparado()
                }
            }
        } else {
            state.isPlaying = false
            state.pausarSiEsPosible()
            state.shouldLoadPlayer = false
        }
    }

    LaunchedEffect(videoUrl, posterUrl, state.shouldLoadPlayer) {
        state.isFirstFrameRendered = false
        if (state.shouldLoadPlayer && posterUrl.isNullOrBlank() && videoUrl.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val thumb = extractVideoPosterLocally(context, videoUrl)
                if (thumb != null) {
                    state.extractedPosterUrl = thumb
                }
            }
        }
    }

    LaunchedEffect(state.isPlaying, state.showOverlayControls) {
        if (state.isPlaying && state.showOverlayControls) {
            delay(2800)
            state.showOverlayControls = false
        }
    }
}
