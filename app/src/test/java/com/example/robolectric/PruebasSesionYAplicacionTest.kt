package com.example

import com.example.data.remote.MobileSession
import com.example.data.repository.EleganRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasSesionYAplicacionTest {

    @Test
    fun `read string from context`() {
        val context = obtenerContextoPrueba()
        val appName = context.getString(R.string.app_name)
        assertEquals("Elegan", appName)
    }

    @Test
    fun `test launch MainActivity`() {
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())
    }

    @Test
    fun `test register and login local user`() {
        val context = obtenerContextoPrueba()
        val email = "test.cliente@gmail.com"
        val password = "Password123!"

        val regResponse = MobileSession.registerLocalUser(
            context = context,
            name = "Cliente Prueba",
            email = email,
            password = password
        )
        assertEquals(email, regResponse.user.email)
        assertEquals("customer", regResponse.user.role)

        val loginResponse = MobileSession.loginLocalUser(
            context = context,
            email = email,
            password = password
        )
        assertEquals(email, loginResponse.user.email)
        assertEquals("Cliente Prueba", loginResponse.user.name)
    }

    @Test
    fun `test super admin login gets admin role`() {
        val context = obtenerContextoPrueba()
        val adminResponse = MobileSession.loginLocalUser(
            context = context,
            email = "cg0220037@gmail.com",
            password = "EleganAdmin2026!"
        )
        assertEquals("admin", adminResponse.user.role)
        assertEquals("cg0220037@gmail.com", adminResponse.user.email)
    }

    @Test
    fun `test delete account removes user credentials and stored data`() = runBlocking {
        val context = obtenerContextoPrueba()
        MobileSession.clear(context)

        val email = "delete.cliente@gmail.com"
        val password = "DeletePass123!"
        MobileSession.registerLocalUser(
            context = context,
            name = "Cliente Temporal",
            email = email,
            password = password
        )

        val repo = EleganRepository(context)
        repo.saveUserProfile("809-555-9999", "Calle El Conde 10", "Santo Domingo")
        val sampleProduct = repo.getInitialSampleProducts().first()
        repo.addToCart(sampleProduct, "M", 1L, "Negro")
        repo.setProductSaved(sampleProduct.id, true)

        repo.deleteUserAccountData()

        assertNull(MobileSession.user(context))
        assertTrue(repo.observeCart().first().isEmpty())
        assertTrue(repo.observeSavedProductIds().first().isEmpty())
        assertNull(repo.observeUserProfile().first())

        var loginFailedAfterDelete = false
        try {
            MobileSession.loginLocalUser(context, email, password)
        } catch (_: IllegalArgumentException) {
            loginFailedAfterDelete = true
        }
        assertTrue(loginFailedAfterDelete)
    }
}
