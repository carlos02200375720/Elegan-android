package com.example.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.data.remote.ClienteHttpElegan

/**
 * Inicializador automático mínimo de Jetpack App Startup.
 * Solo registra el [Context] de aplicación en [ClienteHttpElegan] sin crear clientes
 * OkHttp ni realizar I/O en el arranque en frío (los clientes se instancian `by lazy`).
 */
class ClienteHttpInitializer : Initializer<ClienteHttpElegan> {

    override fun create(context: Context): ClienteHttpElegan {
        ClienteHttpElegan.inicializar(context.applicationContext)
        return ClienteHttpElegan
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
