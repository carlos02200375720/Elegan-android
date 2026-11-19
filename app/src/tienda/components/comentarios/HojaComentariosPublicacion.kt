package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductJson
import com.example.data.model.PublicationComment
import com.example.data.model.productId
import com.example.data.model.productName
import com.example.data.remote.MobileSession
import com.example.ui.components.EleganNotificationHost
import com.example.ui.components.EleganNotificationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicationCommentsBottomSheet(
    product: ProductJson,
    displayName: String,
    comments: List<PublicationComment>,
    isAuthenticated: Boolean = true,
    onRequireAuth: (String, () -> Unit) -> Unit = { _, action -> action() },
    onDismiss: () -> Unit,
    onSubmitComment: (String, String, String, (Result<Unit>) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val commentsNotifier = remember { EleganNotificationState() }
    var newCommentText by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    // skipPartiallyExpanded = true garantiza que abra completamente y no a medias
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun dispatchComment(trimmed: String, authorName: String) {
        isSubmitting = true
        newCommentText = ""
        onSubmitComment(product.productId(), authorName, trimmed) { result ->
            isSubmitting = false
            if (result.isSuccess) {
                commentsNotifier.show("Comentario publicado")
            } else {
                commentsNotifier.show("No se pudo publicar el comentario")
            }
        }
    }

    val submitCurrentComment = {
        val trimmed = newCommentText.trim()
        if (trimmed.isNotEmpty() && !isSubmitting) {
            if (!isAuthenticated) {
                onRequireAuth("Regístrate para comentar en las publicaciones.") {
                    val registeredAuthor = MobileSession.user(context)?.name
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: displayName
                    dispatchComment(trimmed, registeredAuthor)
                }
            } else {
                dispatchComment(trimmed, displayName)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(34.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5CFC7))
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
            ) {
                CabeceraHojaComentarios(
                    productName = product.productName(),
                    onDismiss = onDismiss
                )
                ListaComentariosPublicacion(
                    comments = comments,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                BarraEntradaComentario(
                    commentText = newCommentText,
                    onCommentTextChange = { newCommentText = it },
                    onSubmit = submitCurrentComment
                )
            }

            EleganNotificationHost(
                state = commentsNotifier,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
            )
        }
    }
}
