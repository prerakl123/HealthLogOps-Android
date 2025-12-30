package com.example.healthlogops.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.data.repository.HealthLogRepository
import com.example.healthlogops.ui.components.DateGroup
import com.example.healthlogops.ui.components.FilterChip
import com.example.healthlogops.ui.components.getCategoryIcon
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.example.healthlogops.ui.viewmodel.ViewMode
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

/**
 * Home screen displaying health log entries grouped by date.
 *
 * Features:
 * - Header with app title and daily stats
 * - Filter chips for filtering by category
 * - Logs grouped by date with smart date formatting
 * - Beautiful log cards with category colors and metric pills
 * - Floating action button for adding new logs
 * - Empty state when no logs exist
 *
 * @param viewModel The ViewModel providing log data
 * @param onNavigateToAddLog Callback to navigate to Add Log screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAddLog: () -> Unit,
    onNavigateToEditLog: (Int) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logsWithCategories by viewModel.logsWithCategories.collectAsState()
    val categoryList by viewModel.allCategories.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<Int?>(null) }

    // Quick Actions state
    var showQuickActions by remember { mutableStateOf(false) }
    var selectedLogId by remember { mutableStateOf<Int?>(null) }

    // Filter state - tracks which category filter is active ("" means All)
    var activeFilter by remember { mutableStateOf("") }

    // UI State
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    var showMenu by remember { mutableStateOf(false) }
    var showCategoriesDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Data Management State
    var showExportRangePicker by remember { mutableStateOf(false) }
    var showImportStrategyDialog by remember { mutableStateOf(false) }
    var pendingImportLogs by remember { mutableStateOf<List<HealthLog>?>(null) }
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // Save Launcher (Local Device)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(pendingExportJson?.toByteArray() ?: ByteArray(0))
                    }
                    snackbarHostState.showSnackbar("Backup saved successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                } finally {
                    pendingExportJson = null
                }
            }
        }
    }

    // Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val json = inputStream.bufferedReader().use { it.readText() }
                        val logs = viewModel.parseLogs(json)
                        if (logs != null) {
                            pendingImportLogs = logs
                            showImportStrategyDialog = true
                        } else {
                            snackbarHostState.showSnackbar("Invalid backup file")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error reading file: ${e.message}")
                }
            }
        }
    }

    // Extract unique categories from logs
    val categories = remember(logsWithCategories) {
        logsWithCategories
            .map { it.category.name }
            .distinct()
            .sorted()
    }

    val selectedDate by viewModel.selectedDate.collectAsState()

    // Filter logs based on active filter and selected date
    val filteredLogs = remember(logsWithCategories, activeFilter, selectedDate) {
        var logs = logsWithCategories
        
        // Category Filter
        if (activeFilter.isNotEmpty()) {
            logs = logs.filter { it.category.name == activeFilter }
        }
        
        // Date Filter
        if (selectedDate != null) {
            val filterDate = Calendar.getInstance().apply {
                time = selectedDate!!
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            logs = logs.filter { logWithCategory ->
                 val logDate = Calendar.getInstance().apply {
                    timeInMillis = logWithCategory.log.timestamp
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                logDate == filterDate
            }
        }
        logs
    }

    // Group logs by date - wrapped in remember to avoid heavy computation on every recomposition
    val logsByDate = remember(filteredLogs) {
        filteredLogs.groupBy { logWithCategory ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = logWithCategory.log.timestamp
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.time
        }.toSortedMap(compareByDescending { it })
    }

    // Calculate today's count (from all logs, not filtered)
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time

    val todayCount = logsWithCategories.count { logWithCategory ->
        val logDate = Calendar.getInstance().apply {
            timeInMillis = logWithCategory.log.timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        logDate == today
    }

    val isProcessing by viewModel.isProcessing.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "HealthLogOps",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = when {
                                    todayCount == 0 -> "No activities logged today"
                                    todayCount == 1 -> "Today: 1 activity logged"
                                    else -> "Today: $todayCount activities logged"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.setSelectedDate(if (selectedDate == null) Date() else null) }) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Calendar",
                                tint = if (selectedDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false },
                                modifier = Modifier.width(220.dp)
                            ) {
                                // --- Appearance Section ---
                                Text(
                                    text = "Appearance",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                DropdownMenuItem(
                                    text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode") },
                                    leadingIcon = { 
                                        Icon(
                                            if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, 
                                            contentDescription = null 
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.toggleTheme()
                                        showMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Compact View") },
                                    leadingIcon = { Icon(Icons.Default.ViewHeadline, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.COMPACT)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.COMPACT) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Balanced View") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.BALANCED)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.BALANCED) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                )

                                DropdownMenuItem(
                                    text = { Text("Detailed View") },
                                    leadingIcon = { Icon(Icons.Default.ViewModule, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.DETAILED)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.DETAILED) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary) }
                                )
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // --- Data Section ---
                                Text(
                                    text = "Data Management",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                DropdownMenuItem(
                                    text = { Text("Export Data") },
                                    leadingIcon = { Icon(Icons.Default.Upload, null) },
                                    onClick = {
                                        showExportRangePicker = true
                                        showMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Import Data") },
                                    leadingIcon = { Icon(Icons.Default.Download, null) },
                                    onClick = {
                                        importLauncher.launch("application/json")
                                        showMenu = false
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                                // --- Help & Info ---
                                Text(
                                    text = "Information",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                DropdownMenuItem(
                                    text = { Text("Categories") },
                                    leadingIcon = { Icon(Icons.Default.Category, null) },
                                    onClick = {
                                        onNavigateToCategories()
                                        showMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("About") },
                                    leadingIcon = { Icon(Icons.Default.Info, null) },
                                    onClick = {
                                        onNavigateToAbout()
                                        showMenu = false
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToAddLog,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add log"
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Calendar View
                val activityDates by viewModel.activityDates.collectAsState()
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedDate != null,
                    enter = androidx.compose.animation.expandVertically(),
                    exit = androidx.compose.animation.shrinkVertically()
                ) {
                    com.example.healthlogops.ui.components.CalendarView(
                        selectedDate = selectedDate ?: Date(), // Should not be null if visible, but safe fallback
                        activityDates = activityDates,
                        onDateSelected = { date -> viewModel.setSelectedDate(date) },
                        onJumpToToday = { viewModel.setSelectedDate(Date()) }
                    )
                }
                
                // Filter chips row
                if (categories.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "All" filter chip
                        item {
                            FilterChip(
                                text = "All",
                                isActive = activeFilter.isEmpty(),
                                onClick = { activeFilter = "" }
                            )
                        }

                        // Category filter chips
                        items(categories) { category ->
                            FilterChip(
                                text = category,
                                isActive = activeFilter == category,
                                onClick = { activeFilter = category }
                            )
                        }
                    }
                }

                // Logs content
                if (filteredLogs.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = if (activeFilter.isEmpty()) "No logs yet" else "No $activeFilter logs",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Tap + to add your first activity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Logs list
                    val listState = rememberLazyListState()
                    val canLoadMore by remember {
                        derivedStateOf {
                            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 2
                        }
                    }

                    LaunchedEffect(canLoadMore) {
                        if (canLoadMore && !isProcessing) {
                            val currentDays = viewModel.daysToLoad.value
                            val totalDays = viewModel.totalDaysAvailable.value
                            if (currentDays < totalDays) {
                                viewModel.loadMoreLogs()
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        logsByDate.forEach { (date, logs) ->
                            item(key = date.time) {
                                DateGroup(
                                    date = date,
                                    logs = logs,
                                    onEdit = { logId ->
                                        onNavigateToEditLog(logId)
                                    },
                                    onDelete = { logId ->
                                        logToDelete = logId
                                        showDeleteDialog = true
                                    },
                                    viewMode = viewMode,
                                    onLongPress = { logId ->
                                        selectedLogId = logId
                                        showQuickActions = true
                                    }
                                )
                            }
                        }

                        // Load More indicator/button
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val currentDays by viewModel.daysToLoad.collectAsState()
                                val totalDays by viewModel.totalDaysAvailable.collectAsState()

                                TextButton(
                                    onClick = { 
                                        if (currentDays >= totalDays) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("You've reached the beginning of your history!")
                                            }
                                        }
                                        viewModel.loadMoreLogs() 
                                    }
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Showing $currentDays of $totalDays logging days")
                                        Text(
                                            text = "Load more history",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delete confirmation dialog
            if (showDeleteDialog && logToDelete != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteDialog = false
                        logToDelete = null
                    },
                    title = {
                        Text(text = "Delete Activity")
                    },
                    text = {
                        Text(text = "Are you sure you want to delete this activity? This action cannot be undone.")
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val logId = logToDelete
                                showDeleteDialog = false
                                logToDelete = null
                                scope.launch {
                                    val log = logId?.let { viewModel.getLogById(it) }
                                    if (log != null) {
                                        viewModel.deleteLog(log)
                                        snackbarHostState.showSnackbar("Activity deleted")
                                    }
                                }
                            }
                        ) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDeleteDialog = false
                                logToDelete = null
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Categories Dialog
            if (showCategoriesDialog) {
                AlertDialog(
                    onDismissRequest = { showCategoriesDialog = false },
                    title = {
                        Text(
                            "Health Categories",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                "Track these areas of your health with curated templates.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            categoryList.forEach { category ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .background(
                                                    color = Color(category.color.toColorInt()).copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = getCategoryIcon(category.name),
                                                contentDescription = null,
                                                tint = Color(category.color.toColorInt()),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = category.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )

                                            // Simple description from template
                                            val template = try {
                                                Gson().fromJson(
                                                    category.templateJson,
                                                    Map::class.java
                                                ) as? Map<String, String>
                                            } catch (_: Exception) {
                                                null
                                            }

                                            if (!template.isNullOrEmpty()) {
                                                Text(
                                                    text = "Tracks: " + template.keys.joinToString(", ") {
                                                        it.replace(
                                                            "_",
                                                            " "
                                                        )
                                                    }.lowercase(),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showCategoriesDialog = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Awesome")
                        }
                    }
                )
            }

            // Quick Actions Dialog
            if (showQuickActions && selectedLogId != null) {
                AlertDialog(
                    onDismissRequest = {
                        showQuickActions = false
                        selectedLogId = null
                    },
                    title = { Text("Quick Actions") },
                    text = {
                        Column {
                            ListItem(
                                headlineContent = { Text("Edit Activity") },
                                leadingContent = { Icon(Icons.Default.Edit, null) },
                                modifier = Modifier.clickable {
                                    onNavigateToEditLog(selectedLogId!!)
                                    showQuickActions = false
                                    selectedLogId = null
                                }
                            )
                            ListItem(
                                headlineContent = { Text("Delete Activity") },
                                leadingContent = {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.clickable {
                                    logToDelete = selectedLogId
                                    showDeleteDialog = true
                                    showQuickActions = false
                                    selectedLogId = null
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showQuickActions = false
                            selectedLogId = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Loading context overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {}, // Intercept clicks
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text("Processing...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // Export Date Range Picker
            if (showExportRangePicker) {
                val dateRangePickerState = rememberDateRangePickerState()
                DatePickerDialog(
                    onDismissRequest = { showExportRangePicker = false },
                    confirmButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Option 1: Save to Device
                            TextButton(
                                onClick = {
                                    val start = dateRangePickerState.selectedStartDateMillis
                                    val end = dateRangePickerState.selectedEndDateMillis
                                    if (start != null && end != null) {
                                        scope.launch {
                                            try {
                                                val endOfDay = Calendar.getInstance().apply {
                                                    timeInMillis = end
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 59)
                                                    set(Calendar.MILLISECOND, 999)
                                                }.timeInMillis

                                                val json = viewModel.exportLogs(start, endOfDay)
                                                pendingExportJson = json
                                                createDocumentLauncher.launch("health_logs_backup_${System.currentTimeMillis()}.json")
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Export failed: ${e.message}")
                                            }
                                        }
                                    }
                                    showExportRangePicker = false
                                }
                            ) {
                                Icon(Icons.Default.SaveAlt, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Save File")
                            }

                            // Option 2: Share (Current direct method)
                            Button(
                                onClick = {
                                    val start = dateRangePickerState.selectedStartDateMillis
                                    val end = dateRangePickerState.selectedEndDateMillis
                                    if (start != null && end != null) {
                                        scope.launch {
                                            try {
                                                val endOfDay = Calendar.getInstance().apply {
                                                    timeInMillis = end
                                                    set(Calendar.HOUR_OF_DAY, 23)
                                                    set(Calendar.MINUTE, 59)
                                                    set(Calendar.SECOND, 59)
                                                    set(Calendar.MILLISECOND, 999)
                                                }.timeInMillis

                                                val json = viewModel.exportLogs(start, endOfDay)
                                                val cacheFile = File(context.cacheDir, "health_logs_export.json")
                                                cacheFile.writeText(json)
                                                
                                                val contentUri = FileProvider.getUriForFile(
                                                    context,
                                                    "com.example.healthlogops.fileprovider",
                                                    cacheFile
                                                )

                                                val sendIntent: Intent = Intent().apply {
                                                    action = Intent.ACTION_SEND
                                                    putExtra(Intent.EXTRA_STREAM, contentUri)
                                                    type = "application/json"
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                val shareIntent = Intent.createChooser(sendIntent, "Export Health Logs")
                                                context.startActivity(shareIntent)
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Export failed: ${e.message}")
                                            }
                                        }
                                    }
                                    showExportRangePicker = false
                                }
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Share")
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExportRangePicker = false }) {
                            Text("Cancel")
                        }
                    }
                ) {
                    DateRangePicker(
                        state = dateRangePickerState,
                        modifier = Modifier.weight(1f).padding(16.dp),
                        title = { Text("Select Export Range", modifier = Modifier.padding(16.dp)) },
                        showModeToggle = false
                    )
                }
            }

            // Import Strategy Dialog
            if (showImportStrategyDialog && pendingImportLogs != null) {
                AlertDialog(
                    onDismissRequest = { 
                        showImportStrategyDialog = false
                        pendingImportLogs = null
                    },
                    title = { Text("Import Strategy") },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("A backup with ${pendingImportLogs!!.size} logs was found. How would you like to handle existing data?")
                            
                            val strategies = listOf(
                                Triple("Merge with current data", "Keep existing logs and add imported ones.", HealthLogRepository.ImportStrategy.MERGE_ALL),
                                Triple("Overwrite conflicts for those days", "Replace existing logs on days present in backup.", HealthLogRepository.ImportStrategy.OVERWRITE_CONFLICTS),
                                Triple("Overwrite all in the date range", "Clear the entire backup period before importing.", HealthLogRepository.ImportStrategy.OVERWRITE_RANGE)
                            )

                            strategies.forEach { (label, description, strategy) ->
                                Surface(
                                    onClick = {
                                        val logs = pendingImportLogs
                                        showImportStrategyDialog = false
                                        pendingImportLogs = null
                                        scope.launch {
                                            if (logs != null) {
                                                val success = viewModel.importLogs(logs, strategy)
                                                if (success) {
                                                    snackbarHostState.showSnackbar("Import successful")
                                                } else {
                                                    snackbarHostState.showSnackbar("Import failed")
                                                }
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}, // Handled by strategy options
                    dismissButton = {
                        TextButton(onClick = { 
                            showImportStrategyDialog = false
                            pendingImportLogs = null
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
