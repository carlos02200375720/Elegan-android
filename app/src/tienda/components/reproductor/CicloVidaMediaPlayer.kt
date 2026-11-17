package com.example.ui.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun CicloVidaMediaPlayer(
    videoUrl: String,
    isFocused: Boolean,
    state: EstadoReproductorVideo
) {
    val context = LocalContext.current

    DisposableEffect(videoUrl, state.shouldLoadPlayer) {
        if (!state.shouldLoadPlayer) {
            onDispose {}
        } else {
            state.isDisposed = false
            state.isPrepared = false
            state.hasStarted = false
            state.isCompleted = false
            state.hasError = false
            state.isFirstFrameRendered = false
            state.videoWidth = 0
            state.videoHeight = 0

            val disposed = AtomicBoolean(false)
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { player ->
                    if (disposed.get() || state.isDisposed) return@setOnPreparedListener
                    state.isPrepared = true
                    state.hasError = false
                    runCatching { player.isLooping = true }
                    val w = player.videoWidth
                    val h = player.videoHeight
                    if (w > 0 && h > 0) {
                        state.videoWidth = w
                        state.videoHeight = h
                        ajustarTransformacionVideo(state.textureViewRef, w, h)
                    }
                    if (state.isMuted) {
                        player.setVolume(0f, 0f)
                    } else {
                        player.setVolume(1f, 1f)
                    }
                    if (state.isPlaying || isFocused) {
                        state.isPlaying = true
                        state.iniciarSiPreparado()
                    }
                }
                setOnVideoSizeChangedListener { _, w, h ->
                    if (disposed.get()) return@setOnVideoSizeChangedListener
                    if (w > 0 && h > 0) {
                        state.videoWidth = w
                        state.videoHeight = h
                        ajustarTransformacionVideo(state.textureViewRef, w, h)
                    }
                }
                setOnCompletionListener {
                    if (disposed.get()) return@setOnCompletionListener
                    state.hasStarted = false
                    state.isCompleted = true
                    state.isPlaying = false
                    state.showOverlayControls = true
                }
                setOnErrorListener { _, _, _ ->
                    if (!disposed.get()) {
                        state.isPrepared = false
                        state.hasStarted = false
                        state.hasError = true
                        state.isPlaying = false
                    }
                    true
                }
            }
            state.mediaPlayerRef = mp

            state.surfaceRef?.let { surf ->
                try {
                    mp.setSurface(surf)
                } catch (_: Exception) {}
            }

            if (videoUrl.isNotBlank()) {
                val appContext = context.applicationContext
                videoReleaseScope.launch {
                    var failed = false
                    synchronized(mp) {
                        if (disposed.get()) return@launch
                        try {
                            mp.setDataSource(appContext, Uri.parse(videoUrl))
                            if (!disposed.get()) {
                                mp.prepareAsync()
                            }
                        } catch (_: Exception) {
                            failed = true
                        }
                    }
                    if (failed && !disposed.get()) {
                        withContext(Dispatchers.Main) {
                            if (!disposed.get()) {
                                state.isPrepared = false
                                state.hasStarted = false
                                state.hasError = true
                                state.isPlaying = false
                            }
                        }
                    }
                }
            }

            onDispose {
                disposed.set(true)
                state.isDisposed = true
                state.isPrepared = false
                state.hasStarted = false
                if (state.mediaPlayerRef == mp) {
                    state.mediaPlayerRef = null
                }
                videoReleaseScope.launch {
                    synchronized(mp) {
                        try {
                            mp.setSurface(null)
                        } catch (_: Exception) {}
                        try {
                            // release() basta para liberar el reproductor y su decodificador.
                            mp.release()
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }
}
