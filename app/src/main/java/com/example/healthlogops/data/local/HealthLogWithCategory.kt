package com.example.healthlogops.data.local

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Data class for Room to return HealthLog with its associated Category.
 * Uses Room's @Embedded and @Relation annotations for automatic JOIN.
 */
data class HealthLogWithCategory(
    @Embedded val healthLog: HealthLog,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id"
    )
    val category: Category
)
