package com.example.ui.components

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.observability.MonitorErrores
import kotlinx.coroutines.delay

@Stable
internal class EstadoReproductorVideo(
    initialAutoPlay: Boolean,
    @Suppress("UNUSED_PARAMETER") initialFocused: Boolean = false
) {
    var isPlaying by mutableStateOf(initialAutoPlay)
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
    var retryToken by mutableIntStateOf(0)

    var textureViewRef by mutableStateOf<TextureView?>(null)
    var playerRef by mutableStateOf<ExoPlayer?>(null)

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
        val player = playerRef ?: return
        if (!isPrepared || hasError || isDisposed) return
        try {
            player.play()
            hasStarted = true
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("EleganVideoPlayer", "No se pudo iniciar la reproducción de video", e)
        }
    }

    fun reiniciarYReproducirSiPreparado() {
        val player = playerRef ?: return
        if (!isPrepared || hasError || isDisposed) return
        try {
            player.seekTo(0L)
            player.play()
            hasStarted = true
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("EleganVideoPlayer", "No se pudo reiniciar la reproducción de video", e)
        }
    }

    fun pausarSiEsPosible() {
        val player = playerRef ?: return
        if (hasError || isDisposed) return
        try {
            if (player.isPlaying || player.playWhenReady) {
                player.pause()
            }
            hasStarted = false
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("EleganVideoPlayer", "No se pudo pausar la reproducción de video", e)
        }
    }

    fun pausarPorSegundoPlano() {
        val activoEnPlayer = runCatching {
            val player = playerRef
            player != null && !isDisposed && (player.isPlaying || player.playWhenReady)
        }.getOrDefault(false)

        if (isPlaying || hasStarted || activoEnPlayer) {
            isPlaying = false
            hasStarted = false
            showOverlayControls = true
            pausarSiEsPosible()
        }
    }

    /**
     * Libera el reproductor cuando otro video toma el protagonismo o al salir de foco,
     * devolviendo inmediatamente los decodificadores hardware (`MediaCodec`).
     */
    fun liberarReproductor() {
        isPlaying = false
        hasStarted = false
        showOverlayControls = true
        pausarSiEsPosible()
        shouldLoadPlayer = false
        isFirstFrameRendered = false
        GestorVideoActivo.liberarSiEsActivo(this)
    }

    internal fun liberarPorReemplazo() {
        isPlaying = false
        hasStarted = false
        showOverlayControls = true
        pausarSiEsPosible()
        shouldLoadPlayer = false
        isFirstFrameRendered = false
    }

    fun repetirVideo() {
        GestorVideoActivo.reclamarReproduccion(this)
        isCompleted = false
        shouldLoadPlayer = true
        isPlaying = true
        showOverlayControls = false
        reiniciarYReproducirSiPreparado()
    }

    fun iniciarReproduccion() {
        GestorVideoActivo.reclamarReproduccion(this)
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

    fun reintentarCarga() {
        GestorVideoActivo.reclamarReproduccion(this)
        hasError = false
        isPrepared = false
        isCompleted = false
        hasStarted = false
        isFirstFrameRendered = false
        shouldLoadPlayer = true
        isPlaying = true
        showOverlayControls = false
        retryToken += 1
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
    autoPlay: Boolean,
    videoUrl: String
) {
    var wasFocused by remember(videoUrl) { mutableStateOf(false) }

    LaunchedEffect(isFocused, autoPlay) {
        if (isFocused) {
            if (autoPlay) {
                GestorVideoActivo.reclamarReproduccion(state)
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
            }
        } else if (wasFocused || autoPlay) {
            // Libera el ExoPlayer al salir del foco incluso cuando autoPlay == false
            // para evitar acumular decodificadores MediaCodec al desplazarse por miles de tarjetas.
            state.liberarReproductor()
        }
        wasFocused = isFocused
    }

    LaunchedEffect(state.isPlaying, state.showOverlayControls) {
        if (state.isPlaying && state.showOverlayControls) {
            delay(2800)
            state.showOverlayControls = false
        }
    }
}
