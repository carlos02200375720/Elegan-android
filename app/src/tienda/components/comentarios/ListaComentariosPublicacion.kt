package com.example.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PublicationComment
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun ListaComentariosPublicacion(
    comments: List<PublicationComment>,
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    onReport: (PublicationComment) -> Unit = {},
    onBlock: (PublicationComment) -> Unit = {}
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(comments, key = { it.id }) { comment ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = comment.displayName,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.text,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF333333),
                        lineHeight = 18.sp
                    )
                )
                if (comment.userId.isNotBlank() && comment.userId != currentUserId) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { onReport(comment) }) { Text("Reportar") }
                        TextButton(onClick = { onBlock(comment) }) { Text("Bloquear") }
                    }
                }
            }
            HorizontalDivider(color = Color(0xFFF5F0EA), thickness = 0.75.dp)
        }
        if (comments.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Aún no hay comentarios.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sé el primero en compartir tu opinión sobre esta prenda.",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
