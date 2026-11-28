package com.example

import android.util.Log
import androidx.room.Room
import androidx.startup.AppInitializer
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.data.local.EleganDatabase
import com.example.data.local.toEntity
import com.example.data.model.Product
import com.example.data.repository.ProductosRemoteMediator
import com.example.data.work.FabricaWorkersElegan
import com.example.data.work.ProgramadorSincronizacionProductos
import com.example.data.work.SincronizadorProductosWorker
import com.example.startup.BaseDatosLocalInitializer
import com.example.startup.SincronizacionCatalogoInitializer
import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasWorkManagerSincronizacionTest {

    private fun productMap(index: Int): Map<String, Any?> = mapOf(
        "id" to "wm_prod_$index",
        "name" to "Prenda Sincronizada $index",
        "description" to "Sincronizada en segundo plano #$index",
        "categoryId" to if (index % 2 == 0) "camisas" else "trajes",
        "categoryName" to if (index % 2 == 0) "Camisas" else "Trajes",
        "price" to (2500 + index * 100),
        "inStock" to true
    )

    @Test
    fun `SincronizadorProductosWorker sincroniza paginas acotadas en Room y guarda remoteKey para Paging 3`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val db = Room.inMemoryDatabaseBuilder(context, EleganDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            // Producto obsoleto previo que debe reemplazarse tras sincronización exitosa
            db.productDao().upsert(
                Product(id = "obsoleto_1", name = "Producto Viejo", inStock = true).toEntity(sortOrder = 0)
            )

            val fakeApi = object : BaseFakeEleganAuthApi() {
                override suspend fun productsPage(
                    page: Int,
                    limit: Int,
                    category: String?,
                    query: String?,
                    inStock: Boolean?
                ): Map<String, Any?> {
                    val total = 15
                    val start = ((page - 1) * limit) + 1
                    val end = (start + limit - 1).coerceAtMost(total)
                    val items = if (start <= end) (start..end).map { productMap(it) } else emptyList()
                    return mapOf(
                        "items" to items,
                        "pagination" to mapOf(
                            "page" to page,
                            "limit" to limit,
                            "total" to total,
                            "hasMore" to (end < total)
                        )
                    )
                }
            }

            val factory = FabricaWorkersElegan(
                apiProvider = { fakeApi },
                databaseProvider = { db },
                clockMs = { 1_800_000_000_000L }
            )

            val worker = TestListenableWorkerBuilder<SincronizadorProductosWorker>(
                context = context,
                inputData = workDataOf(
                    SincronizadorProductosWorker.KEY_PAGE_SIZE to 5,
                    SincronizadorProductosWorker.KEY_MAX_PAGES to 2
                )
            )
                .setWorkerFactory(factory)
                .build()

            val result = worker.doWork()
            assertTrue("El worker debe completar con éxito", result is ListenableWorker.Result.Success)
            val output = (result as ListenableWorker.Result.Success).outputData
            assertEquals(10, output.getInt(SincronizadorProductosWorker.KEY_SYNCED_COUNT, -1))
            assertEquals(2, output.getInt(SincronizadorProductosWorker.KEY_PAGES_FETCHED, -1))
            assertEquals(1_800_000_000_000L, output.getLong(SincronizadorProductosWorker.KEY_SYNCED_AT_MS, -1L))

            val persisted = db.productDao().getAllProducts()
            assertEquals(10, persisted.size)
            assertEquals("wm_prod_1", persisted.first().id)
            assertEquals("wm_prod_10", persisted.last().id)
            assertTrue("El producto obsoleto debe eliminarse tras refresco exitoso", persisted.none { it.id == "obsoleto_1" })

            val remoteKey = db.productRemoteKeyDao().getByQueryKey(
                ProductosRemoteMediator.buildQueryKey(category = null, query = null, inStockOnly = true)
            )
            assertNotNull(remoteKey)
            assertEquals(3, remoteKey?.nextPage)
        } finally {
            db.close()
        }
    }

    @Test
    fun `SincronizadorProductosWorker reintenta errores transitorios, falla en 4xx y nunca borra Room`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val db = Room.inMemoryDatabaseBuilder(context, EleganDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            db.productDao().upsertAll(
                listOf(
                    Product(id = "keep_1", name = "Guardado Offline 1", inStock = true).toEntity(sortOrder = 0),
                    Product(id = "keep_2", name = "Guardado Offline 2", inStock = true).toEntity(sortOrder = 1)
                )
            )

            val offlineApi = object : BaseFakeEleganAuthApi() {
                override suspend fun productsPage(
                    page: Int,
                    limit: Int,
                    category: String?,
                    query: String?,
                    inStock: Boolean?
                ): Map<String, Any?> {
                    throw IOException("Sin conexión a internet")
                }
            }

            val offlineFactory = FabricaWorkersElegan(
                apiProvider = { offlineApi },
                databaseProvider = { db }
            )

            // Primer intento con IOException -> Result.retry() sin borrar caché Room
            val retryWorker = TestListenableWorkerBuilder<SincronizadorProductosWorker>(context)
                .setWorkerFactory(offlineFactory)
                .setRunAttemptCount(0)
                .build()
            assertEquals(ListenableWorker.Result.retry(), retryWorker.doWork())
            assertEquals(listOf("keep_1", "keep_2"), db.productDao().getAllProducts().map { it.id })

            // Al superar MAX_RETRY_ATTEMPTS -> Result.failure() preservando caché Room
            val exhaustedWorker = TestListenableWorkerBuilder<SincronizadorProductosWorker>(context)
                .setWorkerFactory(offlineFactory)
                .setRunAttemptCount(SincronizadorProductosWorker.MAX_RETRY_ATTEMPTS)
                .build()
            assertTrue(exhaustedWorker.doWork() is ListenableWorker.Result.Failure)
            assertEquals(listOf("keep_1", "keep_2"), db.productDao().getAllProducts().map { it.id })

            // Error cliente HTTP 400 -> Result.failure() inmediato sin reintentos inútiles
            val clientErrorApi = object : BaseFakeEleganAuthApi() {
                override suspend fun productsPage(
                    page: Int,
                    limit: Int,
                    category: String?,
                    query: String?,
                    inStock: Boolean?
                ): Map<String, Any?> {
                    val errorBody = """{"error":"bad_request"}""".toResponseBody("application/json".toMediaType())
                    throw HttpException(Response.error<Map<String, Any?>>(400, errorBody))
                }
            }
            val badRequestWorker = TestListenableWorkerBuilder<SincronizadorProductosWorker>(context)
                .setWorkerFactory(FabricaWorkersElegan(apiProvider = { clientErrorApi }, databaseProvider = { db }))
                .setRunAttemptCount(0)
                .build()
            assertTrue(badRequestWorker.doWork() is ListenableWorker.Result.Failure)
            assertEquals(listOf("keep_1", "keep_2"), db.productDao().getAllProducts().map { it.id })
        } finally {
            db.close()
        }
    }

    @Test
    fun `ProgramadorSincronizacionProductos registra trabajos unicos con restricciones de red y SincronizacionCatalogoInitializer es diferido`() {
        val context = obtenerContextoPrueba()

        // Verifica configuración de solicitudes
        val periodicReq = ProgramadorSincronizacionProductos.construirSolicitudPeriodica()
        assertEquals(NetworkType.CONNECTED, periodicReq.workSpec.constraints.requiredNetworkType)
        assertTrue(periodicReq.workSpec.constraints.requiresBatteryNotLow())
        assertTrue(periodicReq.tags.contains(ProgramadorSincronizacionProductos.TAG_CATALOG_SYNC))

        val immediateReq = ProgramadorSincronizacionProductos.construirSolicitudInmediata()
        assertEquals(NetworkType.CONNECTED, immediateReq.workSpec.constraints.requiredNetworkType)
        assertTrue(immediateReq.tags.contains(ProgramadorSincronizacionProductos.TAG_CATALOG_SYNC))

        // Inicializa WorkManager en modo de prueba sincrónico con un API stub seguro
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .setWorkerFactory(
                FabricaWorkersElegan(
                    apiProvider = {
                        object : BaseFakeEleganAuthApi() {
                            override suspend fun productsPage(
                                page: Int,
                                limit: Int,
                                category: String?,
                                query: String?,
                                inStock: Boolean?
                            ): Map<String, Any?> = mapOf("items" to emptyList<Any>(), "pagination" to mapOf("hasMore" to false))
                        }
                    }
                )
            )
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        val workManager = WorkManager.getInstance(context)

        // Programar dos veces el trabajo periódico debe conservar un único registro (ExistingPeriodicWorkPolicy.KEEP)
        assertTrue(ProgramadorSincronizacionProductos.programarSincronizacionPeriodica(context))
        assertTrue(ProgramadorSincronizacionProductos.programarSincronizacionPeriodica(context))
        val periodicInfos = workManager
            .getWorkInfosForUniqueWork(ProgramadorSincronizacionProductos.UNIQUE_PERIODIC_SYNC_NAME)
            .get()
        assertEquals(1, periodicInfos.size)
        assertEquals(WorkInfo.State.ENQUEUED, periodicInfos.first().state)

        // Trabajo inmediato único registrado con restricciones de red
        assertTrue(ProgramadorSincronizacionProductos.solicitarSincronizacionInmediata(context, forceReplace = false))
        val immediateInfos = workManager
            .getWorkInfosForUniqueWork(ProgramadorSincronizacionProductos.UNIQUE_IMMEDIATE_SYNC_NAME)
            .get()
        assertEquals(1, immediateInfos.size)

        // Verificar que SincronizacionCatalogoInitializer es diferido y depende de BaseDatosLocalInitializer
        val appInitializer = AppInitializer.getInstance(context)
        assertFalse(
            "SincronizacionCatalogoInitializer no debe ejecutarse en el arranque en frío",
            appInitializer.isEagerlyInitialized(SincronizacionCatalogoInitializer::class.java)
        )
        assertEquals(
            listOf(BaseDatosLocalInitializer::class.java),
            SincronizacionCatalogoInitializer().dependencies()
        )
    }
}
