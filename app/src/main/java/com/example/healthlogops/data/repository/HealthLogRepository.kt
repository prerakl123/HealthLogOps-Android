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

    suspend fun insertCategory(category: Category) {
        categoryDao.insertCategory(category)
    }

    suspend fun getCategoryById(id: Int): Category? {
        return categoryDao.getCategoryById(id)
    }

    // Log Operations
    val allLogs: Flow<List<HealthLog>> = healthLogDao.getAllLogs()
    
    /**
     * Get recent logs with their associated categories.
     * Converts Room's HealthLogWithCategory to UI's LogWithCategory.
     */
    fun getLogsWithCategories(limit: Int = 100): Flow<List<LogWithCategory>> {
        return healthLogDao.getLogsWithCategories(limit).map { list ->
            list.map { healthLogWithCategory ->
                LogWithCategory(
                    log = healthLogWithCategory.healthLog,
                    category = healthLogWithCategory.category
                )
            }
        }
    }

    fun getLogsWithCategoriesSince(startTime: Long): Flow<List<LogWithCategory>> {
        return healthLogDao.getLogsWithCategoriesSince(startTime).map { list ->
            list.map { healthLogWithCategory ->
                LogWithCategory(
                    log = healthLogWithCategory.healthLog,
                    category = healthLogWithCategory.category
                )
            }
        }
    }

    fun getLogsByCategory(categoryId: Int): Flow<List<HealthLog>> {
        return healthLogDao.getLogsByCategory(categoryId)
    }

    suspend fun insertLog(log: HealthLog) {
        healthLogDao.insertLog(log)
    }
    
    suspend fun updateLog(log: HealthLog) {
        healthLogDao.updateLog(log)
    }
    
    suspend fun getLogById(logId: Int): HealthLog? {
        return healthLogDao.getLogById(logId)
    }

    suspend fun getOldestLogTimestamp(): Long? {
        return healthLogDao.getOldestLogTimestamp()
    }

    suspend fun deleteLog(log: HealthLog) {
        healthLogDao.deleteLog(log)
    }

    /**
     * Export logs within a date range to a JSON string.
     */
    suspend fun exportLogsToJson(startTime: Long, endTime: Long): String {
        return with(kotlinx.coroutines.Dispatchers.IO) {
            val logs = healthLogDao.getLogsInRangeSync(startTime, endTime)
            com.google.gson.Gson().toJson(logs)
        }
    }

    enum class ImportStrategy {
        MERGE_ALL,          // Just insert everything (might create duplicates)
        OVERWRITE_RANGE,    // Clear the specific range of the imported logs before inserting
        OVERWRITE_CONFLICTS,// (Advanced) Only overwrite if same timestamp+activity exists. 
                            // For simplicity, we'll implement user's 4 specific choices.
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
        strategy: ImportStrategy
    ): Boolean {
        return with(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (logs.isEmpty()) return@with true

                val minTimestamp = logs.minOf { it.timestamp }
                val maxTimestamp = logs.maxOf { it.timestamp }

                when (strategy) {
                    ImportStrategy.OVERWRITE_RANGE -> {
                        // Delete ALL logs in the database within the date range of the imported file
                        healthLogDao.deleteLogsInRange(minTimestamp, maxTimestamp)
                    }
                    ImportStrategy.OVERWRITE_CONFLICTS -> {
                        // Implementation for "overwrite conflicts for that day":
                        // Actually the user asks for:
                        // 1. Overwrite logs for that day (if conflicts arise)
                        // 2. Merge with available logs
                        // 3. Overwrite all in range
                        // 4. Merge all in range
                        // We'll map these in the ViewModel. 
                        // For "overwrite conflicts for that day", we'll delete logs only on the days that have logs in the import.
                        
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
                            healthLogDao.deleteLogsInRange(dayStart, dayEnd)
                        }
                    }
                    ImportStrategy.MERGE_ALL -> {
                        // Just insert, no deletion
                    }
                }

                // Insert all logs. Note: PK 'id' is reset to 0 to trigger auto-generation and avoid ID conflicts.
                logs.forEach { 
                    healthLogDao.insertLog(it.copy(id = 0)) 
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
}
