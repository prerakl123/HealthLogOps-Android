package com.example.healthlogops.data.model

import com.example.healthlogops.data.local.Category
import com.example.healthlogops.data.local.HealthLog

/**
 * Data class combining a HealthLog with its associated Category.
 * Used for displaying log entries with category information in the UI.
 */
data class LogWithCategory(
    val log: HealthLog,
    val category: Category
)
