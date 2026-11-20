package com.example.ui.components

import android.view.TextureView
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
internal fun SuperficieTextureVideo(
    state: EstadoReproductorVideo
) {
    val textureView = state.textureViewRef
    val player = state.playerRef

    DisposableEffect(player, textureView) {
        if (player != null && textureView != null) {
            player.setVideoTextureView(textureView)
        }
        onDispose {
            if (player != null && textureView != null) {
                runCatching { player.clearVideoTextureView(textureView) }
            }
        }
    }

    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                state.textureViewRef = this
            }
        },
        update = { view ->
            if (state.textureViewRef !== view) {
                state.textureViewRef = view
            }
            ajustarTransformacionVideo(view, state.videoWidth, state.videoHeight)
        },
        modifier = Modifier.fillMaxSize()
    )
}
