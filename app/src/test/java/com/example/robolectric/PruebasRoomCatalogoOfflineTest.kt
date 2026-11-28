package com.example

import androidx.paging.AsyncPagingDataDiffer
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadState
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.example.data.local.EleganDatabase
import com.example.data.local.ProductEntity
import com.example.data.local.toEntity
import com.example.data.model.Product
import com.example.data.model.ProductShippingMethod
import com.example.data.repository.ProductosRemoteMediator
import com.example.data.repository.RepositorioCatalogo
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PruebasRoomCatalogoOfflineTest {

    private fun sampleProductMap(
        id: String,
        name: String,
        categoryId: String = "trajes",
        categoryName: String = "Trajes",
        price: Double = 5200.0,
        inStock: Boolean = true
    ): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "description" to "Confección artesanal italiana para $name",
        "categoryId" to categoryId,
        "categoryName" to categoryName,
        "price" to price.toLong(),
        "imageUrl" to "https://cdn.example.com/$id.jpg",
        "images" to listOf("https://cdn.example.com/$id.jpg", "https://cdn.example.com/$id-2.jpg"),
        "videoUrl" to "https://cdn.example.com/$id.mp4",
        "galleryVideoUrls" to listOf("https://cdn.example.com/$id-detail.mp4"),
        "hasVideo" to true,
        "inStock" to inStock,
        "allowCashOnDelivery" to true,
        "mediaFormat" to "4:5",
        "sizes" to listOf("M", "L"),
        "colors" to listOf("Azul Marino", "Borgoña"),
        "shippingMethods" to listOf(
            mapOf(
                "id" to "ship_express",
                "name" to "Envío Express",
                "price" to 250,
                "deliveryTime" to "24-48h",
                "origin" to "Santo Domingo"
            )
        ),
        "selectedShippingId" to "ship_express",
        "reviewCount" to 12,
        "likesCount" to 44,
        "commentsCount" to 7,
        "savesCount" to 19,
        "sharesCount" to 5
    )

    @Test
    fun `migracion v1 a v2 crea tablas de catalogo y preserva pending_visitor_shares`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val dbName = "elegan_real_migration_1_2_${UUID.randomUUID()}.db"

        // 1. Crear manualmente una base SQLite con el esquema exacto de versión 1
        val v1Helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE IF NOT EXISTS `pending_visitor_shares` (
                                `eventId` TEXT NOT NULL,
                                `productId` TEXT NOT NULL,
                                `visitorId` TEXT NOT NULL,
                                `createdAt` INTEGER NOT NULL,
                                PRIMARY KEY(`eventId`)
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO `pending_visitor_shares` (`eventId`, `productId`, `visitorId`, `createdAt`)
                            VALUES ('evt_v1_preserved', 'prod_legacy_1', 'visitor_v1', 1700000123456)
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        v1Helper.writableDatabase.close()
        v1Helper.close()

        // 2. Abrir la misma base con EleganDatabase (v2 + MIGRATION_1_2)
        val migratedDb = EleganDatabase.createBuilder(context, dbName).build()
        try {
            val preservedShares = migratedDb.pendingVisitorShareDao().getPendingShares()
            assertEquals(1, preservedShares.size)
            assertEquals("evt_v1_preserved", preservedShares[0].eventId)
            assertEquals("prod_legacy_1", preservedShares[0].productId)
            assertEquals("visitor_v1", preservedShares[0].visitorId)
            assertEquals(1_700_000_123_456L, preservedShares[0].createdAt)

            // 3. Verificar que las nuevas tablas de catálogo funcionan sobre la base migrada
            val product = Product(
                id = "prod_migrated_ok",
                name = "Saco Sartorial",
                categoryId = "trajes",
                categoryName = "Trajes",
                price = 7900.0,
                inStock = true,
                shippingMethods = listOf(
                    ProductShippingMethod("s1", "Estándar", 150.0, "2 días", "Atelier")
                )
            )
            migratedDb.productDao().upsert(product.toEntity(sortOrder = 0))
            val loaded = migratedDb.productDao().getById("prod_migrated_ok")
            assertNotNull(loaded)
            assertEquals("Saco Sartorial", loaded?.name)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `catalogo muestra productos guardados en Room sin conexion y resincroniza en segundo plano al volver la red`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val db = Room.inMemoryDatabaseBuilder(context, EleganDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val isOffline = AtomicBoolean(false)
            val catalogVersion = AtomicInteger(1)

            val fakeApi = object : BaseFakeEleganAuthApi() {
                override suspend fun productsPage(
                    page: Int,
                    limit: Int,
                    category: String?,
                    query: String?,
                    inStock: Boolean?
                ): Map<String, Any?> {
                    if (isOffline.get()) {
                        throw IOException("Sin conexión a internet")
                    }
                    val version = catalogVersion.get()
                    val allItems = if (version == 1) {
                        listOf(
                            sampleProductMap("p1", "Traje Milano", "trajes", "Trajes", price = 5000.0),
                            sampleProductMap("p2", "Camisa Lino Blanca", "camisas", "Camisas", price = 1800.0),
                            sampleProductMap("p3", "Mocasin Cuero", "zapatos", "Zapatos", price = 3200.0),
                            sampleProductMap("p4", "Corbata Seda", "accesorios", "Accesorios", price = 950.0)
                        )
                    } else {
                        listOf(
                            sampleProductMap("p1", "Traje Milano Edición 2026", "trajes", "Trajes", price = 5400.0),
                            sampleProductMap("p5", "Abrigo Cachemir", "abrigos", "Abrigos", price = 8900.0)
                        )
                    }.filter { item ->
                        val catOk = category == null || item["categoryId"] == category
                        val qOk = query == null || (item["name"] as String).contains(query, ignoreCase = true)
                        catOk && qOk
                    }

                    val start = ((page - 1) * limit).coerceAtLeast(0)
                    val end = (start + limit).coerceAtMost(allItems.size)
                    val slice = if (start < end) allItems.subList(start, end) else emptyList()
                    return mapOf(
                        "items" to slice,
                        "pagination" to mapOf(
                            "page" to page,
                            "limit" to limit,
                            "total" to allItems.size,
                            "hasMore" to (end < allItems.size)
                        )
                    )
                }

                override suspend fun productById(productId: String): Map<String, Any?> {
                    if (isOffline.get()) {
                        throw IOException("Sin conexión a internet")
                    }
                    return mapOf("item" to sampleProductMap(productId, "Detalle $productId"))
                }
            }

            // 1. Primera sesión con conexión: sincroniza catálogo paginado en Room
            val onlineRepo = RepositorioCatalogo(
                apiProvider = { fakeApi },
                databaseProvider = { db }
            )
            val initialSnapshot = collectPagingSnapshot(
                flow = onlineRepo.observePagedProducts(
                    pageSize = 5,
                    prefetchDistance = 1,
                    maxSize = 20
                ),
                expectMediatorError = false,
                readyCondition = { items -> items.size == 4 }
            )
            assertEquals(listOf("p1", "p2", "p3", "p4"), initialSnapshot.map { it.id })

            // 2. Simular reinicio de app completamente SIN CONEXIÓN (nuevo RepositorioCatalogo sobre la misma Room DB)
            isOffline.set(true)
            val offlineRepo = RepositorioCatalogo(
                apiProvider = { fakeApi },
                databaseProvider = { db }
            )

            // 2a. Flujo reactivo principal emite de inmediato los productos persistidos con todos sus campos complejos
            val persistedList = offlineRepo.observeProducts().first()
            assertEquals(4, persistedList.size)
            assertEquals("Traje Milano", persistedList.first().name)
            assertEquals(listOf("M", "L"), persistedList.first().sizes)
            assertEquals(listOf("Azul Marino", "Borgoña"), persistedList.first().colors)
            assertEquals(1, persistedList.first().shippingMethods.size)
            assertEquals("Envío Express", persistedList.first().shippingMethods.first().name)

            // 2b. Paging 3 sirve contenido desde Room incluso cuando RemoteMediator falla por falta de red
            val offlinePagedSnapshot = collectPagingSnapshot(
                flow = offlineRepo.observePagedProducts(
                    pageSize = 5,
                    prefetchDistance = 1,
                    maxSize = 20
                ),
                expectMediatorError = true,
                readyCondition = { items -> items.size == 4 }
            )
            assertEquals(listOf("p1", "p2", "p3", "p4"), offlinePagedSnapshot.map { it.id })

            // 2c. Búsqueda y filtro por categoría offline consultan directamente Room sin borrar el resto del catálogo
            val offlineFiltered = collectPagingSnapshot(
                flow = offlineRepo.observePagedProducts(
                    category = "camisas",
                    query = "Lino",
                    pageSize = 5,
                    prefetchDistance = 1,
                    maxSize = 20
                ),
                expectMediatorError = true,
                readyCondition = { items -> items.isNotEmpty() }
            )
            assertEquals(listOf("p2"), offlineFiltered.map { it.id })
            assertEquals(4, db.productDao().getAllProducts().size)

            // 2d. Detalle individual por ID responde desde Room sin conexión
            val offlineDetail = offlineRepo.getProductById("p3")
            assertNotNull(offlineDetail)
            assertEquals("Mocasin Cuero", offlineDetail?.name)

            // 3. Al recuperar la conexión, el refresco actualiza Room atómicamente con la nueva versión del servidor
            catalogVersion.set(2)
            isOffline.set(false)
            val resyncedSnapshot = collectPagingSnapshot(
                flow = offlineRepo.observePagedProducts(
                    pageSize = 5,
                    prefetchDistance = 1,
                    maxSize = 20
                ),
                expectMediatorError = false,
                readyCondition = { items -> items.any { it.id == "p5" } }
            )
            assertEquals(listOf("p1", "p5"), resyncedSnapshot.map { it.id })
            assertEquals("Traje Milano Edición 2026", resyncedSnapshot.first().name)
            assertEquals(5400.0, resyncedSnapshot.first().price, 0.01)

            val updatedPersisted = offlineRepo.observeProducts().first()
            assertEquals(listOf("p1", "p5"), updatedPersisted.map { it.id })
        } finally {
            db.close()
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    @Test
    fun `ProductosRemoteMediator nunca elimina la cache local cuando falla el refresco por red`(): Unit = runBlocking {
        val context = obtenerContextoPrueba()
        val db = Room.inMemoryDatabaseBuilder(context, EleganDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            db.productDao().upsertAll(
                listOf(
                    Product(id = "cached_1", name = "Producto Local 1", inStock = true).toEntity(sortOrder = 0),
                    Product(id = "cached_2", name = "Producto Local 2", inStock = true).toEntity(sortOrder = 1)
                )
            )

            val failingMediator = ProductosRemoteMediator(
                databaseProvider = { db },
                fetchPage = { _, _, _, _, _ -> throw IOException("Timeout de servidor") }
            )

            val pagingState = PagingState<Int, ProductEntity>(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 20),
                leadingPlaceholderCount = 0
            )
            val result = failingMediator.load(LoadType.REFRESH, pagingState)
            assertTrue(result is RemoteMediator.MediatorResult.Error)

            val remaining = db.productDao().getAllProducts()
            assertEquals(listOf("cached_1", "cached_2"), remaining.map { it.id })
        } finally {
            db.close()
        }
    }

    private suspend fun collectPagingSnapshot(
        flow: Flow<PagingData<Product>>,
        expectMediatorError: Boolean,
        readyCondition: (List<Product>) -> Boolean
    ): List<Product> = withContext(Dispatchers.Default) {
        withTimeout(15_000) {
            val differ = AsyncPagingDataDiffer(
                diffCallback = object : DiffUtil.ItemCallback<Product>() {
                    override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean =
                        oldItem.id == newItem.id

                    override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean =
                        oldItem == newItem
                },
                updateCallback = object : ListUpdateCallback {
                    override fun onInserted(position: Int, count: Int) = Unit
                    override fun onRemoved(position: Int, count: Int) = Unit
                    override fun onMoved(fromPosition: Int, toPosition: Int) = Unit
                    override fun onChanged(position: Int, count: Int, payload: Any?) = Unit
                },
                mainDispatcher = Dispatchers.Default,
                workerDispatcher = Dispatchers.IO
            )
            val collectJob = launch(Dispatchers.IO) {
                flow.collectLatest { pagingData ->
                    differ.submitData(pagingData)
                }
            }
            try {
                val combinedStates = combine(
                    differ.loadStateFlow,
                    differ.onPagesUpdatedFlow.onStart { emit(Unit) }
                ) { states, _ ->
                    states to differ.snapshot().items
                }.first { (states, items) ->
                    val mediatorOk = if (expectMediatorError) {
                        states.mediator?.refresh is LoadState.Error
                    } else {
                        states.mediator?.refresh is LoadState.NotLoading
                    }
                    states.source.refresh is LoadState.NotLoading &&
                        mediatorOk &&
                        readyCondition(items)
                }
                if (expectMediatorError) {
                    assertTrue(combinedStates.first.mediator?.refresh is LoadState.Error)
                } else {
                    assertTrue(combinedStates.first.mediator?.refresh is LoadState.NotLoading)
                }
                combinedStates.second
            } finally {
                collectJob.cancel()
            }
        }
    }
}
