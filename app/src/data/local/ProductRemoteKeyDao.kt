package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface ProductRemoteKeyDao {

    @Query("SELECT * FROM product_remote_keys WHERE queryKey = :queryKey LIMIT 1")
    suspend fun getByQueryKey(queryKey: String): ProductRemoteKeyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(key: ProductRemoteKeyEntity)

    @Query("DELETE FROM product_remote_keys WHERE queryKey = :queryKey")
    suspend fun deleteByQueryKey(queryKey: String)

    @Query("DELETE FROM product_remote_keys")
    suspend fun clearAll()
}
