package com.example.startup

import android.content.Context
import androidx.startup.Initializer
import com.example.data.local.EleganDatabase

data class EstadoBaseDatosLocal(
    val nombreBaseDatos: String,
    val instanciaLista: Boolean
)

/**
 * Inicializador diferido (lazy) de la base de datos Room ([EleganDatabase]).
 *
 * Declara dependencia explícita sobre [SentryInitializer] para que App Startup inicialice
 * en orden determinista `ClienteHttpInitializer -> SentryInitializer -> BaseDatosLocalInitializer`
 * fuera de la ruta crítica del arranque en frío.
 */
class BaseDatosLocalInitializer : Initializer<EstadoBaseDatosLocal> {

    override fun create(context: Context): EstadoBaseDatosLocal {
        EleganDatabase.getInstance(context.applicationContext)
        return EstadoBaseDatosLocal(
            nombreBaseDatos = EleganDatabase.DATABASE_NAME,
            instanciaLista = true
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> =
        listOf(SentryInitializer::class.java)
}
