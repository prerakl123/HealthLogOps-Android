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
    @Query("SELECT * FROM health_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    fun getAllLogs(userId: String): Flow<List<HealthLog>>

    @Query("SELECT * FROM health_logs WHERE user_id = :userId ORDER BY timestamp DESC")
    suspend fun getAllLogsSync(userId: String): List<HealthLog>

    @Query("SELECT * FROM health_logs WHERE user_id = :userId AND category_id = :categoryId ORDER BY timestamp DESC")
    fun getLogsByCategory(userId: String, categoryId: Int): Flow<List<HealthLog>>

    @Query("SELECT * FROM health_logs WHERE user_id = :userId AND id = :logId")
    suspend fun getLogById(userId: String, logId: Int): HealthLog?

    @Query("SELECT COUNT(id) FROM health_logs WHERE user_id = :userId")
    suspend fun getLogCount(userId: String): Int

    @Query("SELECT MIN(timestamp) FROM health_logs WHERE user_id = :userId")
    suspend fun getOldestLogTimestamp(userId: String): Long?
    
    @Transaction
    @Query("SELECT * FROM health_logs WHERE user_id = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getLogsWithCategories(userId: String, limit: Int = 100): Flow<List<HealthLogWithCategory>>

    @Transaction
    @Query("SELECT * FROM health_logs WHERE user_id = :userId AND timestamp >= :startTime ORDER BY timestamp DESC")
    fun getLogsWithCategoriesSince(startTime: Long, userId: String): Flow<List<HealthLogWithCategory>>


    @Query("SELECT * FROM health_logs WHERE user_id = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    suspend fun getLogsInRangeSync(userId: String, startTime: Long, endTime: Long): List<HealthLog>

    @Query("DELETE FROM health_logs WHERE user_id = :userId AND timestamp BETWEEN :startTime AND :endTime")
    suspend fun deleteLogsInRange(userId: String, startTime: Long, endTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HealthLog): Long

    @Update
    suspend fun updateLog(log: HealthLog)

    @Delete
    suspend fun deleteLog(log: HealthLog)
}
