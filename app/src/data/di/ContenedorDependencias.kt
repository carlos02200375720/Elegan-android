package com.example.data.di

import android.content.Context
import com.example.data.repository.EleganRepository
import com.example.data.repository.RepositorioAutenticacion
import com.example.data.repository.RepositorioDocumentosLegales

/**
 * Contrato del contenedor de dependencias de la aplicación.
 * Permite compartir instancias únicas de repositorios en producción y sustituirlas
 * fácilmente en pruebas unitarias o Robolectric sin depender de frameworks pesados.
 */
interface ContenedorDependenciasElegan {
    val eleganRepository: EleganRepository
    val authRepository: RepositorioAutenticacion
    val legalDocumentsRepository: RepositorioDocumentosLegales
}

/**
 * Contrato implementado por [com.example.EleganApplication] para exponer el contenedor raíz.
 */
interface ProveedorContenedorElegan {
    val contenedor: ContenedorDependenciasElegan
}

/**
 * Implementación concreta del contenedor de dependencias con inicialización perezosa
 * acoplada al ciclo de vida del proceso de la aplicación.
 */
class ContenedorDependenciasApp(context: Context) : ContenedorDependenciasElegan {
    private val appContext = context.applicationContext

    override val eleganRepository: EleganRepository by lazy {
        EleganRepository(appContext)
    }

    override val authRepository: RepositorioAutenticacion by lazy {
        RepositorioAutenticacion()
    }

    override val legalDocumentsRepository: RepositorioDocumentosLegales by lazy {
        RepositorioDocumentosLegales()
    }
}

@Volatile
private var fallbackContainerRef: Pair<Context, ContenedorDependenciasElegan>? = null

/**
 * Resuelve el [ContenedorDependenciasElegan] asociado a la aplicación actual.
 * En ejecución normal devuelve la instancia raíz de `EleganApplication`; en entornos de
 * prueba o previews sin `EleganApplication` crea un respaldo reutilizable por `applicationContext`.
 */
fun Context.obtenerContenedorElegan(): ContenedorDependenciasElegan {
    val app = applicationContext
    if (app is ProveedorContenedorElegan) {
        return app.contenedor
    }
    val cached = fallbackContainerRef
    if (cached != null && cached.first === app) {
        return cached.second
    }
    return synchronized(ContenedorDependenciasApp::class.java) {
        val current = fallbackContainerRef
        if (current != null && current.first === app) {
            current.second
        } else {
            val created = ContenedorDependenciasApp(app)
            fallbackContainerRef = app to created
            created
        }
    }
}
