package com.example.healthlogops.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthLogDao {
    @Query("SELECT * FROM health_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<HealthLog>>

    @Query("SELECT * FROM health_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsSync(): List<HealthLog>

    @Query("SELECT * FROM health_logs WHERE category_id = :categoryId ORDER BY timestamp DESC")
    fun getLogsByCategory(categoryId: Int): Flow<List<HealthLog>>

    @Query("SELECT * FROM health_logs WHERE id = :logId")
    suspend fun getLogById(logId: Int): HealthLog?

    @Query("SELECT COUNT(id) FROM health_logs")
    suspend fun getLogCount(): Int

    @Query("SELECT MIN(timestamp) FROM health_logs")
    suspend fun getOldestLogTimestamp(): Long?
    
    @Transaction
    @Query("SELECT * FROM health_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsWithCategories(limit: Int = 100): Flow<List<HealthLogWithCategory>>

    @Transaction
    @Query("SELECT * FROM health_logs WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLogsWithCategoriesSince(startTime: Long): Flow<List<HealthLogWithCategory>>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HealthLog): Long

    @Update
    suspend fun updateLog(log: HealthLog)

    @Delete
    suspend fun deleteLog(log: HealthLog)
}
