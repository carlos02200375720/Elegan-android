package com.example

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.example.data.local.AlmacenBloqueoTransferencia
import com.example.data.local.AlmacenDataStoreElegan
import com.example.data.local.AlmacenPreferenciasUsuario
import com.example.data.local.EstadoBloqueoTransferencia
import com.example.data.model.BankAccountInfo
import com.example.data.remote.AlmacenCifradoSesion
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasDataStorePreferenciasYSesionTest {

    @Before
    fun setUp() = runBlocking {
        val context = obtenerContextoPrueba()
        MobileSession.clearAsync(context)
        AlmacenBloqueoTransferencia.limpiarAsync(context)
        AlmacenPreferenciasUsuario.limpiarPreferencias(context, conservarVisitorId = false)
    }

    @After
    fun tearDown() = runBlocking {
        val context = obtenerContextoPrueba()
        MobileSession.clearAsync(context)
        AlmacenBloqueoTransferencia.limpiarAsync(context)
        AlmacenPreferenciasUsuario.limpiarPreferencias(context, conservarVisitorId = false)
    }

    @Test
    fun `mobile session persists asynchronously in DataStore and emits reactive updates`() = runBlocking {
        val context = obtenerContextoPrueba()

        assertNull(MobileSession.observeUser(context).first())
        assertFalse(MobileSession.observeSession(context).first().isAuthenticated)

        val response = AuthResponse(
            token = "jwt_datastore_token_001",
            user = AuthUser(
                id = "usr-ds-100",
                email = "cliente.datastore@elegan.com",
                name = "Cliente DataStore",
                photoUrl = "https://cdn.elegan.com/avatar.jpg",
                role = "admin",
                isAdmin = true,
                provider = "google"
            )
        )

        MobileSession.saveAsync(context, response)

        val session = MobileSession.observeSession(context).first()
        assertTrue(session.isAuthenticated)
        assertEquals("jwt_datastore_token_001", session.token)
        assertEquals("usr-ds-100", session.userId)
        assertEquals("cliente.datastore@elegan.com", session.user?.email)
        assertEquals("Cliente DataStore", session.user?.name)
        assertEquals("admin", session.user?.role)
        assertEquals("google", session.user?.provider)

        // Verifica que el archivo binario .preferences_pb de Jetpack DataStore fue creado.
        val pbFile = context.applicationContext.preferencesDataStoreFile(AlmacenDataStoreElegan.SESSION_STORE_NAME)
        assertTrue(pbFile.isFile)
        assertTrue(pbFile.length() > 0L)

        MobileSession.clearAsync(context)
        assertNull(MobileSession.userAsync(context))
        assertNull(MobileSession.tokenAsync(context))
        assertFalse(MobileSession.observeSession(context).first().isAuthenticated)
    }

    @Test
    fun `session DataStore migrates legacy SharedPreferences without losing user or token`() = runBlocking {
        val context = obtenerContextoPrueba()
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val legacyPrefsName = "legacy_session_$suffix"
        val storeName = "migrated_session_$suffix"

        val legacyPrefs = context.applicationContext.getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putString("auth_token", "legacy_jwt_migrated_777")
            .putString("user_id", "usr-legacy-777")
            .putString("email", "migrado@elegan.com")
            .putString("name", "Usuario Migrado")
            .putString("photo_url", "https://cdn.elegan.com/migrado.png")
            .putString("role", "customer")
            .putString("provider", "password")
            .commit()

        val migratedStore = AlmacenDataStoreElegan.crearDataStoreAislado(
            context = context,
            storeName = storeName,
            legacySharedPrefsName = legacyPrefsName
        )

        val session = AlmacenCifradoSesion.observeSessionFrom(migratedStore).first()
        assertTrue(session.isAuthenticated)
        assertEquals("legacy_jwt_migrated_777", session.token)
        assertEquals("usr-legacy-777", session.userId)
        assertEquals("migrado@elegan.com", session.user?.email)
        assertEquals("Usuario Migrado", session.user?.name)
    }

    @Test
    fun `user preferences DataStore manages visitorId categories guest mode and preserves visitorId on reset`() = runBlocking {
        val context = obtenerContextoPrueba()

        val visitorId1 = AlmacenPreferenciasUsuario.obtenerOGenerarVisitorId(context)
        val visitorId2 = AlmacenPreferenciasUsuario.obtenerOGenerarVisitorId(context)
        assertTrue(visitorId1.isNotBlank())
        assertEquals(visitorId1, visitorId2)

        AlmacenPreferenciasUsuario.guardarUltimaCategoria(context, "camisas-sartoriales")
        AlmacenPreferenciasUsuario.guardarModoInvitado(context, true)
        AlmacenPreferenciasUsuario.guardarNotificacionesActivas(context, false)
        AlmacenPreferenciasUsuario.guardarUltimaSincronizacionCatalogo(context, 1_735_000_000_000L)

        val prefs = AlmacenPreferenciasUsuario.observarPreferencias(context).first()
        assertEquals(visitorId1, prefs.visitorId)
        assertEquals("camisas-sartoriales", prefs.lastSelectedCategory)
        assertTrue(prefs.isGuestMode)
        assertFalse(prefs.notificationsEnabled)
        assertEquals(1_735_000_000_000L, prefs.lastCatalogSyncMillis)

        AlmacenPreferenciasUsuario.limpiarPreferencias(context, conservarVisitorId = true)
        val afterPartialClear = AlmacenPreferenciasUsuario.leerPreferencias(context)
        assertEquals(visitorId1, afterPartialClear.visitorId)
        assertNull(afterPartialClear.lastSelectedCategory)
        assertFalse(afterPartialClear.isGuestMode)
        assertTrue(afterPartialClear.notificationsEnabled)
    }

    @Test
    fun `user preferences DataStore migrates legacy visitor_id from SharedPreferences`() = runBlocking {
        val context = obtenerContextoPrueba()
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val legacyPrefsName = "legacy_visitor_$suffix"
        val storeName = "migrated_prefs_$suffix"

        context.applicationContext
            .getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("visitor_id", "visitor-legacy-abc-123")
            .commit()

        val migratedStore = AlmacenDataStoreElegan.crearDataStoreAislado(
            context = context,
            storeName = storeName,
            legacySharedPrefsName = legacyPrefsName
        )

        val migratedId = AlmacenPreferenciasUsuario.obtenerOGenerarVisitorIdDesde(migratedStore)
        assertEquals("visitor-legacy-abc-123", migratedId)
    }

    @Test
    fun `transfer lock DataStore persists asynchronously and migrates legacy plaintext SharedPreferences`() = runBlocking {
        val context = obtenerContextoPrueba()
        val account = BankAccountInfo(
            id = "acc-ds-1",
            bankName = "Banco Reservas",
            accountNumber = "9600123456",
            accountType = "Ahorros",
            holderName = "Elegan SRL",
            holderDocument = "131000001"
        )
        val state = EstadoBloqueoTransferencia(
            userId = "usr-ds-lock",
            subtotal = 120.0,
            discount = 10.0,
            shipping = 15.0,
            total = 125.0,
            couponCode = "ELEGAN10",
            customerName = "Carlos DataStore",
            email = "carlos@elegan.com",
            phone = "8095550100",
            address = "Av. Winston Churchill",
            city = "Santo Domingo",
            selectedAccountId = account.id,
            pendingTransferOrderId = "ORD-DS-555",
            bankAccounts = listOf(account)
        )

        AlmacenBloqueoTransferencia.guardarAsync(context, state)
        assertTrue(AlmacenBloqueoTransferencia.tieneBloqueoActivoAsync(context, "usr-ds-lock"))
        assertFalse(AlmacenBloqueoTransferencia.tieneBloqueoActivoAsync(context, "otro-usuario"))

        val observed = AlmacenBloqueoTransferencia.observarBloqueo(context, "usr-ds-lock").first()
        assertNotNull(observed)
        assertEquals(125.0, observed!!.total, 0.001)
        assertEquals("ORD-DS-555", observed.pendingTransferOrderId)
        assertEquals(1, observed.bankAccounts.size)

        AlmacenBloqueoTransferencia.limpiarAsync(context)
        assertNull(AlmacenBloqueoTransferencia.cargarAsync(context, "usr-ds-lock"))

        // Verifica migración desde SharedPreferences heredado en texto plano hacia DataStore.
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val legacyPrefsName = "legacy_lock_$suffix"
        val storeName = "migrated_lock_$suffix"

        context.applicationContext
            .getSharedPreferences(legacyPrefsName, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("is_active", true)
            .putString("user_id", "usr-legacy-lock")
            .putString("total", "89.5")
            .putString("customer_name", "Cliente Heredado")
            .putString("selected_account_id", "acc-legacy")
            .putString(
                "bank_accounts_json",
                """[{"id":"acc-legacy","bankName":"BHD","accountNumber":"111222","accountType":"Corriente","holderName":"Elegan","holderDocument":"001"}]"""
            )
            .commit()

        val migratedLockStore = AlmacenDataStoreElegan.crearDataStoreAislado(
            context = context,
            storeName = storeName,
            legacySharedPrefsName = legacyPrefsName
        )

        val migratedLock = AlmacenBloqueoTransferencia.observarBloqueoDesde(
            context = context,
            dataStore = migratedLockStore,
            expectedUserId = "usr-legacy-lock"
        ).first()
        assertNotNull(migratedLock)
        assertEquals(89.5, migratedLock!!.total, 0.001)
        assertEquals("Cliente Heredado", migratedLock.customerName)
        assertEquals("BHD", migratedLock.bankAccounts.first().bankName)
    }

    @Test
    fun `corrupted DataStore file recovers gracefully with empty preferences`() = runBlocking {
        val context = obtenerContextoPrueba()
        val storeName = "corrupted_store_${UUID.randomUUID().toString().replace("-", "").take(8)}"
        val file = context.applicationContext.preferencesDataStoreFile(storeName)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(0x00, 0xFF.toByte(), 0x7F, 0x11, 0x22, 0x33, 0x44))

        val recoveredStore = AlmacenDataStoreElegan.crearDataStoreAislado(
            context = context,
            storeName = storeName
        )

        val prefs = AlmacenPreferenciasUsuario.observarPreferenciasDesde(recoveredStore).first()
        assertEquals("", prefs.visitorId)
        assertNull(prefs.lastSelectedCategory)
        assertFalse(prefs.isGuestMode)
    }
}
