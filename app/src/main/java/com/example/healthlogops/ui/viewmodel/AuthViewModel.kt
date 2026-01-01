package com.example.healthlogops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlogops.data.local.User
import com.example.healthlogops.data.repository.SessionManager
import com.example.healthlogops.data.repository.UserRepository
import com.example.healthlogops.util.ErrorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val currentUserId: StateFlow<String?> = sessionManager.userId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        viewModelScope.launch {
            currentUserId.collect { id ->
                if (id != null) {
                    _currentUser.value = userRepository.getUserById(id)
                } else {
                    _currentUser.value = null
                }
            }
        }
    }

    suspend fun login(userId: String, pass: String): Boolean {
        return try {
            val user = userRepository.login(userId, pass)
            if (user != null) {
                sessionManager.saveSession(userId)
                true
            } else {
                ErrorManager.emitError("Invalid credentials")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorManager.emitError("Login failed: ${e.localizedMessage}")
            false
        }
    }

    suspend fun register(user: User): Boolean {
        return try {
            if (!userRepository.userExists(user.userId)) {
                userRepository.registerUser(user)
                sessionManager.saveSession(user.userId)
                true
            } else {
                ErrorManager.emitError("User already exists")
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorManager.emitError("Registration failed: ${e.localizedMessage}")
            false
        }
    }

    suspend fun logout() {
        sessionManager.clearSession()
    }

    suspend fun userExists(userId: String): Boolean {
        return userRepository.userExists(userId)
    }

    fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email
        val name = parts[0]
        val domain = parts[1]
        if (name.length <= 4) return name.first() + "***" + "@" + domain
        return name.take(3) + "******" + name.takeLast(1) + "@" + domain
    }

    suspend fun forgotPassword(userId: String): String? {
        val email = userRepository.getUserEmail(userId)
        return if (email != null) {
            maskEmail(email)
        } else {
            null
        }
    }
    
    suspend fun updateUser(user: User) {
        try {
            userRepository.updateUser(user)
            _currentUser.value = user
        } catch (e: Exception) {
            e.printStackTrace()
            ErrorManager.emitError("Failed to update profile: ${e.localizedMessage}")
        }
    }
}

class AuthViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
