package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface PendingVisitorShareDao {
    @Query("SELECT * FROM pending_visitor_shares ORDER BY createdAt ASC")
    fun observePendingShares(): Flow<List<PendingVisitorShareEntity>>

    @Query("SELECT * FROM pending_visitor_shares ORDER BY createdAt ASC")
    suspend fun getPendingShares(): List<PendingVisitorShareEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingShare(entry: PendingVisitorShareEntity)

    @Query("DELETE FROM pending_visitor_shares WHERE eventId = :eventId")
    suspend fun deleteByEventId(eventId: String)

    @Query("DELETE FROM pending_visitor_shares")
    suspend fun clearAll()
}
