package com.example.healthlogops.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "health_logs",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["category_id"]),
        Index(value = ["timestamp"])
    ]
)
data class HealthLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "category_id") val categoryId: Int,
    @ColumnInfo(name = "activity_name") val activityName: String,
    val timestamp: Long, // Epoch millis
    val metricsJson: String, // JSON String of metric values
    val notes: String?,
    @ColumnInfo(name = "activity_start_time") val activityStartTime: Long? = null,
    @ColumnInfo(name = "activity_end_time") val activityEndTime: Long? = null
)
