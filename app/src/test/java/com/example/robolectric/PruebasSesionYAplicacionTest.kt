package com.example

import com.example.data.model.ProductJson
import com.example.data.model.productId
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
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
    fun `session cache stores only a backend token and user identity`() {
        val context = obtenerContextoPrueba()
        MobileSession.clear(context)

        val response = AuthResponse(
            token = "backend_test_token",
            user = AuthUser(
                id = "user-test-001",
                email = "test.cliente@gmail.com",
                name = "Cliente Prueba",
                role = "customer",
                provider = "password"
            )
        )
        MobileSession.save(context, response)

        assertEquals("backend_test_token", MobileSession.token(context))
        assertEquals("user-test-001", MobileSession.userId(context))
        assertEquals("test.cliente@gmail.com", MobileSession.user(context)?.email)
    }

    @Test
    fun `legacy local session token is not accepted as authenticated user`() {
        val context = obtenerContextoPrueba()
        MobileSession.clear(context)

        val legacyResponse = AuthResponse(
            token = "local_session_legacy",
            user = AuthUser(id = "local-user", email = "old@example.com")
        )
        try {
            MobileSession.save(context, legacyResponse)
        } catch (_: IllegalArgumentException) {
            // The session store rejects locally fabricated tokens only through validation.
        }

        assertNull(MobileSession.user(context))
    }

    @Test
    fun `account deletion does not clear local data before backend confirmation`() = runBlocking {
        val context = obtenerContextoPrueba()
        MobileSession.clear(context)

        val repo = EleganRepository(context)
        val sampleProduct: ProductJson = mapOf(
            "id" to "backend_prod_delete_test",
            "name" to "Camisa Sartorial",
            "category" to "camisa",
            "price" to 89.0,
            "imageUrl" to "https://cdn.example.com/camisa.jpg"
        )
        repo.addToCart(sampleProduct, "M", 1L, "Negro")
        repo.setProductSaved(sampleProduct.productId(), true)

        try {
            repo.deleteUserAccountData()
            throw AssertionError("La eliminación debe exigir una sesión autenticada.")
        } catch (_: IllegalStateException) {
            // Sin token no se contacta el backend ni se borra el contenido local.
        }

        assertTrue(repo.observeCart().first().isNotEmpty())
        assertTrue(repo.observeSavedProductIds().first().isNotEmpty())
    }}
