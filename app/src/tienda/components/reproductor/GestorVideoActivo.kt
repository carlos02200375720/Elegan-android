package com.example.ui.components

/**
 * Coordinador global de reproducción de video que garantiza que, como máximo,
 * exista una sola instancia activa de `ExoPlayer` decodificando hardware (`MediaCodec`)
 * al mismo tiempo en toda la aplicación.
 */
internal object GestorVideoActivo {
    @Volatile
    private var estadoActivo: EstadoReproductorVideo? = null

    fun reclamarReproduccion(nuevo: EstadoReproductorVideo) {
        val anterior: EstadoReproductorVideo? = synchronized(this) {
            val prev = estadoActivo
            if (prev === nuevo) {
                null
            } else {
                estadoActivo = nuevo
                prev
            }
        }
        anterior?.liberarPorReemplazo()
    }

    fun liberarSiEsActivo(estado: EstadoReproductorVideo) {
        synchronized(this) {
            if (estadoActivo === estado) {
                estadoActivo = null
            }
        }
    }

    fun estadoActivoActual(): EstadoReproductorVideo? = estadoActivo

    fun reiniciarParaPruebas() {
        synchronized(this) {
            estadoActivo = null
        }
    }
}
