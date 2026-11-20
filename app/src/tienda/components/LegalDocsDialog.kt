package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.observability.MonitorErrores
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.remote.ApiAutenticacionMovil
import com.example.ui.EleganTopBarShadow
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

enum class LegalDocType {
    PRIVACY_POLICY,
    TERMS_AND_CONDITIONS
}

private data class LegalDocument(
    val title: String,
    val subtitle: String,
    val content: String
)

@Composable
fun LegalDocumentDialog(
    docType: LegalDocType,
    onDismiss: () -> Unit
) {
    val isPrivacy = docType == LegalDocType.PRIVACY_POLICY
    val documentType = if (isPrivacy) "privacy_policy" else "terms_and_conditions"
    var document by remember(docType) { mutableStateOf<LegalDocument?>(null) }
    var loading by remember(docType) { mutableStateOf(true) }
    var error by remember(docType) { mutableStateOf("") }

    LaunchedEffect(documentType) {
        loading = true
        error = ""
        try {
            val response = ApiAutenticacionMovil.service.getLegalDocuments()
            val items = response["items"] as? List<*> ?: emptyList<Any?>()
            val item = items.filterIsInstance<Map<*, *>>()
                .firstOrNull { it["type"]?.toString() == documentType }
            if (item == null) {
                error = "El documento todavía no está publicado."
            } else {
                document = LegalDocument(
                    title = item["title"]?.toString().orEmpty(),
                    subtitle = item["subtitle"]?.toString().orEmpty(),
                    content = item["content"]?.toString().orEmpty()
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MonitorErrores.registrarAdvertencia("LegalDocs", "No se pudo cargar el documento legal $documentType.", e)
            error = "No se pudo cargar el documento. Comprueba tu conexión e inténtalo de nuevo."
        } finally {
            loading = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 12.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            val isCompact = maxWidth < 350.dp
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp)
                    .height((maxHeight * 0.88f).coerceAtLeast(320.dp))
                    .testTag(if (isPrivacy) "privacy_policy_dialog" else "terms_dialog")
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        color = Color.White,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(start = 14.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isPrivacy) Icons.Outlined.PrivacyTip else Icons.Outlined.Description,
                                contentDescription = null,
                                tint = EleganBurgundy,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = document?.title ?: if (isPrivacy) "Política de Privacidad" else "Términos y Condiciones",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EleganCharcoal,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = if (isCompact) 15.sp else 17.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("legal_dialog_close_btn")) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = EleganCharcoal)
                            }
                        }
                    }
                    EleganTopBarShadow()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (isCompact) 14.dp else 18.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, Color(0xFFE8DFD6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = document?.subtitle ?: "ELEGAN Boutique • República Dominicana",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = EleganBurgundyDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Contacto oficial: cg0220037@gmail.com",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF66605A))
                                )
                            }
                        }
                        when {
                            loading -> CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                color = EleganBurgundy
                            )
                            error.isNotBlank() -> Text(
                                text = error,
                                color = Color(0xFF9B1C31),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            else -> document?.content.orEmpty()
                                .split(Regex("\\n\\s*\\n"))
                                .filter { it.isNotBlank() }
                                .forEach { paragraph ->
                                    Text(
                                        text = paragraph.trim(),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = Color(0xFF4A4A4A),
                                            lineHeight = 20.sp,
                                            fontSize = if (isCompact) 12.5.sp else 13.sp
                                        )
                                    )
                                    HorizontalDivider(color = Color(0xFFF0EAE3))
                                }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("legal_dialog_accept_btn")
                        ) {
                            Text("Entendido", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
