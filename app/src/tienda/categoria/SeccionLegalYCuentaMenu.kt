package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.LegalDocType
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal

@Composable
internal fun SeccionLegalYCuentaMenu(
    isCompact: Boolean,
    isAuthenticated: Boolean = true,
    onOpenLegalDoc: (LegalDocType) -> Unit,
    onSignOut: () -> Unit = {},
    onRequestDeleteAccount: () -> Unit
) {
    Spacer(modifier = Modifier.height(6.dp))
    HorizontalDivider(color = Color(0xFFEDE6DF))
    Spacer(modifier = Modifier.height(10.dp))

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("menu_legal_account_section")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            FilaAccionLegalMenu(
                icon = Icons.Outlined.PrivacyTip,
                title = "Política de Privacidad",
                tint = EleganBurgundy,
                textColor = EleganCharcoal,
                fontWeight = FontWeight.SemiBold,
                showArrow = true,
                isCompact = isCompact,
                testTag = "menu_privacy_policy_item",
                onClick = { onOpenLegalDoc(LegalDocType.PRIVACY_POLICY) }
            )

            HorizontalDivider(color = Color(0xFFEDE6DF))

            FilaAccionLegalMenu(
                icon = Icons.Outlined.Description,
                title = "Términos y Condiciones de Uso",
                tint = EleganBurgundy,
                textColor = EleganCharcoal,
                fontWeight = FontWeight.SemiBold,
                showArrow = true,
                isCompact = isCompact,
                testTag = "menu_terms_item",
                onClick = { onOpenLegalDoc(LegalDocType.TERMS_AND_CONDITIONS) }
            )

            if (isAuthenticated) {
                HorizontalDivider(color = Color(0xFFEDE6DF))

                FilaAccionLegalMenu(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = stringResource(R.string.menu_sign_out),
                    tint = Color(0xFFB00020),
                    textColor = Color(0xFFB00020),
                    fontWeight = FontWeight.SemiBold,
                    showArrow = false,
                    isCompact = isCompact,
                    testTag = "menu_sign_out_item",
                    onClick = onSignOut
                )
            }

            HorizontalDivider(color = Color(0xFFEDE6DF))

            FilaAccionLegalMenu(
                icon = Icons.Default.DeleteForever,
                title = "Eliminar mi cuenta",
                tint = Color(0xFFC62828),
                textColor = Color(0xFFC62828),
                fontWeight = FontWeight.Bold,
                showArrow = false,
                isCompact = isCompact,
                testTag = "menu_delete_account_item",
                onClick = onRequestDeleteAccount
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun FilaAccionLegalMenu(
    icon: ImageVector,
    title: String,
    tint: Color,
    textColor: Color,
    fontWeight: FontWeight,
    showArrow: Boolean,
    isCompact: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = if (isCompact) 12.dp else 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = fontWeight,
                color = textColor,
                fontSize = if (isCompact) 13.5.sp else 14.sp
            ),
            modifier = Modifier.weight(1f)
        )
        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF9E958C),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
