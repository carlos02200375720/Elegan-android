package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy

@Composable
fun DetallesYAccionesPublicacion(
    productId: String,
    description: String,
    commentCount: Int,
    onCommentClick: () -> Unit,
    onProductClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF4A4A4A)
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (commentCount > 0) {
            Text(
                text = "Ver comentarios",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { onCommentClick() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            val isCompactPost = maxWidth < 330.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(if (isCompactPost) 6.dp else 8.dp)
            ) {
                Button(
                    onClick = onProductClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EleganBurgundy,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(
                        horizontal = if (isCompactPost) 10.dp else 16.dp,
                        vertical = 10.dp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("post_buy_btn_$productId")
                ) {
                    Text(
                        text = "Comprar Producto",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isCompactPost) 12.sp else 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                OutlinedButton(
                    onClick = onProductClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(
                        horizontal = if (isCompactPost) 10.dp else 16.dp,
                        vertical = 10.dp
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = EleganBurgundy
                    ),
                    modifier = Modifier.testTag("post_details_btn_$productId")
                ) {
                    Text(
                        text = "Ver Detalle",
                        fontSize = if (isCompactPost) 12.sp else 13.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
