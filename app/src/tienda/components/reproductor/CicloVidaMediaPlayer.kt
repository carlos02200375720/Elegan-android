package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer

@Composable
internal fun CicloVidaMediaPlayer(
    videoUrl: String,
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

            val player = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(
                    crearMediaSourceConCacheVideo(context.applicationContext)
                )
                .build()
                .apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = if (state.isMuted) 0f else 1f
                }

            state.playerRef = player

            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (state.isDisposed || state.playerRef !== player) return
                    state.isPrepared = playbackState == Player.STATE_READY
                    if (playbackState == Player.STATE_ENDED) {
                        state.hasStarted = false
                        state.isCompleted = true
                        state.isPlaying = false
                        state.showOverlayControls = true
                    } else if (playbackState == Player.STATE_READY && state.isPlaying) {
                        player.play()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (state.isDisposed || state.playerRef !== player) return
                    state.hasStarted = isPlaying
                }

                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (state.isDisposed || state.playerRef !== player) return
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        state.videoWidth = videoSize.width
                        state.videoHeight = videoSize.height
                        ajustarTransformacionVideo(
                            state.textureViewRef,
                            videoSize.width,
                            videoSize.height
                        )
                    }
                }

                override fun onRenderedFirstFrame() {
                    if (!state.isDisposed && state.playerRef === player) {
                        state.isFirstFrameRendered = true
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    if (state.isDisposed || state.playerRef !== player) return
                    state.isPrepared = false
                    state.hasStarted = false
                    state.hasError = true
                    state.isPlaying = false
                }
            })

            if (videoUrl.isNotBlank()) {
                player.setMediaItem(MediaItem.fromUri(videoUrl))
                player.prepare()
                if (state.isPlaying) player.play()
            } else {
                state.hasError = true
            }

            onDispose {
                state.isDisposed = true
                state.isPrepared = false
                state.hasStarted = false
                if (state.playerRef === player) {
                    state.textureViewRef?.let { textureView ->
                        runCatching { player.clearVideoTextureView(textureView) }
                    }
                    state.playerRef = null
                }
                player.release()
            }
        }
    }
}
