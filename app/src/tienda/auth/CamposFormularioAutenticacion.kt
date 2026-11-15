package com.example.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EleganBurgundy

@Composable
internal fun CamposFormularioAutenticacion(
    isRegister: Boolean,
    fieldGap: Dp,
    nameInput: String,
    onNameChange: (String) -> Unit,
    emailInput: String,
    onEmailChange: (String) -> Unit,
    passwordInput: String,
    onPasswordChange: (String) -> Unit,
    confirmPasswordInput: String,
    onConfirmPasswordChange: (String) -> Unit,
    isPasswordVisible: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    if (isRegister) {
        CampoTextoAutenticacion(
            value = nameInput,
            onValueChange = onNameChange,
            label = "Nombre completo",
            leadingIcon = Icons.Default.Person,
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            testTag = "auth_name_input"
        )
        Spacer(modifier = Modifier.height(fieldGap))
    }

    CampoTextoAutenticacion(
        value = emailInput,
        onValueChange = onEmailChange,
        label = "Correo electrónico",
        leadingIcon = Icons.Default.Email,
        keyboardType = KeyboardType.Email,
        imeAction = ImeAction.Next,
        testTag = "auth_email_input"
    )

    Spacer(modifier = Modifier.height(fieldGap))

    CampoTextoAutenticacion(
        value = passwordInput,
        onValueChange = onPasswordChange,
        label = "Contraseña",
        leadingIcon = Icons.Default.Lock,
        keyboardType = KeyboardType.Password,
        imeAction = if (isRegister) ImeAction.Next else ImeAction.Done,
        testTag = "auth_password_input",
        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        onDone = { if (!isRegister) onSubmit() },
        trailingIcon = {
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Mostrar u ocultar contraseña",
                    tint = Color(0xFF777777)
                )
            }
        }
    )

    if (isRegister) {
        Spacer(modifier = Modifier.height(fieldGap))

        CampoTextoAutenticacion(
            value = confirmPasswordInput,
            onValueChange = onConfirmPasswordChange,
            label = "Confirmar contraseña",
            leadingIcon = Icons.Default.Lock,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
            testTag = "auth_confirm_password_input",
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            onDone = onSubmit
        )
    }
}

@Composable
private fun CampoTextoAutenticacion(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    testTag: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onDone: (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = EleganBurgundy)
        },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onDone?.invoke() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = EleganBurgundy,
            unfocusedBorderColor = Color(0xFFDDD5CE)
        )
    )
}
