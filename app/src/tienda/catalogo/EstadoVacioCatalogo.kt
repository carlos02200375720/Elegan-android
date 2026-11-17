package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
fun EstadoVacioCatalogo(
    isCompact: Boolean,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    hasActiveFilters: Boolean = true,
    onRetry: () -> Unit = {},
    onResetFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isCompact) 16.dp else 32.dp, vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = EleganBurgundy)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Cargando productos desde el servidor...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                }

                !errorMessage.isNullOrBlank() -> {
                    Text(
                        text = "No se pudo cargar el catálogo",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                    ) {
                        Text("Reintentar")
                    }
                }

                !hasActiveFilters -> {
                    Text(
                        text = "Catálogo sin productos disponibles",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Actualmente no hay prendas publicadas en el backend.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                    ) {
                        Text("Actualizar catálogo")
                    }
                }

                else -> {
                    Text(
                        text = "No se encontraron productos",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Intenta con otro término de búsqueda o selecciona otra categoría.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.Gray
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onResetFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = EleganBurgundy)
                    ) {
                        Text("Ver todo el catálogo")
                    }
                }
            }
        }
    }
}
