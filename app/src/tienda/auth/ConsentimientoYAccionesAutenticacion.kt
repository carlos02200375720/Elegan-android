package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LegalDocType
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun CasillaConsentimientoLegal(
    isCompact: Boolean,
    acceptedTerms: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onOpenLegalDoc: (LegalDocType) -> Unit
) {
    val consentAnnotated = remember(isCompact) {
        val linkStyle = TextLinkStyles(
            style = SpanStyle(
                color = EleganBurgundy,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        )
        buildAnnotatedString {
            append("He leído y acepto los ")
            withLink(LinkAnnotation.Clickable(tag = "terms", styles = linkStyle) {
                onOpenLegalDoc(LegalDocType.TERMS_AND_CONDITIONS)
            }) {
                append("Términos y Condiciones")
            }
            append(" y la ")
            withLink(LinkAnnotation.Clickable(tag = "privacy", styles = linkStyle) {
                onOpenLegalDoc(LegalDocType.PRIVACY_POLICY)
            }) {
                append("Política de Privacidad")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = acceptedTerms,
            onCheckedChange = onAcceptedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = EleganBurgundy,
                uncheckedColor = Color(0xFF8C847C),
                checkmarkColor = Color.White
            ),
            modifier = Modifier.testTag("auth_accept_terms_checkbox")
        )
        Text(
            text = consentAnnotated,
            style = MaterialTheme.typography.bodySmall.copy(
                color = EleganCharcoal,
                fontSize = if (isCompact) 11.5.sp else 12.5.sp,
                lineHeight = 17.sp
            ),
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        )
    }
}

@Composable
internal fun TarjetaErrorAutenticacion(errorMessage: String, isCompact: Boolean) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
        border = BorderStroke(1.dp, Color(0xFFFFCCCC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = errorMessage,
                color = Color(0xFFD32F2F),
                fontSize = if (isCompact) 12.5.sp else 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
internal fun BotonesAccionAutenticacion(
    isRegister: Boolean,
    isLoading: Boolean,
    acceptedTerms: Boolean,
    isCompact: Boolean,
    isShortScreen: Boolean,
    buttonHeight: Dp,
    onSubmit: () -> Unit,
    onGuestSignIn: () -> Unit
) {
    Button(
        onClick = onSubmit,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = buttonHeight)
            .testTag("auth_submit_button"),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EleganBurgundy,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFD6CEC7),
            disabledContentColor = Color.White.copy(alpha = 0.85f)
        ),
        enabled = !isLoading && (!isRegister || acceptedTerms)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = if (isRegister) "Crear Mi Cuenta Elegan" else "Iniciar Sesión",
                fontWeight = FontWeight.SemiBold,
                fontSize = if (isCompact) 14.sp else 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 10.dp else 14.dp))

    OutlinedButton(
        onClick = onGuestSignIn,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = buttonHeight)
            .testTag("auth_guest_button"),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.5.dp, EleganBurgundy),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy)
    ) {
        Text(
            text = "Ingresar como Invitado",
            fontSize = if (isCompact) 14.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
