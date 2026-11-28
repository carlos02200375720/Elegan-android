package com.example.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.example.data.media.ConfiguracionCacheImagenesCoil
import com.example.data.model.Product
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.data.observability.MonitorErrores
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import kotlinx.coroutines.delay

private const val MAX_AUTO_IMAGE_RETRIES = 2

internal fun construirPeticionImagenPublicacion(
    context: Context,
    imageUrl: String,
    maxSizePx: Int = ConfiguracionCacheImagenesCoil.FEED_MEDIA_SIZE_PX,
    retryAttempt: Int = 0
): ImageRequest = ConfiguracionCacheImagenesCoil.peticionImagenPublicacion(
    context = context,
    imageUrl = imageUrl,
    maxSizePx = maxSizePx,
    retryAttempt = retryAttempt
)

@Composable
internal fun ElementoMultimediaPublicacion(
    product: Product,
    item: GalleryMediaItem?,
    page: Int,
    ratio: Float,
    isVideoFocusedProvider: () -> Boolean,
    onProductClick: () -> Unit
) {
    when (item) {
        is GalleryMediaItem.Video -> EleganVideoPlayer(
            videoUrl = item.videoUrl,
            posterUrl = item.posterUrl,
            aspectRatio = ratio,
            autoPlay = false,
            isFocused = isVideoFocusedProvider(),
            showControls = true,
            testTagPrefix = "inicio_video_${product.productId()}_$page",
            modifier = Modifier.fillMaxSize()
        )
        is GalleryMediaItem.Image -> ImagenPublicacionProducto(
            imageUrl = item.imageUrl,
            contentDescription = "${product.productName()} - ${page + 1}",
            onClick = onProductClick
        )
        null -> {}
    }
}

@Composable
internal fun ImagenPublicacionProducto(
    imageUrl: String,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
    maxSizePx: Int = 900
) {
    val context = LocalContext.current
    var retryAttempt by remember(imageUrl) { mutableIntStateOf(0) }
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }

    val isAutoRetrying = imageState is AsyncImagePainter.State.Error && retryAttempt < MAX_AUTO_IMAGE_RETRIES
    val isPermanentError = imageState is AsyncImagePainter.State.Error && retryAttempt >= MAX_AUTO_IMAGE_RETRIES
    val isLoading = imageState is AsyncImagePainter.State.Loading || isAutoRetrying

    LaunchedEffect(imageUrl, imageState, retryAttempt) {
        val errorState = imageState as? AsyncImagePainter.State.Error ?: return@LaunchedEffect
        if (retryAttempt < MAX_AUTO_IMAGE_RETRIES) {
            delay(if (retryAttempt == 0) 600L else 1500L)
            retryAttempt += 1
        } else {
            val sanitizedUrl = imageUrl.trim().substringBefore('?').substringBefore('#').take(180)
            MonitorErrores.registrarAdvertencia(
                tag = "EleganMedia",
                mensaje = "No se pudo cargar imagen de publicación tras reintentos.",
                error = errorState.result.throwable,
                extras = mapOf("imageUrl" to sanitizedUrl)
            )
        }
    }

    val imageRequest = remember(imageUrl, context, maxSizePx, retryAttempt) {
        construirPeticionImagenPublicacion(
            context = context,
            imageUrl = imageUrl,
            maxSizePx = maxSizePx,
            retryAttempt = retryAttempt
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .let { base ->
                if (onClick != null && !isPermanentError) base.clickable(onClick = onClick) else base
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            onState = { state -> imageState = state },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = EleganBurgundy.copy(alpha = 0.7f),
                trackColor = Color(0xFFE0D7CC),
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(28.dp)
            )
        } else if (isPermanentError) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BrokenImage,
                    contentDescription = "Error al cargar imagen",
                    tint = Color(0xFF9C8F82),
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No se pudo cargar esta imagen",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = EleganCharcoal,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = {
                        imageState = AsyncImagePainter.State.Loading(null)
                        retryAttempt += 1
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color.White.copy(alpha = 0.92f),
                        contentColor = EleganBurgundy
                    ),
                    modifier = Modifier.testTag("media_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reintentar carga de imagen",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Reintentar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
