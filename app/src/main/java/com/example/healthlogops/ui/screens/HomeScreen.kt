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
import com.example.healthlogops.ui.components.DateGroup
import com.example.healthlogops.ui.components.FilterChip
import com.example.healthlogops.ui.components.getCategoryIcon
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.example.healthlogops.ui.viewmodel.ViewMode
import com.google.gson.Gson
import kotlinx.coroutines.launch
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

    // Extract unique categories from logs
    val categories = remember(logsWithCategories) {
        logsWithCategories
            .map { it.category.name }
            .distinct()
            .sorted()
    }

    // Filter logs based on active filter
    val filteredLogs = remember(logsWithCategories, activeFilter) {
        if (activeFilter.isEmpty()) {
            logsWithCategories
        } else {
            logsWithCategories.filter { it.category.name == activeFilter }
        }
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
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(if (isDarkMode) "Light Mode" else "Dark Mode") },
                                    onClick = {
                                        viewModel.toggleTheme()
                                        showMenu = false
                                    }
                                )

                                HorizontalDivider()

                                Text(
                                    text = "View Mode",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                DropdownMenuItem(
                                    text = { Text("Compact") },
                                    leadingIcon = { Icon(Icons.Default.ViewHeadline, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.COMPACT)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.COMPACT) Text("✓") }
                                )

                                DropdownMenuItem(
                                    text = { Text("Balanced") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.BALANCED)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.BALANCED) Text("✓") }
                                )

                                DropdownMenuItem(
                                    text = { Text("Detailed") },
                                    leadingIcon = { Icon(Icons.Default.ViewModule, null) },
                                    onClick = {
                                        viewModel.setViewMode(ViewMode.DETAILED)
                                        showMenu = false
                                    },
                                    trailingIcon = { if (viewMode == ViewMode.DETAILED) Text("✓") }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Categories") },
                                    onClick = {
                                        onNavigateToCategories()
                                        showMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("About") },
                                    onClick = {
                                        onNavigateToAbout()
                                        showMenu = false
                                    }
                                )

                                HorizontalDivider()

                                DropdownMenuItem(
                                    text = { Text("Export Data (JSON)") },
                                    leadingIcon = { Icon(Icons.Default.Share, null) },
                                    onClick = {
                                        scope.launch {
                                            val json = viewModel.exportLogs()
                                            val sendIntent: Intent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, json)
                                                type = "application/json"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Export Health Logs")
                                            context.startActivity(shareIntent)

                                            snackbarHostState.showSnackbar("Sharing data...")
                                        }
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
                                scope.launch {
                                    val log = viewModel.getLogById(logToDelete!!)
                                    if (log != null) {
                                        viewModel.deleteLog(log)
                                        snackbarHostState.showSnackbar("Activity deleted")
                                    }
                                    showDeleteDialog = false
                                    logToDelete = null
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
        }
    }
}
