package com.example.healthlogops.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE userId = :userId AND password = :password")
    suspend fun login(userId: String, password: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE userId = :userId)")
    suspend fun userExists(userId: String): Boolean

    @Query("SELECT email FROM users WHERE userId = :userId")
    suspend fun getUserEmail(userId: String): String?
}
