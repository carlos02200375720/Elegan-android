package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
    MobileSession.loginClientLocally(context)
}
