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

    // Calendar & Date Selection
    private val _selectedDate = MutableStateFlow<java.util.Date?>(null)
    val selectedDate: StateFlow<java.util.Date?> = _selectedDate.asStateFlow()

    val activityDates: StateFlow<Set<Long>> = logsWithCategories
        .map { logs ->
            logs.map { log ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = log.log.timestamp
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }.toSet()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    fun setSelectedDate(date: java.util.Date?) {
        _selectedDate.value = date
        // Logic to filter is handled in UI for now, effectively.
        // But if we want strict data layer handling we can filter here too.
        // For "Jump to Today" feature:
        if (date != null && isSameDay(date, java.util.Date())) {
             _selectedDate.value = null // Null implies "All" or "Today" depending on view, but user wants "current date highlighted" which implies selection.
             // Actually, "Jump to Today" usually selects Today.
             _selectedDate.value = java.util.Date()
        }
    }
    
    private fun isSameDay(date1: java.util.Date, date2: java.util.Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
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
     * Export logs within a date range to a JSON string.
     */
    suspend fun exportLogs(startTime: Long, endTime: Long): String {
        _isProcessing.value = true
        return try {
            repository.exportLogsToJson(startTime, endTime)
        } finally {
            _isProcessing.value = false
        }
    }

    /**
     * Parse logs from JSON for inspection.
     */
    fun parseLogs(json: String): List<HealthLog>? {
        return repository.parseLogsFromJson(json)
    }

    /**
     * Import logs with a selected strategy.
     */
    suspend fun importLogs(logs: List<HealthLog>, strategy: HealthLogRepository.ImportStrategy): Boolean {
        _isProcessing.value = true
        return try {
            repository.importLogsWithStrategy(logs, strategy)
        } finally {
            _isProcessing.value = false
        }
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
