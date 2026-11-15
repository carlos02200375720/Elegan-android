package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.SupportMessage
import com.example.ui.theme.EleganIvory

val supportQuestionsSuggestions = listOf(
    "¿Cuánto tarda el envío?",
    "¿Cómo elijo mi talla de camisa o blazer?",
    "¿Tienen algún cupón de descuento activo?",
    "¿Cómo gestiono una devolución o cambio?"
)

@Composable
fun ChatsScreen(
    messages: List<SupportMessage>,
    cartItemCount: Long = 0L,
    onNavigateToCart: () -> Unit = {},
    onSendMessage: (text: String, mediaUri: String, mediaType: String) -> Unit,
    onBack: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaType by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val mime = context.contentResolver.getType(uri).orEmpty()
            pendingMediaUri = uri
            pendingMediaType = if (mime.startsWith("video/")) "video" else "image"
        }
    }

    LaunchedEffect(messages.size, isKeyboardOpen) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CabeceraChatConcierge(
                cartItemCount = cartItemCount,
                onBack = onBack,
                onNavigateToCart = onNavigateToCart
            )
        },
        bottomBar = {
            BarraEntradaMensajeChat(
                inputText = inputText,
                onInputTextChange = { inputText = it },
                pendingMediaUri = pendingMediaUri,
                pendingMediaType = pendingMediaType,
                isKeyboardOpen = isKeyboardOpen,
                onOpenMediaPicker = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                onRemoveAttachment = {
                    pendingMediaUri = null
                    pendingMediaType = ""
                },
                onSend = {
                    val media = pendingMediaUri?.toString().orEmpty()
                    if (inputText.isNotBlank() || media.isNotBlank()) {
                        onSendMessage(inputText, media, pendingMediaType)
                        inputText = ""
                        pendingMediaUri = null
                        pendingMediaType = ""
                    }
                }
            )
        },
        containerColor = EleganIvory
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 10.dp
                isWide -> 24.dp
                else -> 16.dp
            }
            val maxBubbleWidth = (maxWidth * if (isWide) 0.68f else 0.78f).coerceIn(220.dp, 460.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .testTag("chats_screen"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    BurbujaMensajeChat(
                        msg = msg,
                        isCompact = isCompact,
                        maxBubbleWidth = maxBubbleWidth
                    )
                }
            }
        }
    }
}
