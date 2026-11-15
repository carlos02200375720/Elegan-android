package com.example.ui

import android.net.Uri
import android.widget.MediaController
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.LocalBoutiqueMedia
import com.example.data.model.Product
import com.example.ui.components.EleganVideoPlayer
import com.example.ui.components.LocalEleganNotifier
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import com.example.ui.theme.EleganIvory
import kotlinx.coroutines.launch

@Immutable
sealed interface GalleryMediaItem {
    @Immutable
    data class Video(val videoUrl: String, val posterUrl: String) : GalleryMediaItem
    @Immutable
    data class Image(val imageUrl: String) : GalleryMediaItem
}

@Composable
fun ProductoScreen(
    product: Product,
    cartItemCount: Long,
    isSaved: Boolean = false,
    onToggleSave: ((Result<Boolean>) -> Unit) -> Unit = {},
    onAddToCart: (Product, String, String, Long) -> Unit,
    onBuyNow: (Product, String, String, Long) -> Unit,
    onNavigateToCart: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val defaultSize = product.sizes.firstOrNull() ?: "Estándar"
    var selectedSize by remember(product.id) { mutableStateOf(defaultSize) }
    var selectedColor by remember(product.id) { mutableStateOf(product.colors.firstOrNull().orEmpty()) }
    var quantity by remember { mutableLongStateOf(1L) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val notifier = LocalEleganNotifier.current

    val mediaItems = remember(product) { getProductMediaItems(product) }
    val pagerState = rememberPagerState(pageCount = { mediaItems.size })

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                EleganBottomBarShadow()
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onAddToCart(product, selectedSize, selectedColor, quantity)
                                if (notifier != null) {
                                    notifier.show("¡Añadido al carrito con éxito!")
                                } else {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("¡Añadido al carrito con éxito!")
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("add_to_cart_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = EleganBurgundy
                            ),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                                brush = androidx.compose.ui.graphics.SolidColor(EleganBurgundy)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Añadir",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                onBuyNow(product, selectedSize, selectedColor, quantity)
                            },
                            modifier = Modifier
                                .weight(1.2f)
                                .fillMaxHeight()
                                .testTag("buy_now_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Comprar Ahora",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color.White
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("producto_screen")
            ) {
                // Hero product image gallery (contenedor multimedia cuadrado 1:1)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(Color(0xFFF0EBE3))
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (val item = mediaItems.getOrNull(page)) {
                                is GalleryMediaItem.Video -> {
                                    val isVideoFocused = (pagerState.currentPage == page)
                                    EleganVideoPlayer(
                                        videoUrl = item.videoUrl,
                                        posterUrl = item.posterUrl,
                                        aspectRatio = 1f,
                                        autoPlay = isVideoFocused,
                                        isFocused = isVideoFocused,
                                        showControls = true,
                                        testTagPrefix = "gallery_video_${product.id}",
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                is GalleryMediaItem.Image -> {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = "${product.name} - Imagen ${page + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                null -> {}
                            }
                        }

                        // Etiqueta de categoría en esquina inferior izquierda
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = EleganBurgundyDark.copy(alpha = 0.9f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 12.dp)
                        ) {
                            Text(
                                text = product.category.uppercase(),
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Indicador de contenido multimedia en la esquina inferior derecha
                        val currentItem = mediaItems.getOrNull(pagerState.currentPage)
                        val isCurrentVideo = currentItem is GalleryMediaItem.Video

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 12.dp)
                                .testTag("gallery_content_indicator")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentVideo) Icons.Default.Videocam else Icons.Default.Collections,
                                    contentDescription = "Contenido multimedia",
                                    tint = if (isCurrentVideo) EleganGold else Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCurrentVideo) "Video • ${pagerState.currentPage + 1}/${mediaItems.size}" else "${pagerState.currentPage + 1}/${mediaItems.size}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }

                // Info section
                item {
                    Column(modifier = Modifier.padding(20.dp)) {
                        // Título de la publicación más pequeño
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = 16.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Precio en color vino de la marca sin texto USD ni valoraciones ficticias
                        Text(
                            text = "$${String.format("%.2f", product.price)}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = EleganBurgundy,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier.testTag("producto_price_text")
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0xFFE5DDD5))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Description
                        Text(
                            text = "Descripción",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF4A4A4A),
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Size selector
                        if (product.sizes.isNotEmpty()) {
                            Text(
                                text = "Seleccionar Talla / Medida:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("size_selector_row")
                            ) {
                                items(product.sizes) { size ->
                                    val isSelected = size == selectedSize
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) EleganBurgundy else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) EleganBurgundy else Color(0xFFD3CAC0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedSize = size }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .testTag("size_chip_$size"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = size,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) Color.White else EleganCharcoal,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        if (product.colors.isNotEmpty()) {
                            Text(
                                text = "Seleccionar Color:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("color_selector_row")
                            ) {
                                items(product.colors) { color ->
                                    val isSelected = color == selectedColor
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) EleganBurgundy else Color.White)
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) EleganBurgundy else Color(0xFFD3CAC0),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedColor = color }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .testTag("color_chip_$color"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = color,
                                            maxLines = 1,
                                            softWrap = false,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) Color.White else EleganCharcoal,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(20.dp))
                        }

                        // Quantity selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cantidad:",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal
                                )
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFD3CAC0), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { if (quantity > 1) quantity-- },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = "Menos", tint = EleganBurgundy)
                                }
                                Text(
                                    text = "$quantity",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    ),
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                                IconButton(
                                    onClick = { quantity++ },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Más", tint = EleganBurgundy)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Guarantee banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = EleganBurgundy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Envío y embalaje confidencial de lujo",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = EleganCharcoal
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Diamond,
                                        contentDescription = null,
                                        tint = EleganGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Autenticidad y confección artesanal certificada",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = EleganCharcoal
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }

            // Cabecera fija sin fondo ni sombra sobre la galería
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .background(Color.Transparent)
                    .padding(horizontal = 4.dp)
                    .align(Alignment.TopCenter)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .testTag("producto_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val willSave = !isSaved
                            onToggleSave { result ->
                                notifier?.show(
                                    if (result.isSuccess) {
                                        if (willSave) "Publicación guardada en tu perfil"
                                        else "Publicación eliminada de guardados"
                                    } else {
                                        "No se pudo actualizar la publicación guardada"
                                    }
                                )
                            }
                        },
                        modifier = Modifier.testTag("producto_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isSaved) "Quitar de guardados" else "Guardar publicación",
                            tint = if (isSaved) EleganGold else Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier.testTag("producto_cart_button")
                    ) {
                        if (cartItemCount > 0L) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = EleganBurgundy,
                                        contentColor = Color.White
                                    ) {
                                        Text("$cartItemCount")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Carrito",
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Carrito",
                                tint = Color.Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Retorna la lista unificada de elementos multimedia (video y fotos) para la galería.
 * Si el producto tiene video, la portada del video también acompaña como imagen la galería.
 */
fun getProductMediaItems(product: Product): List<GalleryMediaItem> {
    val items = mutableListOf<GalleryMediaItem>()
    val basePoster = if (product.imageUrl.isNotBlank() && !product.imageUrl.endsWith(".mp4", ignoreCase = true)) {
        product.imageUrl
    } else {
        product.images.firstOrNull { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }.orEmpty()
    }

    if (product.hasVideo && product.videoUrl.isNotBlank()) {
        val videoPoster = basePoster.ifBlank {
            getDefaultCategoryFallback(product.category)
        }
        items.add(GalleryMediaItem.Video(videoUrl = product.videoUrl, posterUrl = videoPoster))
        // La portada que genera el video la toma como imagen para acompañar el contenido de la galería
        if (videoPoster.isNotBlank()) {
            items.add(GalleryMediaItem.Image(imageUrl = videoPoster))
        }
    }

    val images = getProductGalleryImages(product)
    images.forEach { imgUrl ->
        if (imgUrl.isNotBlank() && !imgUrl.endsWith(".mp4", ignoreCase = true)) {
            if (items.none { it is GalleryMediaItem.Image && it.imageUrl == imgUrl }) {
                items.add(GalleryMediaItem.Image(imageUrl = imgUrl))
            }
        }
    }
    return if (items.isNotEmpty()) items else listOf(GalleryMediaItem.Image(basePoster))
}

fun getDefaultCategoryFallback(category: String): String {
    return LocalBoutiqueMedia.fallbackImageForCategory(category)
}

/**
 * Retorna la lista de imágenes para la galería multimedia del producto.
 * Si el producto ya cuenta con su lista de imágenes, se utiliza dicha lista.
 * Si no, se proveen perspectivas locales acordes a su categoría.
 */
fun getProductGalleryImages(product: Product): List<String> {
    val userImages = (listOfNotNull(product.imageUrl.takeIf { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }) +
            product.images.filter { it.isNotBlank() && !it.endsWith(".mp4", ignoreCase = true) }).distinct()
    if (userImages.isNotEmpty()) return userImages

    val base = if (product.imageUrl.isNotBlank() && !product.imageUrl.endsWith(".mp4", ignoreCase = true)) {
        product.imageUrl
    } else {
        getDefaultCategoryFallback(product.category)
    }

    val supplements = LocalBoutiqueMedia.gallerySupplementsForCategory(product.category)
    return (listOf(base) + supplements).distinct()
}
