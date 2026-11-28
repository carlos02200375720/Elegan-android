package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.AlmacenBloqueoTransferencia
import com.example.data.local.EstadoBloqueoTransferencia
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.UserProfile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VerificasionScreen(
    cartItems: List<CartItem>, subtotal: Double, discount: Double, shipping: Double, total: Double,
    cashOnDeliveryEnabled: Boolean = false, cashOnDeliveryMessage: String = "",
    userProfile: UserProfile?, bankAccounts: List<BankAccountInfo> = emptyList(),
    currentUserId: String = "",
    sessionUserName: String = "",
    sessionUserEmail: String = "",
    isSubmittingPayment: Boolean = false, paymentError: String? = null,
    onDismissPaymentError: () -> Unit = {},
    couponCode: String = "",
    pendingTransferOrderId: String? = null,
    restoredTransferLock: EstadoBloqueoTransferencia? = null,
    onTransferLockChanged: (EstadoBloqueoTransferencia) -> Unit = {},
    onTransferLockExitByHeaderBack: () -> Unit = {},
    onLoadBankAccounts: () -> Unit = {},
    onSubmitBankTransfer: (String, Uri, String, String, String, String, String) -> Unit,
    onSubmitCashOnDelivery: (String, String, String, String, String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val estado = rememberEstadoFormularioVerificacion(
        userProfile = userProfile,
        restoredTransferLock = restoredTransferLock,
        sessionUserName = sessionUserName,
        sessionUserEmail = sessionUserEmail
    )
    LaunchedEffect(cashOnDeliveryEnabled) {
        if (!cashOnDeliveryEnabled) {
            estado.showCashOnDeliveryPage = false
            if (estado.selectedPayment == OPCION_PAGO_CONTRA_ENTREGA) {
                estado.selectedPayment = OPCION_PAGO_TRANSFERENCIA
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var paymentProofFilePath by remember { mutableStateOf(restoredTransferLock?.paymentProofFilePath) }
    var proofUri by remember {
        mutableStateOf<Uri?>(
            restoredTransferLock?.paymentProofFilePath
                ?.let { path -> File(path).takeIf { it.isFile && it.length() > 0L }?.let { Uri.fromFile(it) } }
        )
    }
    var selectedAccountId by remember { mutableStateOf(restoredTransferLock?.selectedAccountId.orEmpty()) }
    var isLocalErrorDismissed by remember(paymentError) { mutableStateOf(false) }

    fun dismissPaymentError() {
        isLocalErrorDismissed = true
        onDismissPaymentError()
    }

    fun buildTransferLockState(
        accountId: String = selectedAccountId,
        proofPath: String? = paymentProofFilePath,
        accounts: List<BankAccountInfo> = bankAccounts,
        orderId: String? = pendingTransferOrderId
    ): EstadoBloqueoTransferencia = EstadoBloqueoTransferencia(
        userId = currentUserId.ifBlank { userProfile?.userId.orEmpty() },
        subtotal = subtotal,
        discount = discount,
        shipping = shipping,
        total = total,
        couponCode = couponCode,
        customerName = estado.customerName,
        email = estado.email,
        phone = estado.phone,
        address = estado.address,
        city = estado.city,
        selectedAccountId = accountId,
        pendingTransferOrderId = orderId,
        paymentProofFilePath = proofPath,
        bankAccounts = accounts
    )

    fun persistTransferLock(
        accountId: String = selectedAccountId,
        proofPath: String? = paymentProofFilePath
    ) {
        val lockState = buildTransferLockState(accountId = accountId, proofPath = proofPath)
        AlmacenBloqueoTransferencia.guardar(context, lockState)
        onTransferLockChanged(lockState)
    }

    fun abrirPantallaTransferencia(resetDraft: Boolean) {
        if (resetDraft) {
            AlmacenBloqueoTransferencia.eliminarCopiaComprobante(context)
            proofUri = null
            paymentProofFilePath = null
            selectedAccountId = ""
        }
        onLoadBankAccounts()
        estado.showTransferPage = true
        persistTransferLock(
            accountId = if (resetDraft) "" else selectedAccountId,
            proofPath = if (resetDraft) null else paymentProofFilePath
        )
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { pickedUri ->
        if (pickedUri != null) {
            proofUri = pickedUri
            scope.launch {
                val copiedPath = withContext(Dispatchers.IO) {
                    AlmacenBloqueoTransferencia.guardarCopiaComprobante(context, pickedUri)
                }
                if (copiedPath != null) {
                    paymentProofFilePath = copiedPath
                    proofUri = Uri.fromFile(File(copiedPath))
                }
                if (estado.showTransferPage && AlmacenBloqueoTransferencia.tieneBloqueoActivo(context)) {
                    persistTransferLock(proofPath = copiedPath ?: paymentProofFilePath)
                }
            }
        }
    }

    LaunchedEffect(estado.showTransferPage) {
        if (estado.showTransferPage) {
            onLoadBankAccounts()
        }
    }

    LaunchedEffect(estado.showTransferPage, bankAccounts, pendingTransferOrderId) {
        if (estado.showTransferPage && AlmacenBloqueoTransferencia.tieneBloqueoActivo(context)) {
            persistTransferLock()
        }
    }

    fun validateDelivery(): Boolean {
        if (!estado.isDeliveryFormValid()) {
            val mensaje = estado.mensajeCampoFaltante()
            scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                snackbarHostState.showSnackbar(mensaje)
            }
            return false
        }
        return true
    }

    // Solo contra entrega permite regresar con el botón atrás del sistema.
    // Transferencia bloquea el botón atrás del sistema dentro de TranfereciaScreen.
    BackHandler(enabled = estado.showCashOnDeliveryPage && !estado.showTransferPage) {
        dismissPaymentError()
        estado.showCashOnDeliveryPage = false
    }

    val pasoActual = when {
        estado.showTransferPage -> PasoVerificacion.TRANSFERENCIA
        estado.showCashOnDeliveryPage -> PasoVerificacion.CONTRA_ENTREGA
        else -> PasoVerificacion.FORMULARIO
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = pasoActual,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val entrandoSubpantalla = targetState != PasoVerificacion.FORMULARIO
                if (entrandoSubpantalla) {
                    ContentTransform(
                        targetContentEnter = slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 320)
                        ),
                        initialContentExit = fadeOut(
                            animationSpec = tween(durationMillis = 320),
                            targetAlpha = 0.99f
                        ),
                        targetContentZIndex = 1f,
                        sizeTransform = SizeTransform(clip = false)
                    )
                } else {
                    ContentTransform(
                        targetContentEnter = fadeIn(
                            animationSpec = tween(durationMillis = 280),
                            initialAlpha = 0.99f
                        ),
                        initialContentExit = slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(durationMillis = 280)
                        ),
                        targetContentZIndex = -1f,
                        sizeTransform = SizeTransform(clip = false)
                    )
                }
            },
            label = "verificasion_subpage_transition"
        ) { paso ->
            when (paso) {
                PasoVerificacion.TRANSFERENCIA -> TranfereciaScreen(
                total = total, bankAccounts = bankAccounts, paymentProofUri = proofUri,
                selectedAccountId = selectedAccountId, isSubmitting = isSubmittingPayment,
                onBack = {
                    if (!isSubmittingPayment) {
                        estado.showTransferPage = false
                        proofUri = null
                        paymentProofFilePath = null
                        selectedAccountId = ""
                        AlmacenBloqueoTransferencia.limpiar(context)
                        onTransferLockExitByHeaderBack()
                    }
                },
                onPickPaymentProof = {
                    dismissPaymentError()
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemovePaymentProof = {
                    proofUri = null
                    paymentProofFilePath = null
                    AlmacenBloqueoTransferencia.eliminarCopiaComprobante(context)
                    persistTransferLock(proofPath = null)
                },
                onSelectAccount = { accountId ->
                    dismissPaymentError()
                    selectedAccountId = accountId
                    persistTransferLock(accountId = accountId)
                },
                onConfirmPayment = {
                    val uri = proofUri
                    if (!validateDelivery()) return@TranfereciaScreen
                    if (bankAccounts.isEmpty()) {
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("No hay cuentas bancarias disponibles. Pulsa reintentar para cargarlas.")
                        }
                        return@TranfereciaScreen
                    }
                    if (selectedAccountId.isBlank()) {
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Selecciona la cuenta a la que realizaste la transferencia.")
                        }
                        return@TranfereciaScreen
                    }
                    if (uri == null) {
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Sube la imagen del comprobante.")
                        }
                        return@TranfereciaScreen
                    }
                    dismissPaymentError()
                    onSubmitBankTransfer(selectedAccountId, uri, estado.customerName, estado.email, estado.phone, estado.address, estado.city)
                },
                onRetryLoadAccounts = {
                    dismissPaymentError()
                    onLoadBankAccounts()
                }
            )
                PasoVerificacion.CONTRA_ENTREGA -> ContraEntregaScreen(
                total = total,
                message = cashOnDeliveryMessage,
                customerName = estado.customerName,
                phone = estado.phone,
                address = estado.address,
                city = estado.city,
                itemCount = cartItems.sumOf { it.quantity },
                isSubmitting = isSubmittingPayment,
                onBack = {
                    dismissPaymentError()
                    estado.showCashOnDeliveryPage = false
                },
                onConfirmOrder = {
                    if (validateDelivery()) {
                        dismissPaymentError()
                        onSubmitCashOnDelivery(estado.customerName, estado.email, estado.phone, estado.address, estado.city)
                    }
                }
            )
                PasoVerificacion.FORMULARIO -> Scaffold(
                contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                topBar = { CabeceraVerificacion(onBack = onBack) },
                bottomBar = {
                    BarraInferiorConfirmarPago(total = total, isSubmitting = isSubmittingPayment, onConfirmClick = {
                        if (validateDelivery()) {
                            dismissPaymentError()
                            if (estado.selectedPayment == OPCION_PAGO_TRANSFERENCIA) {
                                abrirPantallaTransferencia(resetDraft = true)
                            } else if (cashOnDeliveryEnabled) {
                                estado.showCashOnDeliveryPage = true
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("El pago contra entrega no está disponible para los productos seleccionados.") }
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
                                paymentOptions = estado.paymentOptions.filter { it != OPCION_PAGO_CONTRA_ENTREGA || cashOnDeliveryEnabled },
                                selectedPayment = estado.selectedPayment,
                                onSelectOption = { estado.selectedPayment = it },
                                onOpenSelectedAction = { selected ->
                                    if (!validateDelivery()) return@SeccionMetodoPago
                                    if (selected == OPCION_PAGO_TRANSFERENCIA) abrirPantallaTransferencia(resetDraft = false)
                                    else estado.showCashOnDeliveryPage = true
                                }
                            )
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
        }
        BannerErrorPago(
            message = paymentError,
            visible = !paymentError.isNullOrBlank() && !isLocalErrorDismissed,
            showRetry = estado.showTransferPage && bankAccounts.isEmpty(),
            retryLabel = "Reintentar cargar cuentas",
            onRetry = {
                dismissPaymentError()
                onLoadBankAccounts()
            },
            onDismiss = { dismissPaymentError() },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 58.dp, start = 14.dp, end = 14.dp)
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp, start = 12.dp, end = 12.dp)
        )
    }
}

private enum class PasoVerificacion {
    FORMULARIO,
    TRANSFERENCIA,
    CONTRA_ENTREGA
}
