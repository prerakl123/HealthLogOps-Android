package com.example.healthlogops.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String, // Material Icon name
    val color: String, // Hex color string
    val templateJson: String // JSON String defining the fields
)
