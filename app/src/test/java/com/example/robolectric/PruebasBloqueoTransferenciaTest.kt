package com.example

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.BankAccountInfo
import com.example.data.model.CartItem
import com.example.data.model.UserProfile
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import com.example.ui.AlmacenBloqueoTransferencia
import com.example.ui.EstadoBloqueoTransferencia
import com.example.ui.VerificasionScreen
import com.example.ui.theme.EleganTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasBloqueoTransferenciaTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleAccount = BankAccountInfo(
        id = "acc-pop-1",
        bankName = "Banco popular",
        accountNumber = "849431325",
        accountType = "Corriente",
        holderName = "Carlos David Garcia Batista",
        holderDocument = "02200375729"
    )

    private val sampleProfile = UserProfile(
        displayName = "Carlos David",
        email = "carlos@example.com",
        phone = "8095550199",
        address = "Calle Principal #10",
        city = "Santo Domingo"
    )

    @Before
    fun setUp() {
        val context = obtenerContextoPrueba()
        AlmacenBloqueoTransferencia.limpiar(context)
        MobileSession.clear(context)
        MobileSession.save(
            context,
            AuthResponse(
                token = "backend_transfer_lock_token",
                user = AuthUser(
                    id = "user-lock-001",
                    email = "carlos@example.com",
                    name = "Carlos David"
                )
            )
        )
    }

    @After
    fun tearDown() {
        val context = obtenerContextoPrueba()
        AlmacenBloqueoTransferencia.limpiar(context)
        MobileSession.clear(context)
    }

    @Test
    fun `transfer lock persists full checkout draft bank accounts and local proof file across restarts`() {
        val context = obtenerContextoPrueba()
        val tempProof = File.createTempFile("proof_source_", ".jpg", context.cacheDir)
        tempProof.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        try {
            val copiedPath = AlmacenBloqueoTransferencia.guardarCopiaComprobante(context, Uri.fromFile(tempProof))
            assertNotNull(copiedPath)
            assertTrue(File(copiedPath!!).isFile)

            val saved = EstadoBloqueoTransferencia(
                userId = "user-lock-001",
                subtotal = 55.0,
                discount = 0.0,
                shipping = 0.0,
                total = 55.0,
                couponCode = "VIP10",
                customerName = "Carlos David",
                email = "carlos@example.com",
                phone = "8095550199",
                address = "Calle Principal #10",
                city = "Santo Domingo",
                selectedAccountId = sampleAccount.id,
                pendingTransferOrderId = "ORD-777",
                paymentProofFilePath = copiedPath,
                bankAccounts = listOf(sampleAccount)
            )
            AlmacenBloqueoTransferencia.guardar(context, saved)

            assertTrue(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))
            val loaded = AlmacenBloqueoTransferencia.cargar(context, "user-lock-001")
            assertNotNull(loaded)
            assertEquals(55.0, loaded!!.total, 0.001)
            assertEquals("VIP10", loaded.couponCode)
            assertEquals(sampleAccount.id, loaded.selectedAccountId)
            assertEquals("ORD-777", loaded.pendingTransferOrderId)
            assertEquals(copiedPath, loaded.paymentProofFilePath)
            assertEquals(1, loaded.bankAccounts.size)
            assertEquals("849431325", loaded.bankAccounts.first().accountNumber)

            AlmacenBloqueoTransferencia.limpiar(context)
            assertFalse(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))
            assertNull(AlmacenBloqueoTransferencia.cargar(context, "user-lock-001"))
            assertFalse(File(copiedPath).exists())
        } finally {
            tempProof.delete()
        }
    }

    @Test
    fun `system back button cannot exit transfer screen but header back arrow unlocks and returns`() {
        val context = obtenerContextoPrueba()
        var headerExitTriggered = false

        composeTestRule.setContent {
            EleganTheme {
                VerificasionScreen(
                    cartItems = listOf(CartItem(id = "c1", productId = "p1", name = "Camisa", price = 55.0, quantity = 1)),
                    subtotal = 55.0,
                    discount = 0.0,
                    shipping = 0.0,
                    total = 55.0,
                    userProfile = sampleProfile,
                    bankAccounts = listOf(sampleAccount),
                    onTransferLockExitByHeaderBack = { headerExitTriggered = true },
                    onSubmitBankTransfer = { _, _, _, _, _, _, _ -> },
                    onSubmitCashOnDelivery = { _, _, _, _, _ -> },
                    onBack = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_order_btn").performClick()
        composeTestRule.waitForIdle()

        // Debe estar en la pantalla de Transferencia y el bloqueo persistido activo
        composeTestRule.onNodeWithTag("tranferecia_screen").assertIsDisplayed()
        assertTrue(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))

        // Presionar el botón atrás del sistema NO debe sacar al cliente ni limpiar el bloqueo
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tranferecia_screen").assertIsDisplayed()
        assertFalse(headerExitTriggered)
        assertTrue(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))

        // Hacer clic en la flecha de regreso de la cabecera SÍ debe salir y limpiar el bloqueo
        composeTestRule.onNodeWithTag("tranferecia_back_btn").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tranferecia_screen").assertDoesNotExist()
        composeTestRule.onNodeWithTag("verificasion_screen").assertIsDisplayed()
        assertTrue(headerExitTriggered)
        assertFalse(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))
    }

    @Test
    fun `reopening app with active transfer lock lands directly on transfer screen with saved amount and account`() {
        val context = obtenerContextoPrueba()
        AlmacenBloqueoTransferencia.guardar(
            context,
            EstadoBloqueoTransferencia(
                userId = "user-lock-001",
                subtotal = 55.0,
                discount = 0.0,
                shipping = 0.0,
                total = 55.0,
                customerName = "Carlos David",
                email = "carlos@example.com",
                phone = "8095550199",
                address = "Calle Principal #10",
                city = "Santo Domingo",
                selectedAccountId = sampleAccount.id,
                bankAccounts = listOf(sampleAccount)
            )
        )

        composeTestRule.setContent {
            EleganTheme {
                AppContent(
                    isAuthenticated = true,
                    onSignOut = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        // Al reabrir la app debe abrir directamente en la pantalla de transferencia
        composeTestRule.onNodeWithTag("tranferecia_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("RD$ 55.00").assertIsDisplayed()
        composeTestRule.onNodeWithText("Banco popular").assertIsDisplayed()
        composeTestRule.onNodeWithText("849431325").assertIsDisplayed()

        // El botón atrás del sistema sigue bloqueado después de reabrir la app
        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tranferecia_screen").assertIsDisplayed()
        assertTrue(AlmacenBloqueoTransferencia.tieneBloqueoActivo(context, "user-lock-001"))
    }

    @Test
    fun `transfer account card allows copying account number holder name and document number`() {
        val context = obtenerContextoPrueba()
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager

        composeTestRule.setContent {
            EleganTheme {
                com.example.ui.TranfereciaScreen(
                    total = 105.0,
                    bankAccounts = listOf(sampleAccount),
                    paymentProofUri = null,
                    selectedAccountId = sampleAccount.id,
                    isSubmitting = false,
                    onBack = {},
                    onPickPaymentProof = {},
                    onRemovePaymentProof = {},
                    onSelectAccount = {},
                    onConfirmPayment = {}
                )
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("copy_account_${sampleAccount.id}").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("849431325", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        composeTestRule.onNodeWithTag("copy_holder_name_${sampleAccount.id}").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("Carlos David Garcia Batista", clipboard.primaryClip?.getItemAt(0)?.text?.toString())

        composeTestRule.onNodeWithTag("copy_holder_document_${sampleAccount.id}").performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("02200375729", clipboard.primaryClip?.getItemAt(0)?.text?.toString())
    }
}
