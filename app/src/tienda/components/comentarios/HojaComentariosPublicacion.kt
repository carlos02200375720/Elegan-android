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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.MobileSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
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
    var blockedUserIds by remember { mutableStateOf(setOf<String>()) }
    var pendingBlock by remember { mutableStateOf<PublicationComment?>(null) }
    val scope = rememberCoroutineScope()
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
                    comments = comments.filterNot { it.userId in blockedUserIds },
                    modifier = Modifier.weight(1f),
                    currentUserId = MobileSession.userId(context).orEmpty(),
                    onReport = { comment ->
                        if (!isAuthenticated) {
                            onRequireAuth("Inicia sesión para reportar comentarios.") {}
                        } else {
                            scope.launch {
                                try {
                                    val token = MobileSession.token(context)
                                        ?.takeIf { it.isNotBlank() && !it.startsWith("local_session_") }
                                        ?: throw IllegalStateException("Inicia sesión para reportar.")
                                    ApiAutenticacionMovil.service.reportComment(
                                        authorization = "Bearer $token",
                                        commentId = comment.id,
                                        request = mapOf("reason" to "Contenido inapropiado")
                                    )
                                    commentsNotifier.show("Reporte enviado para revisión")
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (error: Exception) {
                                    MonitorErrores.registrarAdvertencia("EleganComments", "No se pudo enviar el reporte del comentario.", error)
                                    commentsNotifier.show(error.message ?: "No se pudo enviar el reporte")
                                }
                            }
                        }
                    },
                    onBlock = { comment ->
                        if (!isAuthenticated) {
                            onRequireAuth("Inicia sesión para bloquear usuarios.") {}
                        } else {
                            pendingBlock = comment
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                BarraEntradaComentario(
                    commentText = newCommentText,
                    onCommentTextChange = { newCommentText = it },
                    onSubmit = submitCurrentComment
                )
            }

            pendingBlock?.let { target ->
                AlertDialog(
                    onDismissRequest = { pendingBlock = null },
                    title = { Text("Bloquear usuario") },
                    text = { Text("Dejarás de ver los comentarios de este usuario en las publicaciones.") },
                    confirmButton = {
                        TextButton(onClick = {
                            pendingBlock = null
                            scope.launch {
                                try {
                                    val token = MobileSession.token(context)
                                        ?.takeIf { it.isNotBlank() && !it.startsWith("local_session_") }
                                        ?: throw IllegalStateException("Inicia sesión para bloquear.")
                                    ApiAutenticacionMovil.service.blockUser(
                                        authorization = "Bearer $token",
                                        userId = target.userId
                                    )
                                    blockedUserIds = blockedUserIds + target.userId
                                    commentsNotifier.show("Usuario bloqueado")
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (error: Exception) {
                                    MonitorErrores.registrarAdvertencia("EleganComments", "No se pudo bloquear al usuario.", error)
                                    commentsNotifier.show(error.message ?: "No se pudo bloquear al usuario")
                                }
                            }
                        }) { Text("Bloquear") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingBlock = null }) { Text("Cancelar") }
                    }
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
