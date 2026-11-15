package com.example.ui.components

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun SuperficieTextureVideo(
    state: EstadoReproductorVideo,
    isFocused: Boolean
) {
    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                state.textureViewRef = this

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        state.surfaceRef?.release()
                        val s = Surface(st)
                        state.surfaceRef = s
                        try {
                            state.mediaPlayerRef?.setSurface(s)
                            if ((state.isPlaying || isFocused) && state.isPrepared) {
                                state.mediaPlayerRef?.start()
                            }
                        } catch (_: Exception) {}
                        ajustarTransformacionVideo(this@apply, state.videoWidth, state.videoHeight)
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        ajustarTransformacionVideo(this@apply, state.videoWidth, state.videoHeight)
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            state.mediaPlayerRef?.pause()
                            state.mediaPlayerRef?.setSurface(null)
                        } catch (_: Exception) {}
                        state.surfaceRef?.release()
                        state.surfaceRef = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                        if (!state.isFirstFrameRendered) {
                            state.isFirstFrameRendered = true
                        }
                    }
                }

                addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                    val w = right - left
                    val h = bottom - top
                    if (w > 0 && h > 0 && (w != oldRight - oldLeft || h != oldBottom - oldTop)) {
                        ajustarTransformacionVideo(this, state.videoWidth, state.videoHeight)
                    }
                }

                if (isAvailable && surfaceTexture != null) {
                    val s = Surface(surfaceTexture)
                    state.surfaceRef = s
                    try {
                        state.mediaPlayerRef?.setSurface(s)
                    } catch (_: Exception) {}
                    ajustarTransformacionVideo(this, state.videoWidth, state.videoHeight)
                }
            }
        },
        update = { tv ->
            state.textureViewRef = tv
            ajustarTransformacionVideo(tv, state.videoWidth, state.videoHeight)
        },
        modifier = Modifier.fillMaxSize()
    )
}
