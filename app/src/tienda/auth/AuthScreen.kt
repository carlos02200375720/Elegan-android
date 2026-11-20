package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.LegalDocType
import com.example.ui.components.LegalDocumentDialog

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onGuestContinue: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var confirmPasswordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var activeLegalDoc by remember { mutableStateOf<LegalDocType?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordRecovery by remember { mutableStateOf(false) }

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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val isCompact = maxWidth < 350.dp
        val isWide = maxWidth >= 600.dp
        val isShortScreen = maxHeight < 640.dp

        val horizontalPad = when {
            isCompact -> 16.dp
            isWide -> 32.dp
            else -> 24.dp
        }
        val verticalPad = if (isCompact || isShortScreen) 12.dp else 20.dp
        val topSpacer = when {
            isCompact || isShortScreen -> 8.dp
            isWide -> 28.dp
            else -> 16.dp
        }
        val logoSize = when {
            isCompact || isShortScreen -> 72.dp
            isWide -> 92.dp
            else -> 86.dp
        }
        val sectionSpacer = if (isCompact || isShortScreen) 18.dp else 26.dp
        val fieldGap = if (isCompact || isShortScreen) 10.dp else 14.dp
        val buttonHeight = if (isCompact) 46.dp else 48.dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .padding(horizontal = horizontalPad, vertical = verticalPad),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(topSpacer))

                CabeceraMarcaAutenticacion(
                    logoSize = logoSize,
                    isCompact = isCompact,
                    isShortScreen = isShortScreen
                )

                Spacer(modifier = Modifier.height(sectionSpacer))

                SelectorPestanasAutenticacion(
                    selectedTab = selectedTab,
                    isCompact = isCompact,
                    onSelectTab = {
                        selectedTab = it
                        errorMessage = null
                    }
                )

                Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 18.dp else 24.dp))

                CamposFormularioAutenticacion(
                    isRegister = selectedTab == 1,
                    fieldGap = fieldGap,
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

                if (selectedTab == 0) {
                    TextButton(
                        onClick = { showPasswordRecovery = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = com.example.ui.theme.EleganBurgundy)
                    }
                }

                if (selectedTab == 1) {
                    Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 10.dp else 12.dp))
                    CasillaConsentimientoLegal(
                        isCompact = isCompact,
                        acceptedTerms = acceptedTerms,
                        onAcceptedChange = { acceptedTerms = it },
                        onOpenLegalDoc = { activeLegalDoc = it }
                    )
                }

                errorMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 12.dp else 16.dp))
                    TarjetaErrorAutenticacion(errorMessage = msg, isCompact = isCompact)
                }

                Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 18.dp else 24.dp))

                BotonesAccionAutenticacion(
                    isRegister = selectedTab == 1,
                    isLoading = isLoading,
                    acceptedTerms = acceptedTerms,
                    isCompact = isCompact,
                    isShortScreen = isShortScreen,
                    buttonHeight = buttonHeight,
                    onSubmit = submitAuth,
                    onGuestSignIn = onGuestContinue
                )

                Spacer(modifier = Modifier.height(if (isCompact || isShortScreen) 16.dp else 24.dp))
            }
        }
    }

    if (showPasswordRecovery) {
        DialogoRecuperacionContrasena(
            onDismiss = { showPasswordRecovery = false },
            onPasswordReset = {
                errorMessage = "Tu contraseña se actualizó. Ya puedes iniciar sesión."
                selectedTab = 0
                passwordInput = ""
            }
        )
    }

    activeLegalDoc?.let { docType ->
        LegalDocumentDialog(
            docType = docType,
            onDismiss = { activeLegalDoc = null }
        )
    }
}
