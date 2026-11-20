package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BannerErrorPago(
    message: String?,
    visible: Boolean,
    showRetry: Boolean = false,
    retryLabel: String = "Reintentar",
    onRetry: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && !message.isNullOrBlank(),
        enter = fadeIn(animationSpec = tween(180)) +
            slideInVertically(animationSpec = tween(220)) { -it / 2 },
        exit = fadeOut(animationSpec = tween(160)) +
            slideOutVertically(animationSpec = tween(180)) { -it / 2 },
        modifier = modifier
    ) {
        val text = message.orEmpty()
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFFDF2F2),
            border = BorderStroke(1.dp, Color(0xFFE7B8BD)),
            shadowElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .testTag("payment_error_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error de pago",
                    tint = Color(0xFFB00020),
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "No se pudo completar la acción",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Color(0xFF8C0D1E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF4A151B),
                            lineHeight = 17.sp,
                            fontSize = 12.5.sp
                        )
                    )
                    if (showRetry && onRetry != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF8C0D1E)
                            ),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("payment_error_retry_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = retryLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dismiss_payment_error_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar aviso de error",
                        tint = Color(0xFF8C0D1E),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
