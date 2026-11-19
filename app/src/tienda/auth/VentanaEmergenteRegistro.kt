package com.example.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.components.LegalDocType
import com.example.ui.components.LegalDocumentDialog
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal

@Composable
fun VentanaEmergenteRegistro(
    promptMessage: String? = null,
    initialTab: Int = 1,
    onDismiss: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var activeLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val submitAuth: () -> Unit = {
        validarYEjecutarAutenticacion(
            context = context,
            coroutineScope = coroutineScope,
            isRegister = selectedTab == 1,
            nameInput = nameInput,
            emailInput = emailInput,
            passwordInput = passwordInput,
            confirmPasswordInput = confirmPasswordInput,
            acceptedTerms = acceptedTerms,
            onLoadingChange = { isLoading = it },
            onError = { errorMessage = it },
            onAuthSuccess = onAuthSuccess
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 10.dp,
                border = BorderStroke(1.dp, Color(0xFFEDE6DF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .heightIn(max = 720.dp)
                    .testTag("register_popup_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF6EFE9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.ic_launcher_fg),
                                    contentDescription = "Logo Elegan",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .graphicsLayer(scaleX = 1.35f, scaleY = 1.35f)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedTab == 1) "Crea tu cuenta Elegan" else "Inicia sesión en Elegan",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundyDark,
                                        fontSize = 18.sp
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = promptMessage?.takeIf { it.isNotBlank() }
                                        ?: "Regístrate en segundos para interactuar, guardar prendas y comprar.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = EleganCharcoal.copy(alpha = 0.72f),
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    )
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("register_popup_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar ventana de registro",
                                tint = Color(0xFF6E655E)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SelectorPestanasAutenticacion(
                        selectedTab = selectedTab,
                        isCompact = false,
                        onSelectTab = {
                            selectedTab = it
                            errorMessage = null
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CamposFormularioAutenticacion(
                        isRegister = selectedTab == 1,
                        fieldGap = 12.dp,
                        nameInput = nameInput,
                        onNameChange = { nameInput = it },
                        emailInput = emailInput,
                        onEmailChange = { emailInput = it },
                        passwordInput = passwordInput,
                        onPasswordChange = { passwordInput = it },
                        confirmPasswordInput = confirmPasswordInput,
                        onConfirmPasswordChange = { confirmPasswordInput = it },
                        isPasswordVisible = isPasswordVisible,
                        onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
                        onSubmit = submitAuth
                    )

                    if (selectedTab == 1) {
                        Spacer(modifier = Modifier.height(10.dp))
                        CasillaConsentimientoLegal(
                            isCompact = true,
                            acceptedTerms = acceptedTerms,
                            onAcceptedChange = { acceptedTerms = it },
                            onOpenLegalDoc = { activeLegalDoc = it }
                        )
                    }

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(12.dp))
                        TarjetaErrorAutenticacion(errorMessage = msg, isCompact = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val isRegister = selectedTab == 1
                    Button(
                        onClick = submitAuth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("register_popup_submit_button"),
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
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegister) "Crear Mi Cuenta Elegan" else "Iniciar Sesión",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("register_popup_dismiss_button"),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF6E655E))
                    ) {
                        Text(
                            text = "Seguir explorando",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    activeLegalDoc?.let { docType ->
        LegalDocumentDialog(
            docType = docType,
            onDismiss = { activeLegalDoc = null }
        )
    }
}
