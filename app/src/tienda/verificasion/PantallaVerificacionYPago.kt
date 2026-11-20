package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.UserProfile
import kotlinx.coroutines.launch

@Composable
fun VerificasionScreen(
    cartItems: List<CartItem>, subtotal: Double, discount: Double, shipping: Double, total: Double,
    userProfile: UserProfile?, bankAccounts: List<BankAccountInfo> = emptyList(),
    isSubmittingPayment: Boolean = false, paymentError: String? = null,
    onLoadBankAccounts: () -> Unit = {},
    onSubmitBankTransfer: (String, Uri, String, String, String, String, String) -> Unit,
    onSubmitCashOnDelivery: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val estado = rememberEstadoFormularioVerificacion(userProfile)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var proofUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAccountId by remember { mutableStateOf("") }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { proofUri = it }

    fun validateDelivery(): Boolean {
        if (!estado.isDeliveryFormValid() || estado.phone.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Completa nombre, teléfono y dirección de entrega.") }
            return false
        }
        return true
    }

    BackHandler(enabled = estado.showTransferPage || estado.showCashOnDeliveryPage) {
        estado.showTransferPage = false
        estado.showCashOnDeliveryPage = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            estado.showTransferPage -> TranfereciaScreen(
                total = total, bankAccounts = bankAccounts, paymentProofUri = proofUri,
                selectedAccountId = selectedAccountId, isSubmitting = isSubmittingPayment,
                onBack = { estado.showTransferPage = false },
                onPickPaymentProof = { imagePicker.launch("image/*") },
                onRemovePaymentProof = { proofUri = null },
                onSelectAccount = { selectedAccountId = it },
                onConfirmPayment = {
                    val uri = proofUri
                    if (!validateDelivery()) return@TranfereciaScreen
                    if (bankAccounts.isEmpty()) { scope.launch { snackbarHostState.showSnackbar("No hay cuentas bancarias disponibles.") }; return@TranfereciaScreen }
                    if (selectedAccountId.isBlank()) { scope.launch { snackbarHostState.showSnackbar("Selecciona la cuenta a la que realizaste la transferencia.") }; return@TranfereciaScreen }
                    if (uri == null) { scope.launch { snackbarHostState.showSnackbar("Sube la imagen del comprobante.") }; return@TranfereciaScreen }
                    onSubmitBankTransfer(selectedAccountId, uri, estado.customerName, estado.email, estado.phone, estado.address, estado.city)
                }
            )
            estado.showCashOnDeliveryPage -> PantallaContraEntrega(
                total = total, isSubmitting = isSubmittingPayment,
                onBack = { estado.showCashOnDeliveryPage = false },
                onAccept = {
                    if (validateDelivery()) onSubmitCashOnDelivery(estado.customerName, estado.email, estado.phone, estado.address, estado.city)
                }
            )
            else -> Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                topBar = { CabeceraVerificacion(onBack = onBack) },
                bottomBar = {
                    BarraInferiorConfirmarPago(total = total, isSubmitting = isSubmittingPayment, onConfirmClick = {
                        if (validateDelivery()) {
                            if (estado.selectedPayment == OPCION_PAGO_TRANSFERENCIA) {
                                proofUri = null; selectedAccountId = ""; onLoadBankAccounts(); estado.showTransferPage = true
                            } else {
                                estado.showCashOnDeliveryPage = true
                            }
                        }
                    })
                },
                containerColor = Color.White
            ) { padding ->
                BoxWithConstraints(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
                    val horizontalPad = if (maxWidth < 350.dp) 12.dp else if (maxWidth >= 600.dp) 24.dp else 16.dp
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 680.dp).testTag("verificasion_screen"),
                        contentPadding = PaddingValues(horizontal = horizontalPad, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { BannerSeguridadSsl() }
                        item { SeccionDireccionEntrega(estado.customerName, { estado.customerName = it }, estado.email, { estado.email = it }, estado.phone, { estado.phone = it }, estado.address, { estado.address = it }, estado.city, { estado.city = it }) }
                        item { HorizontalDivider(color = Color(0xFFF0EAE4)) }
                        item { SeccionResumenArticulos(cartItems, subtotal, discount, shipping, total) }
                        item { HorizontalDivider(color = Color(0xFFF0EAE4)) }
                        item {
                            SeccionMetodoPago(
                                paymentOptions = estado.paymentOptions,
                                selectedPayment = estado.selectedPayment,
                                onSelectOption = { estado.selectedPayment = it },
                                onOpenSelectedAction = { selected ->
                                    if (!validateDelivery()) return@SeccionMetodoPago
                                    if (selected == OPCION_PAGO_TRANSFERENCIA) { onLoadBankAccounts(); estado.showTransferPage = true }
                                    else estado.showCashOnDeliveryPage = true
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
        if (!paymentError.isNullOrBlank()) {
            androidx.compose.material3.Text(paymentError, color = Color(0xFFB00020), modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
        }
    }
}
