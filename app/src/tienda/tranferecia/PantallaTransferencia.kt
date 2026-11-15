package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BankAccountInfo

@Composable
fun TranfereciaScreen(
    total: Double,
    bankAccounts: List<BankAccountInfo>,
    paymentProofUri: Uri?,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onPickPaymentProof: () -> Unit,
    onRemovePaymentProof: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    BackHandler { onBack() }

    val clipboardManager = LocalClipboardManager.current
    var copiedAccountId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("tranferecia_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            CabeceraTransferencia(
                total = total,
                onBack = onBack
            )
        },
        bottomBar = {
            BarraInferiorTransferencia(
                hasProof = paymentProofUri != null,
                isSubmitting = isSubmitting,
                onConfirmPayment = onConfirmPayment
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 20.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 16.dp)
            ) {
                Text(
                    text = "Transfiere el monto total a cualquiera de las siguientes cuentas bancarias y sube la imagen del comprobante para confirmar tu pago.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF5F5F5F),
                        lineHeight = 18.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    bankAccounts.forEach { account ->
                        TarjetaCuentaBancaria(
                            account = account,
                            isCopied = copiedAccountId == account.id,
                            onCopyAccount = {
                                clipboardManager.setText(AnnotatedString(account.accountNumber))
                                copiedAccountId = account.id
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                CargaComprobanteTransferencia(
                    paymentProofUri = paymentProofUri,
                    isCompact = isCompact,
                    onPickPaymentProof = onPickPaymentProof,
                    onRemovePaymentProof = onRemovePaymentProof
                )

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
