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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.EleganTopBarShadow
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

enum class LegalDocType {
    PRIVACY_POLICY,
    TERMS_AND_CONDITIONS
}

private data class LegalSection(
    val title: String,
    val body: String
)

private val privacySections = listOf(
    LegalSection(
        title = "1. Responsable de los datos",
        body = "ELEGAN Boutique (República Dominicana) es la entidad responsable del tratamiento de los datos personales recopilados en esta aplicación. Para cualquier consulta de soporte o privacidad puedes escribirnos al correo oficial: cg0220037@gmail.com."
    ),
    LegalSection(
        title = "2. Datos personales recopilados",
        body = "Al crear una cuenta o confirmar un pedido recopilamos únicamente los datos necesarios para gestionar tu compra y entrega: nombre completo, correo electrónico, número de teléfono y dirección de envío, almacenados de forma cifrada y segura en nuestras bases de datos en la nube (MongoDB Atlas) y en la sesión segura de tu dispositivo."
    ),
    LegalSection(
        title = "3. Uso de Galería y Selector de Imágenes (Comprobantes)",
        body = "El acceso a tus imágenes mediante el selector seguro del sistema se utiliza única y exclusivamente cuando decides voluntariamente subir la fotografía del comprobante de transferencia bancaria para validar tu pedido (o adjuntar una imagen en el chat de soporte). Dicha imagen se almacena de forma privada en servidores seguros (Google Cloud Storage) sin hacerse pública ni mostrarse a otros usuarios."
    ),
    LegalSection(
        title = "4. Proveedores tecnológicos (Terceros)",
        body = "Tus datos y comprobantes se procesan bajo la infraestructura segura de Google Cloud (Cloud Run y Cloud Storage) y MongoDB Atlas exclusivamente para operar la tienda y validar tus pedidos. Nunca vendemos, alquilamos ni compartimos tu información personal con terceros con fines comerciales."
    ),
    LegalSection(
        title = "5. Retención y eliminación de datos",
        body = "Puedes solicitar la eliminación de tu cuenta desde la opción \"Eliminar mi cuenta\" en el menú de la aplicación o escribiendo a cg0220037@gmail.com. Al completar la solicitud eliminamos tu cuenta, perfil, carrito, publicaciones guardadas, conversaciones y comprobantes. Los registros de compra que deban conservarse por obligaciones contables, fiscales o para atender reclamaciones se desvinculan de tu cuenta y se anonimizan; dejan de estar disponibles en tu historial personal. Los demás datos personales se eliminan de nuestros sistemas."
    )
)

private val termsSections = listOf(
    LegalSection(
        title = "1. Alcance de la tienda",
        body = "ELEGAN Boutique es una tienda de moda y accesorios masculinos operada en República Dominicana. Esta aplicación está destinada exclusivamente a la compra directa de prendas de la tienda y no cuenta con publicaciones sociales públicas ni contenido generado por usuarios."
    ),
    LegalSection(
        title = "2. Proceso de compra y validación de transferencias",
        body = "Los pedidos pagados mediante transferencia bancaria quedan en estado pendiente hasta que nuestro equipo verifique el comprobante enviado por el cliente y confirme la acreditación de los fondos en la cuenta bancaria correspondiente."
    ),
    LegalSection(
        title = "3. Comprobantes falsos, alterados o ilegibles",
        body = "ELEGAN Boutique se reserva el derecho de rechazar o cancelar cualquier pedido si el comprobante enviado no coincide con el monto total de la compra, resulta ilegible, presenta alteraciones o no se refleja acreditado en nuestra cuenta bancaria."
    ),
    LegalSection(
        title = "4. Envíos, cambios y devoluciones",
        body = "Las entregas en Santo Domingo se realizan en un plazo estimado de 24 a 48 horas hábiles, y hacia el interior de República Dominicana en un plazo de 2 a 4 días hábiles tras la confirmación del pedido. Se aceptan cambios de talla o devoluciones dentro de los 7 días posteriores a la recepción, siempre que el artículo esté sin uso y con sus etiquetas y embalaje originales."
    ),
    LegalSection(
        title = "5. Atención al cliente",
        body = "Para consultas sobre pedidos, validaciones de pago, envíos o cambios puedes comunicarte desde el chat de soporte en la app o escribiendo a cg0220037@gmail.com."
    )
)

@Composable
fun LegalDocumentDialog(
    docType: LegalDocType,
    onDismiss: () -> Unit
) {
    val isPrivacy = docType == LegalDocType.PRIVACY_POLICY
    val title = if (isPrivacy) "Política de Privacidad" else "Términos y Condiciones de Uso"
    val subtitle = if (isPrivacy) {
        "ELEGAN Boutique • República Dominicana"
    } else {
        "Reglas de compra, envíos y transferencias"
    }
    val sections = if (isPrivacy) privacySections else termsSections

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
                shape = RoundedCornerShape(16.dp),
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
                    // Cabecera fija de 50dp consistente con toda la app
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 14.dp, end = 4.dp),
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
                                text = title,
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
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("legal_dialog_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = EleganCharcoal
                                )
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
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFAF6F1),
                            border = BorderStroke(1.dp, Color(0xFFE8DFD6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = EleganBurgundyDark,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Contacto oficial: cg0220037@gmail.com",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF66605A)
                                    )
                                )
                            }
                        }

                        sections.forEach { section ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal,
                                        fontSize = if (isCompact) 13.5.sp else 14.5.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = section.body,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = Color(0xFF4A4A4A),
                                        lineHeight = 19.sp,
                                        fontSize = if (isCompact) 12.5.sp else 13.sp
                                    )
                                )
                            }
                            HorizontalDivider(color = Color(0xFFF0EAE3))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EleganBurgundy,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("legal_dialog_accept_btn")
                        ) {
                            Text(
                                text = "Entendido",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
