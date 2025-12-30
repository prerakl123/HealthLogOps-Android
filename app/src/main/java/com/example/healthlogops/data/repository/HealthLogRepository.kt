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
     * Export all logs to a JSON string for migration.
     */
    suspend fun exportLogsToJson(): String {
        return with(kotlinx.coroutines.Dispatchers.IO) {
            val logs = healthLogDao.getAllLogsSync()
            com.google.gson.Gson().toJson(logs)
        }
    }
}
