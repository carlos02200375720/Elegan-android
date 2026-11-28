package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PendingVisitorShareEntity::class,
        ProductEntity::class,
        ProductRemoteKeyEntity::class
    ],
    version = EleganDatabase.SCHEMA_VERSION,
    exportSchema = true
)
internal abstract class EleganDatabase : RoomDatabase() {
    abstract fun pendingVisitorShareDao(): PendingVisitorShareDao
    abstract fun productDao(): ProductDao
    abstract fun productRemoteKeyDao(): ProductRemoteKeyDao

    companion object {
        const val DATABASE_NAME = "elegan_local.db"
        const val SCHEMA_VERSION = 2

        /**
         * Migración explícita v1 -> v2: crea las tablas locales para el catálogo persistente
         * (`cached_products` con sus índices y `product_remote_keys` para Paging 3)
         * preservando intacta la cola de eventos `pending_visitor_shares`.
         */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_products` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `categoryId` TEXT NOT NULL,
                        `categoryName` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `imageUrl` TEXT NOT NULL,
                        `videoUrl` TEXT NOT NULL,
                        `hasVideo` INTEGER NOT NULL,
                        `inStock` INTEGER NOT NULL,
                        `allowCashOnDelivery` INTEGER NOT NULL,
                        `mediaFormat` TEXT NOT NULL,
                        `selectedShippingId` TEXT NOT NULL,
                        `reviewCount` INTEGER NOT NULL,
                        `likesCount` INTEGER,
                        `commentsCount` INTEGER,
                        `savesCount` INTEGER,
                        `sharesCount` INTEGER,
                        `imagesJson` TEXT NOT NULL,
                        `galleryVideoUrlsJson` TEXT NOT NULL,
                        `sizesJson` TEXT NOT NULL,
                        `colorsJson` TEXT NOT NULL,
                        `shippingMethodsJson` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `syncedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_products_categoryId` ON `cached_products` (`categoryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_products_inStock` ON `cached_products` (`inStock`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_products_sortOrder` ON `cached_products` (`sortOrder`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `product_remote_keys` (
                        `queryKey` TEXT NOT NULL,
                        `nextPage` INTEGER,
                        `lastUpdatedMs` INTEGER NOT NULL,
                        `serverPaginated` INTEGER NOT NULL,
                        PRIMARY KEY(`queryKey`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Registro explícito de migraciones para evolucionar el esquema sin destruir datos locales
         * (especialmente la cola pendiente en `pending_visitor_shares`).
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)

        @Volatile
        private var instance: EleganDatabase? = null

        fun createBuilder(
            context: Context,
            databaseName: String = DATABASE_NAME
        ): Builder<EleganDatabase> =
            Room.databaseBuilder(
                context.applicationContext,
                EleganDatabase::class.java,
                databaseName
            ).addMigrations(*ALL_MIGRATIONS)

        fun getInstance(context: Context): EleganDatabase {
            return instance ?: synchronized(this) {
                instance ?: createBuilder(context)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
