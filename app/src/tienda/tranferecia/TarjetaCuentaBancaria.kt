package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccountInfo
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganBurgundyDark
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold

@Composable
fun TarjetaCuentaBancaria(
    account: BankAccountInfo,
    isCopied: Boolean,
    onCopyAccount: () -> Unit,
    isHolderNameCopied: Boolean = false,
    onCopyHolderName: () -> Unit = {},
    isHolderDocumentCopied: Boolean = false,
    onCopyHolderDocument: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAF8F5),
        border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_account_card_${account.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = EleganBurgundy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = account.bankName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = EleganCharcoal
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (account.accountType.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EleganGold.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = account.accountType,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = EleganBurgundyDark,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CampoCopiableCuenta(
                label = "Número de cuenta",
                value = account.accountNumber,
                isCopied = isCopied,
                copyContentDescription = "Copiar número de cuenta",
                buttonTestTag = "copy_account_${account.id}",
                isPrimaryValue = true,
                onCopy = onCopyAccount
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoCopiableCuenta(
                label = "Dueño de la cuenta",
                value = account.holderName,
                isCopied = isHolderNameCopied,
                copyContentDescription = "Copiar dueño de la cuenta",
                buttonTestTag = "copy_holder_name_${account.id}",
                isPrimaryValue = false,
                onCopy = onCopyHolderName
            )

            Spacer(modifier = Modifier.height(8.dp))

            CampoCopiableCuenta(
                label = "No. de documento",
                value = account.holderDocument,
                isCopied = isHolderDocumentCopied,
                copyContentDescription = "Copiar número de documento",
                buttonTestTag = "copy_holder_document_${account.id}",
                isPrimaryValue = false,
                onCopy = onCopyHolderDocument
            )
        }
    }
}

@Composable
private fun CampoCopiableCuenta(
    label: String,
    value: String,
    isCopied: Boolean,
    copyContentDescription: String,
    buttonTestTag: String,
    isPrimaryValue: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFEAE3DC), RoundedCornerShape(8.dp))
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            )
            Text(
                text = value,
                style = if (isPrimaryValue) {
                    MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = EleganCharcoal,
                        letterSpacing = 0.6.sp
                    )
                } else {
                    MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = EleganCharcoal
                    )
                }
            )
        }
        IconButton(
            onClick = onCopy,
            enabled = value.isNotBlank(),
            modifier = Modifier.testTag(buttonTestTag)
        ) {
            Icon(
                imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = copyContentDescription,
                tint = if (isCopied) Color(0xFF2E7D32) else EleganBurgundy,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
