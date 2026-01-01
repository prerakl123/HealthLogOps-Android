package com.example.healthlogops.data.repository

import com.example.healthlogops.data.local.Category
import com.example.healthlogops.data.local.CategoryDao
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.data.local.HealthLogDao
import com.example.healthlogops.data.model.LogWithCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthLogRepository(
    private val categoryDao: CategoryDao,
    private val healthLogDao: HealthLogDao
) {
    // Category Operations
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
    
    fun getAllCategoriesByUser(userId: String): Flow<List<Category>> {
        return categoryDao.getAllCategoriesByUser(userId)
    }

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    // Log Operations
    fun getAllLogs(userId: String): Flow<List<HealthLog>> {
        return healthLogDao.getAllLogs(userId)
    }
    
    /**
     * Get recent logs with their associated categories.
     * Converts Room's HealthLogWithCategory to UI's LogWithCategory.
     */
    fun getLogsWithCategories(userId: String, limit: Int = 100): Flow<List<LogWithCategory>> {
        return healthLogDao.getLogsWithCategories(userId, limit).map { list ->
            list.map { healthLogWithCategory ->
                LogWithCategory(
                    log = healthLogWithCategory.healthLog,
                    category = healthLogWithCategory.category
                )
            }
        }
    }

    fun getLogsWithCategoriesSince(startTime: Long, userId: String): Flow<List<LogWithCategory>> {
        return healthLogDao.getLogsWithCategoriesSince(startTime, userId).map { list ->
            list.map { healthLogWithCategory ->
                LogWithCategory(
                    log = healthLogWithCategory.healthLog,
                    category = healthLogWithCategory.category
                )
            }
        }
    }

    fun getLogsByCategory(userId: String, categoryId: Int): Flow<List<HealthLog>> {
        return healthLogDao.getLogsByCategory(userId, categoryId)
    }

    suspend fun insertLog(log: HealthLog) {
        healthLogDao.insertLog(log)
    }
    
    suspend fun updateLog(log: HealthLog) {
        healthLogDao.updateLog(log)
    }
    
    suspend fun getLogById(userId: String, logId: Int): HealthLog? {
        return healthLogDao.getLogById(userId, logId)
    }

    suspend fun getOldestLogTimestamp(userId: String): Long? {
        return healthLogDao.getOldestLogTimestamp(userId)
    }

    suspend fun deleteLog(log: HealthLog) {
        healthLogDao.deleteLog(log)
    }

    /**
     * Export logs within a date range to a JSON string.
     */
    suspend fun exportLogsToJson(userId: String, startTime: Long, endTime: Long): String {
        return with(kotlinx.coroutines.Dispatchers.IO) {
            val logs = healthLogDao.getLogsInRangeSync(userId, startTime, endTime)
            com.google.gson.Gson().toJson(logs)
        }
    }

    enum class ImportStrategy {
        MERGE_ALL,          // Just insert everything (might create duplicates)
        OVERWRITE_RANGE,    // Clear the specific range of the imported logs before inserting
        OVERWRITE_CONFLICTS,// (Advanced) Only overwrite if same timestamp+activity exists. 
    }

    /**
     * Parse logs from JSON string to inspect them before importing.
     */
    fun parseLogsFromJson(json: String): List<HealthLog>? {
        return try {
            val listType = object : com.google.gson.reflect.TypeToken<List<HealthLog>>() {}.type
            com.google.gson.Gson().fromJson(json, listType)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Import logs from a JSON string using a specific strategy.
     */
    suspend fun importLogsWithStrategy(
        logs: List<HealthLog>,
        strategy: ImportStrategy,
        userId: String
    ): Boolean {
        return with(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (logs.isEmpty()) return@with true

                val minTimestamp = logs.minOf { it.timestamp }
                val maxTimestamp = logs.maxOf { it.timestamp }

                when (strategy) {
                    ImportStrategy.OVERWRITE_RANGE -> {
                        // Delete ALL logs in the database within the date range of the imported file
                        healthLogDao.deleteLogsInRange(userId, minTimestamp, maxTimestamp)
                    }
                    ImportStrategy.OVERWRITE_CONFLICTS -> {
                        val daysWithLogs = logs.map { 
                            val cal = java.util.Calendar.getInstance()
                            cal.timeInMillis = it.timestamp
                            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                            cal.set(java.util.Calendar.MINUTE, 0)
                            cal.set(java.util.Calendar.SECOND, 0)
                            cal.set(java.util.Calendar.MILLISECOND, 0)
                            cal.timeInMillis
                        }.toSet()

                        daysWithLogs.forEach { dayStart ->
                            val dayEnd = dayStart + (24 * 60 * 60 * 1000L) - 1
                            healthLogDao.deleteLogsInRange(userId, dayStart, dayEnd)
                        }
                    }
                    ImportStrategy.MERGE_ALL -> {
                        // Just insert, no deletion
                    }
                }

                // Insert all logs. Note: PK 'id' is reset to 0.
                // WE MUST provide the userId here to the copy() method because
                // GSON might have deserialized it as null if it was missing from the JSON.
                logs.forEach { 
                    healthLogDao.insertLog(it.copy(id = 0, userId = userId)) 
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
