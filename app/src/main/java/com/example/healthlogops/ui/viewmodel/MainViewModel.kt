package com.example.healthlogops.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.data.repository.HealthLogRepository
import com.example.healthlogops.data.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ViewMode { COMPACT, BALANCED, DETAILED }

class MainViewModel(
    private val repository: HealthLogRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val allCategories = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Time-based pagination state
    private val _daysToLoad = MutableStateFlow(7)
    val daysToLoad: StateFlow<Int> = _daysToLoad.asStateFlow()

    private val _totalDaysAvailable = MutableStateFlow(7)
    val totalDaysAvailable: StateFlow<Int> = _totalDaysAvailable.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val logsWithCategories = _daysToLoad.flatMapLatest { days ->
        val startTime = System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000L)
        repository.getLogsWithCategoriesSince(startTime)
    }.onEach {
        updateTotalDaysAvailable()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun updateTotalDaysAvailable() {
        viewModelScope.launch {
            val oldestTimestamp = repository.getOldestLogTimestamp()
            if (oldestTimestamp != null) {
                val diffMillis = System.currentTimeMillis() - oldestTimestamp
                val days = (diffMillis / (24 * 60 * 60 * 1000L)).toInt() + 1
                _totalDaysAvailable.value = days.coerceAtLeast(7)
            }
        }
    }

    fun loadMoreLogs() {
        _daysToLoad.value += 7
    }

    // Operation state
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // Preferences loading state
    private val _isPreferencesLoaded = MutableStateFlow(false)
    val isPreferencesLoaded: StateFlow<Boolean> = _isPreferencesLoaded.asStateFlow()

    // UI State from DataStore
    val isDarkMode: StateFlow<Boolean> = userPreferencesRepository.isDarkMode
        .onEach { _isPreferencesLoaded.value = true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val viewMode: StateFlow<ViewMode> = userPreferencesRepository.viewMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ViewMode.BALANCED
        )

    fun toggleTheme() {
        viewModelScope.launch {
            userPreferencesRepository.toggleTheme()
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch {
            userPreferencesRepository.setViewMode(mode)
        }
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
    suspend fun insertLog(log: HealthLog) {
        _isProcessing.value = true
        try {
            repository.insertLog(log)
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Update an existing health log entry.
     */
    suspend fun updateLog(log: HealthLog) {
        _isProcessing.value = true
        try {
            repository.updateLog(log)
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Delete a health log entry.
     */
    suspend fun deleteLog(log: HealthLog) {
        _isProcessing.value = true
        try {
            repository.deleteLog(log)
        } finally {
            _isProcessing.value = false
        }
    }
    
    /**
     * Get a specific log by ID.
     */
    suspend fun getLogById(logId: Int): HealthLog? {
        return repository.getLogById(logId)
    }

    /**
     * Export all logs to a JSON string.
     */
    suspend fun exportLogs(): String {
        return repository.exportLogsToJson()
    }
}

class MainViewModelFactory(
    private val repository: HealthLogRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userPreferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
