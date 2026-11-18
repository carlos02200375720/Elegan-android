package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [PendingVisitorShareEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class EleganDatabase : RoomDatabase() {
    abstract fun pendingVisitorShareDao(): PendingVisitorShareDao

    companion object {
        @Volatile
        private var instance: EleganDatabase? = null

        fun getInstance(context: Context): EleganDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EleganDatabase::class.java,
                    "elegan_local.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
