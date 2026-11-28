package com.example.data.work

import android.content.Context
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.error.ErrorApi
import com.example.data.error.MapeadorErroresApi
import com.example.data.local.EleganDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.ProductRemoteKeyEntity
import com.example.data.local.toEntity
import com.example.data.observability.MonitorErrores
import com.example.data.remote.ApiAutenticacionMovil
import com.example.data.remote.EleganAuthApi
import com.example.data.repository.ProductosPagingSource
import com.example.data.repository.ProductosRemoteMediator
import com.example.data.repository.extractPaginaProductos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker de WorkManager encargado de sincronizar el catálogo de productos en segundo plano
 * desde el backend hacia Room sin bloquear la interfaz de usuario.
 *
 * - Descarga páginas acotadas (`page` / `limit`) para no saturar memoria ni red con miles de prendas.
 * - Actualiza Room de forma atómica con [withTransaction] solo después de recibir datos válidos;
 *   si ocurre un error de red, la caché local permanece intacta para lectura offline.
 * - Registra la clave de paginación en `product_remote_keys` para que Paging 3 continúe cargando
 *   páginas adicionales de manera fluida.
 */
class SincronizadorProductosWorker internal constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val apiProvider: () -> EleganAuthApi,
    private val databaseProvider: () -> EleganDatabase,
    private val clockMs: () -> Long
) : CoroutineWorker(appContext, workerParams) {

    constructor(
        appContext: Context,
        workerParams: WorkerParameters
    ) : this(
        appContext = appContext,
        workerParams = workerParams,
        apiProvider = { ApiAutenticacionMovil.service },
        databaseProvider = { EleganDatabase.getInstance(appContext.applicationContext) },
        clockMs = { System.currentTimeMillis() }
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val pageSize = inputData.getInt(KEY_PAGE_SIZE, DEFAULT_SYNC_PAGE_SIZE)
            .coerceIn(5, ProductosPagingSource.MAX_PAGE_SIZE)
        val maxPages = inputData.getInt(KEY_MAX_PAGES, DEFAULT_MAX_SYNC_PAGES)
            .coerceIn(1, MAX_ALLOWED_SYNC_PAGES)

        try {
            val api = apiProvider()
            val db = databaseProvider()
            val syncedEntities = ArrayList<ProductEntity>(pageSize)
            val seenIds = HashSet<String>()

            var currentPage = 1
            var pagesFetched = 0
            var nextPageForMediator: Int? = null
            var lastServerPaginated = true
            val syncedAtMs = clockMs()

            while (pagesFetched < maxPages) {
                val rawResponse = api.productsPage(
                    page = currentPage,
                    limit = pageSize,
                    category = null,
                    query = null,
                    inStock = null
                )
                val pagina = extractPaginaProductos(
                    response = rawResponse,
                    requestedPage = currentPage,
                    requestedLimit = pageSize
                )
                pagesFetched += 1
                lastServerPaginated = pagina.serverPaginated

                var addedInPage = 0
                for (product in pagina.items) {
                    if (product.id.isBlank() || !seenIds.add(product.id)) continue
                    syncedEntities.add(
                        product.toEntity(
                            sortOrder = syncedEntities.size,
                            syncedAtMs = syncedAtMs
                        )
                    )
                    addedInPage += 1
                }

                nextPageForMediator = pagina.nextPage
                if (pagina.nextPage == null || pagina.items.isEmpty() || (addedInPage == 0 && !pagina.serverPaginated)) {
                    nextPageForMediator = null
                    break
                }
                currentPage = pagina.nextPage
            }

            db.withTransaction {
                val productDao = db.productDao()
                val remoteKeyDao = db.productRemoteKeyDao()

                productDao.deleteAll()
                remoteKeyDao.clearAll()

                if (syncedEntities.isNotEmpty()) {
                    productDao.upsertAll(syncedEntities)
                }

                remoteKeyDao.upsert(
                    ProductRemoteKeyEntity(
                        queryKey = ProductosRemoteMediator.buildQueryKey(
                            category = null,
                            query = null,
                            inStockOnly = true
                        ),
                        nextPage = nextPageForMediator,
                        lastUpdatedMs = syncedAtMs,
                        serverPaginated = lastServerPaginated
                    )
                )
            }

            Result.success(
                workDataOf(
                    KEY_SYNCED_COUNT to syncedEntities.size,
                    KEY_PAGES_FETCHED to pagesFetched,
                    KEY_SYNCED_AT_MS to syncedAtMs
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            val mapped = MapeadorErroresApi.mapear(t)
            val extras = mapOf(
                "error_category" to mapped.errorCategory,
                "attempt" to (runAttemptCount + 1).toString()
            )
            val shouldRetry = mapped.isRetryable && runAttemptCount < MAX_RETRY_ATTEMPTS
            if (shouldRetry) {
                MonitorErrores.registrarAdvertencia(
                    tag = TAG,
                    mensaje = "Sincronización de productos reintentará (${mapped.errorCategory}, intento ${runAttemptCount + 1}).",
                    error = t,
                    extras = extras
                )
                Result.retry()
            } else {
                if (mapped is ErrorApi.NetworkError) {
                    MonitorErrores.registrarAdvertencia(
                        tag = TAG,
                        mensaje = "Sincronización agotó reintentos por red; caché Room preservada.",
                        error = t,
                        extras = extras
                    )
                } else {
                    MonitorErrores.registrarError(
                        tag = TAG,
                        mensaje = "Sincronización de productos falló (${mapped.errorCategory}): ${mapped.userMessage}",
                        error = t,
                        extras = extras
                    )
                }
                Result.failure(
                    workDataOf(
                        KEY_ERROR_MESSAGE to mapped.userMessage,
                        KEY_ERROR_CATEGORY to mapped.errorCategory
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "EleganCatalogSyncWorker"
        const val KEY_PAGE_SIZE = "page_size"
        const val KEY_MAX_PAGES = "max_pages"
        const val KEY_SYNCED_COUNT = "synced_count"
        const val KEY_PAGES_FETCHED = "pages_fetched"
        const val KEY_SYNCED_AT_MS = "synced_at_ms"
        const val KEY_ERROR_MESSAGE = "error_message"
        const val KEY_ERROR_CATEGORY = "error_category"

        const val DEFAULT_SYNC_PAGE_SIZE = 30
        const val DEFAULT_MAX_SYNC_PAGES = 15
        const val MAX_ALLOWED_SYNC_PAGES = 50
        const val MAX_RETRY_ATTEMPTS = 3
    }
}
