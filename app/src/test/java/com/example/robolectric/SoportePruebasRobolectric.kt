package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.AuthResponse
import com.example.data.remote.AuthUser
import com.example.data.remote.MobileSession

internal fun obtenerContextoPrueba(): Context =
    ApplicationProvider.getApplicationContext()

internal fun limpiarPreferenciasPrueba(context: Context, vararg nombres: String) {
    nombres.forEach { nombre ->
        context.getSharedPreferences(nombre, Context.MODE_PRIVATE).edit().clear().commit()
    }
}

internal fun reiniciarSesionClienteDemo(context: Context) {
    MobileSession.clear(context)
    MobileSession.save(
        context,
        AuthResponse(
            token = "backend_test_token",
            user = AuthUser(
                id = "user-demo-001",
                email = "cliente.elegan@gmail.com",
                name = "Cliente Elegan",
                role = "customer",
                provider = "password"
            )
        )
    )
}
