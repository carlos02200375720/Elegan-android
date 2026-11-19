package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [PendingVisitorShareEntity::class],
    version = 1,
    exportSchema = true
)
internal abstract class EleganDatabase : RoomDatabase() {
    abstract fun pendingVisitorShareDao(): PendingVisitorShareDao

    companion object {
        const val DATABASE_NAME = "elegan_local.db"

        /**
         * Registro explícito de migraciones para evolucionar el esquema sin destruir datos locales
         * (especialmente la cola pendiente en `pending_visitor_shares`).
         * Ejemplo al incrementar `version = 2`:
         * `object : Migration(1, 2) { override fun migrate(db: SupportSQLiteDatabase) { ... } }`
         */
        val ALL_MIGRATIONS: Array<Migration> = emptyArray()

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
