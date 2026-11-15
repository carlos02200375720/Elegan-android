package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EleganBurgundy
import com.example.ui.theme.EleganCharcoal
import com.example.ui.theme.EleganGold
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun CardPaymentScreen(
    total: Double,
    customerName: String,
    customerEmail: String,
    itemCount: Long,
    itemsSummary: String,
    isSubmittingOrder: Boolean,
    onBack: () -> Unit,
    onPaymentSuccess: (paymentMethodLabel: String, transactionId: String) -> Unit
) {
    BackHandler(enabled = !isSubmittingOrder) { onBack() }

    var cardHolder by remember(customerName) { mutableStateOf(customerName) }
    var cardNumberDigits by remember { mutableStateOf("") }
    var cardExpiryDigits by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val amountFormatted = remember(total) {
        String.format(Locale.US, "%.2f", total.coerceAtLeast(0.01))
    }

    val cardBrand = remember(cardNumberDigits) {
        when {
            cardNumberDigits.startsWith("4") -> "Visa"
            cardNumberDigits.startsWith("5") || cardNumberDigits.startsWith("2") -> "Mastercard"
            cardNumberDigits.startsWith("3") -> "American Express"
            else -> "Tarjeta"
        }
    }

    val formattedCardNumber = remember(cardNumberDigits) {
        cardNumberDigits.chunked(4).joinToString(" ")
    }

    val formattedExpiry = remember(cardExpiryDigits) {
        if (cardExpiryDigits.length <= 2) cardExpiryDigits
        else "${cardExpiryDigits.take(2)}/${cardExpiryDigits.drop(2)}"
    }

    fun submitLocalCardPayment() {
        val holder = cardHolder.trim()
        if (holder.length < 3) {
            scope.launch {
                snackbarHostState.showSnackbar("Ingresa el nombre del titular de la tarjeta.")
            }
            return
        }
        if (cardNumberDigits.length < 13) {
            scope.launch {
                snackbarHostState.showSnackbar("Ingresa un número de tarjeta válido (mínimo 13 dígitos).")
            }
            return
        }
        if (cardExpiryDigits.length < 4) {
            scope.launch {
                snackbarHostState.showSnackbar("Ingresa la fecha de vencimiento en formato MM/AA.")
            }
            return
        }
        val month = cardExpiryDigits.take(2).toIntOrNull() ?: 0
        if (month !in 1..12) {
            scope.launch {
                snackbarHostState.showSnackbar("El mes de vencimiento debe estar entre 01 y 12.")
            }
            return
        }
        if (cardCvv.length < 3) {
            scope.launch {
                snackbarHostState.showSnackbar("Ingresa un código CVV válido de 3 o 4 dígitos.")
            }
            return
        }
        val last4 = cardNumberDigits.takeLast(4)
        val localTxId = "ELEGAN-${System.currentTimeMillis().toString().takeLast(6)}"
        onPaymentSuccess("$cardBrand •••• $last4", localTxId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("card_payment_screen"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                            .padding(start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (!isSubmittingOrder) onBack() },
                            modifier = Modifier.testTag("card_payment_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = EleganCharcoal
                            )
                        }

                        Text(
                            text = "Pago con Tarjeta",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal,
                                fontFamily = FontFamily.Serif,
                                fontSize = 17.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF9F3EA),
                            modifier = Modifier.testTag("card_payment_header_amount")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Total a pagar",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF6B6560),
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp
                                    )
                                )
                                Text(
                                    text = "$$amountFormatted",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganBurgundy,
                                        fontSize = 13.sp,
                                        lineHeight = 15.sp
                                    )
                                )
                            }
                        }
                    }
                }
                EleganTopBarShadow()
            }
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                EleganBottomBarShadow()
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        val isCompactBar = maxWidth < 350.dp
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 680.dp)
                                .padding(horizontal = if (isCompactBar) 12.dp else 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onBack,
                                enabled = !isSubmittingOrder,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = if (isCompactBar) 10.dp else 14.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, EleganBurgundy.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = EleganBurgundy),
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .testTag("card_payment_cancel_btn")
                            ) {
                                Text(
                                    text = "Cambiar",
                                    fontSize = if (isCompactBar) 12.sp else 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }

                            Button(
                                onClick = ::submitLocalCardPayment,
                                enabled = !isSubmittingOrder,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EleganBurgundy,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("card_payment_confirm_btn")
                            ) {
                                if (isSubmittingOrder) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pagar $$amountFormatted",
                                        fontSize = if (isCompactBar) 13.sp else 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White),
            contentAlignment = Alignment.TopCenter
        ) {
            val isCompact = maxWidth < 350.dp
            val isWide = maxWidth >= 600.dp
            val horizontalPad = when {
                isCompact -> 12.dp
                isWide -> 24.dp
                else -> 16.dp
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPad, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFAF8F5),
                    border = BorderStroke(1.dp, Color(0xFFE5DDD5)),
                    modifier = Modifier.fillMaxWidth()
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
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Checkout Local Seguro",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EleganCharcoal
                                    )
                                )
                            }
                            Text(
                                text = "$itemCount art.",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = EleganBurgundy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (customerName.isNotBlank() || customerEmail.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = listOf(customerName, customerEmail).filter { it.isNotBlank() }.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF5F5F5F),
                                    fontSize = 12.sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (itemsSummary.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = itemsSummary,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF7A736D),
                                    fontSize = 11.5.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = EleganGold.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = EleganCharcoal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Visa / Mastercard / Débito • Procesamiento en el dispositivo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EleganCharcoal
                                )
                            }
                        }
                    }
                }

                // Vista previa elegante de la tarjeta
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EleganCharcoal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ELEGAN CLUB",
                                color = EleganGold,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.5.sp
                            )
                            Text(
                                text = cardBrand.uppercase(Locale.ROOT),
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = if (formattedCardNumber.isNotBlank()) formattedCardNumber else "•••• •••• •••• ••••",
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            letterSpacing = 1.2.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TITULAR",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = cardHolder.ifBlank { "NOMBRE DEL CLIENTE" }.uppercase(Locale.ROOT),
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "VENCE",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = formattedExpiry.ifBlank { "MM/AA" },
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = cardHolder,
                    onValueChange = { cardHolder = it },
                    label = { Text("Nombre en la Tarjeta *") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EleganBurgundy,
                        unfocusedBorderColor = Color(0xFFDDD5CE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_holder_input")
                )

                OutlinedTextField(
                    value = formattedCardNumber,
                    onValueChange = { input ->
                        cardNumberDigits = input.filter { it.isDigit() }.take(16)
                    },
                    label = { Text("Número de Tarjeta *") },
                    placeholder = { Text("0000 0000 0000 0000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EleganBurgundy,
                        unfocusedBorderColor = Color(0xFFDDD5CE)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("card_number_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = formattedExpiry,
                        onValueChange = { input ->
                            cardExpiryDigits = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("Vence (MM/AA) *") },
                        placeholder = { Text("12/28") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_expiry_input")
                    )

                    OutlinedTextField(
                        value = cardCvv,
                        onValueChange = { input ->
                            cardCvv = input.filter { it.isDigit() }.take(4)
                        },
                        label = { Text("CVV *") },
                        placeholder = { Text("123") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EleganBurgundy,
                            unfocusedBorderColor = Color(0xFFDDD5CE)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("card_cvv_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isSubmittingOrder) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.88f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = EleganBurgundy,
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Pago aprobado. Registrando tu pedido...",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EleganCharcoal
                            )
                        )
                    }
                }
            }
        }
    }
}
