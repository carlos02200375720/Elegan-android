package com.example.data.remote

import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Categorías de refresco desacopladas para los eventos en tiempo real del WebSocket.
 * Evita que eventos de alta frecuencia (comentarios, likes, guardados) disparen
 * recargas masivas del catálogo completo para miles de usuarios simultáneos.
 */
enum class AccionRefrescoTiempoReal {
    PRODUCTOS,
    CATEGORIAS,
    ANUNCIOS,
    CARRITO,
    PEDIDOS,
    MENSAJES_SOPORTE,
    PERFIL_USUARIO,
    COMENTARIOS_ABIERTOS,
    INTERACCIONES_VISIBLES,
    CONTRA_ENTREGA
}

/**
 * Coordinador con coalescencia (*debounce*) y *jitter* aleatorio para proteger al cliente y
 * al backend frente a tormentas de eventos (*thundering herd*) con miles de usuarios conectados.
 *
 * - Eventos privados del usuario (`cart`, `orders`, `supportMessages`, `usuarios`, `blocked_users`)
 *   se agrupan en una ventana corta para mantener frescura inmediata.
 * - Eventos globales de catálogo (`products`, `store_categories`, `announcements`, `cash_on_delivery`)
 *   se agrupan con ventana + *jitter* para escalonar las consultas entre miles de dispositivos.
 * - Eventos sociales de alta frecuencia (`comments`, `likes`, `guardadas`, `publication_shares`)
 *   nunca recargan el catálogo completo: solo actualizan comentarios abiertos e interacciones visibles.
 */
class CoordinadorEventosTiempoReal(
    private val scope: CoroutineScope,
    private val onEjecutar: (AccionRefrescoTiempoReal) -> Unit,
    private val delayPrivadoMs: Long = 350L,
    private val delayGlobalBaseMs: Long = 1_500L,
    private val delaySocialBaseMs: Long = 2_500L,
    private val jitterMaximoMs: Long = 1_500L,
    private val jitterProvider: (Long) -> Long = { max ->
        if (max <= 0L) 0L else Random.nextLong(0L, max + 1L)
    }
) {
    private val lock = Any()
    private val trabajosPendientes = HashMap<AccionRefrescoTiempoReal, Job>()

    fun procesarEvento(event: RealtimeEvent) {
        when (event.collection) {
            "products" -> programar(AccionRefrescoTiempoReal.PRODUCTOS, delayConJitter(delayGlobalBaseMs))
            "store_categories" -> programar(AccionRefrescoTiempoReal.CATEGORIAS, delayConJitter(delayGlobalBaseMs))
            "announcements" -> programar(AccionRefrescoTiempoReal.ANUNCIOS, delayConJitter(delayGlobalBaseMs))
            "cash_on_delivery", "cashOnDelivery" ->
                programar(AccionRefrescoTiempoReal.CONTRA_ENTREGA, delayConJitter(delayGlobalBaseMs))
            "cart" -> programar(AccionRefrescoTiempoReal.CARRITO, delayPrivadoMs)
            "orders" -> programar(AccionRefrescoTiempoReal.PEDIDOS, delayPrivadoMs)
            "supportMessages" -> programar(AccionRefrescoTiempoReal.MENSAJES_SOPORTE, delayPrivadoMs)
            "usuarios" -> programar(AccionRefrescoTiempoReal.PERFIL_USUARIO, delayPrivadoMs)
            "blocked_users" -> programar(AccionRefrescoTiempoReal.COMENTARIOS_ABIERTOS, delayPrivadoMs)
            "comments" -> {
                // Solo refresca la hoja de comentarios abierta y los conteos visibles,
                // evitando recargar miles de productos cuando cualquier usuario comenta.
                programar(AccionRefrescoTiempoReal.COMENTARIOS_ABIERTOS, delayPrivadoMs)
                programar(AccionRefrescoTiempoReal.INTERACCIONES_VISIBLES, delayConJitter(delaySocialBaseMs))
            }
            "likes", "guardadas", "publication_shares" -> {
                programar(AccionRefrescoTiempoReal.INTERACCIONES_VISIBLES, delayConJitter(delaySocialBaseMs))
            }
        }
    }

    fun cancelarTodo() = synchronized(lock) {
        trabajosPendientes.values.forEach { it.cancel() }
        trabajosPendientes.clear()
    }

    private fun delayConJitter(baseMs: Long): Long =
        (baseMs + jitterProvider(jitterMaximoMs)).coerceAtLeast(0L)

    private fun programar(accion: AccionRefrescoTiempoReal, esperaMs: Long) = synchronized(lock) {
        trabajosPendientes[accion]?.cancel()
        lateinit var job: Job
        job = scope.launch {
            if (esperaMs > 0L) {
                delay(esperaMs)
            }
            val shouldRun = synchronized(lock) {
                if (trabajosPendientes[accion] === job) {
                    trabajosPendientes.remove(accion)
                    true
                } else {
                    false
                }
            }
            if (shouldRun) {
                onEjecutar(accion)
            }
        }
        trabajosPendientes[accion] = job
    }
}
