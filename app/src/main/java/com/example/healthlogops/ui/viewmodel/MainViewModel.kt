package com.example.healthlogops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.data.repository.HealthLogRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ViewMode { COMPACT, BALANCED, DETAILED }

class MainViewModel(private val repository: HealthLogRepository) : ViewModel() {
    val allCategories = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val logsWithCategories = repository.getLogsWithCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI State
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _viewMode = MutableStateFlow(ViewMode.BALANCED)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    /**
     * Get unique activity names for a specific category to provide suggestions.
     */
    fun getActivitySuggestions(categoryId: Int): List<String> {
        return logsWithCategories.value
            .filter { it.category.id == categoryId }
            .map { it.log.activityName }
            .distinct()
            .sorted()
    }
    
    /**
     * Insert a new health log entry.
     */
    fun insertLog(log: HealthLog) {
        viewModelScope.launch {
            repository.insertLog(log)
        }
    }
    
    /**
     * Update an existing health log entry.
     */
    fun updateLog(log: HealthLog) {
        viewModelScope.launch {
            repository.updateLog(log)
        }
    }
    
    /**
     * Delete a health log entry.
     */
    fun deleteLog(log: HealthLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
    
    /**
     * Get a specific log by ID.
     */
    suspend fun getLogById(logId: Int): HealthLog? {
        return repository.getLogById(logId)
    }
}

class MainViewModelFactory(private val repository: HealthLogRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
