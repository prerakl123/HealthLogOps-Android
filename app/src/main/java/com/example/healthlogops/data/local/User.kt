package com.example.healthlogops.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val password: String,
    val email: String,
    val age: Int? = null,
    val height: Float? = null,
    val weight: Float? = null,
    val profileParameters: String = "{}" // JSON for extra parameters like job type
)
