package com.example.healthlogops.data.repository

import com.example.healthlogops.data.local.User
import com.example.healthlogops.data.local.UserDao

class UserRepository(private val userDao: UserDao) {
    suspend fun getUserById(userId: String): User? = userDao.getUserById(userId)
    
    suspend fun login(userId: String, password: String): User? = userDao.login(userId, password)
    
    suspend fun registerUser(user: User) = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun userExists(userId: String): Boolean = userDao.userExists(userId)
    
    suspend fun getUserEmail(userId: String): String? = userDao.getUserEmail(userId)
}
