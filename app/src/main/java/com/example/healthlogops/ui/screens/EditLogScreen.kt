package com.example.healthlogops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.healthlogops.data.local.Category
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.ui.components.KeyValueField
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

/**
 * Edit Log Screen for modifying existing health log entries.
 *
 * Features:
 * - Pre-populated form with existing log data
 * - Category selection (read-only display)
 * - Activity name editing
 * - Dynamic fields based on category template
 * - Custom fields editing
 * - Notes section
 * - Update functionality
 *
 * @param logId The ID of the log to edit
 * @param viewModel The ViewModel providing data and update operations
 * @param onNavigateBack Callback to navigate back to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLogScreen(
    logId: Int,
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val categories by viewModel.allCategories.collectAsState()
    val logsWithCategories by viewModel.logsWithCategories.collectAsState()

    // State for the log being edited
    var log by remember { mutableStateOf<HealthLog?>(null) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var activityName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val metrics = remember { mutableStateMapOf<String, String>() }
    var customFields by remember { mutableStateOf<List<CustomFieldData>>(emptyList()) }
    var customFieldCounter by remember { mutableIntStateOf(0) }
    
    // New Time Editing State
    var activityStartTime by remember { mutableStateOf<Long?>(null) }
    var activityEndTime by remember { mutableStateOf<Long?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // UI State
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showActivitySuggestions by remember { mutableStateOf(false) }
    val activitySuggestions = remember(selectedCategory, logsWithCategories) {
        selectedCategory?.let { viewModel.getActivitySuggestions(it.id) } ?: emptyList()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val isProcessing by viewModel.isProcessing.collectAsState()

    // AI Analysis states
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<com.example.healthlogops.data.remote.MealAnalysisResponse?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Load the log data
    LaunchedEffect(logId) {
        scope.launch {
            val loadedLog = viewModel.getLogById(logId)
            if (loadedLog != null) {
                log = loadedLog
                activityName = loadedLog.activityName
                notes = loadedLog.notes ?: ""
                activityStartTime = loadedLog.activityStartTime
                activityEndTime = loadedLog.activityEndTime

                // Parse metrics
                val parsedMetrics = editParseMetrics(loadedLog.metricsJson)

                // Find the category
                val category = categories.find { it.id == loadedLog.categoryId }
                selectedCategory = category

                // Separate template metrics from custom fields
                if (category != null) {
                    val templateFields = editParseTemplateFields(category.templateJson)
                    val templateKeys = templateFields.keys

                    // Template metrics - populate the SnapshotStateMap as strings
                    metrics.clear()
                    parsedMetrics.filterKeys { it in templateKeys }.forEach { (k: String, v: Any) ->
                        metrics[k] = v.toString()
                    }

                    // Custom fields
                    val customMetrics = parsedMetrics.filterKeys { it !in templateKeys }
                    customFields = customMetrics.entries.toList().mapIndexed { index: Int, entry: Map.Entry<String, Any> ->
                        CustomFieldData(id = index, name = entry.key, value = entry.value.toString())
                    }
                    customFieldCounter = customFields.size
                } else {
                    // If category not found, treat all as custom
                    metrics.clear()
                    customFields = parsedMetrics.entries.toList().mapIndexed { index: Int, entry: Map.Entry<String, Any> ->
                        CustomFieldData(id = index, name = entry.key, value = entry.value.toString())
                    }
                    customFieldCounter = customFields.size
                }

                isLoading = false
            } else {
                // Log not found, go back
                onNavigateBack()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Edit Activity",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category (read-only display)
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Category",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            ExposedDropdownMenuBox(
                                expanded = showCategoryDropdown,
                                onExpandedChange = { showCategoryDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedCategory?.name ?: "Select category",
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown)
                                    },
                                    colors = OutlinedTextFieldDefaults.colors()
                                )

                                ExposedDropdownMenu(
                                    expanded = showCategoryDropdown,
                                    onDismissRequest = { showCategoryDropdown = false }
                                ) {
                                    categories.forEach { category ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .background(
                                                                Color(category.color.toColorInt()),
                                                                androidx.compose.foundation.shape.CircleShape
                                                            )
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(category.name)
                                                }
                                            },
                                            onClick = {
                                                selectedCategory = category
                                                showCategoryDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Activity Name
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Activity Name",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ExposedDropdownMenuBox(
                                expanded = showActivitySuggestions && activitySuggestions.isNotEmpty(),
                                onExpandedChange = { showActivitySuggestions = it }
                            ) {
                                OutlinedTextField(
                                    value = activityName,
                                    onValueChange = {
                                        activityName = it
                                        showActivitySuggestions = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    placeholder = { Text("Enter activity name") },
                                    singleLine = true,
                                    trailingIcon = {
                                        if (activitySuggestions.isNotEmpty()) {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showActivitySuggestions)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors()
                                )

                                if (activitySuggestions.isNotEmpty()) {
                                    val filteredSuggestions = activitySuggestions.filter {
                                        it.contains(activityName, ignoreCase = true)
                                    }

                                    if (filteredSuggestions.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = showActivitySuggestions,
                                            onDismissRequest = { showActivitySuggestions = false }
                                        ) {
                                            filteredSuggestions.forEach { suggestion ->
                                                DropdownMenuItem(
                                                    text = { Text(suggestion) },
                                                    onClick = {
                                                        activityName = suggestion
                                                        showActivitySuggestions = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Template Fields
                    selectedCategory?.let { category ->
                        val templateFields = editParseTemplateFields(category.templateJson)
                        if (templateFields.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Details",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            items(templateFields.entries.toList()) { entry: Map.Entry<String, String> ->
                                val fieldName = entry.key
                                val fieldType = entry.value
                                KeyValueField(
                                    fieldName = fieldName,
                                    value = metrics[fieldName] ?: "",
                                    onValueChange = { newValue ->
                                        metrics[fieldName] = newValue
                                    },
                                    fieldType = fieldType
                                )
                            }
                        }
                    }

                    // Custom Fields Section
                    if (customFields.isNotEmpty()) {
                        item {
                            Text(
                                text = "Custom Fields",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        items(customFields, key = { it.id }) { customField ->
                            KeyValueField(
                                fieldName = "",
                                fieldType = "str",
                                value = customField.value,
                                onValueChange = { newValue ->
                                    customFields = customFields.map {
                                        if (it.id == customField.id) it.copy(value = newValue) else it
                                    }
                                },
                                isCustom = true,
                                customFieldName = customField.name,
                                onFieldNameChange = { newName ->
                                    customFields = customFields.map {
                                        if (it.id == customField.id) it.copy(name = newName) else it
                                    }
                                },
                                onRemove = {
                                    customFields = customFields.filter { it.id != customField.id }
                                }
                            )
                        }
                    }

                    // Add Custom Field Button
                    item {
                        OutlinedButton(
                            onClick = {
                                customFieldCounter++
                                customFields = customFields + CustomFieldData(
                                    id = customFieldCounter,
                                    name = "Custom $customFieldCounter",
                                    value = ""
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Field")
                        }
                    }

                    // Activity Time (Optional)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Activity Time (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Start Time
                                OutlinedTextField(
                                    value = if (activityStartTime != null) formatDateTime(activityStartTime!!) else "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Start Time") },
                                    placeholder = { Text("select") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showDateTimePicker(context, activityStartTime) { timestamp ->
                                                activityStartTime = timestamp
                                            }
                                        },
                                    enabled = false, // Disable typing, handled by click
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                                
                                // End Time
                                OutlinedTextField(
                                    value = if (activityEndTime != null) formatDateTime(activityEndTime!!) else "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("End Time") },
                                    placeholder = { Text("select") },
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            showDateTimePicker(context, activityEndTime) { timestamp ->
                                                activityEndTime = timestamp
                                            }
                                        },
                                    enabled = false, // Disable typing, handled by click
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            
                            // Clear button if set
                            if (activityStartTime != null || activityEndTime != null) {
                                TextButton(
                                    onClick = { 
                                        activityStartTime = null
                                        activityEndTime = null
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Clear Times")
                                }
                            }
                        }
                    }

                    // Notes Section with AI Analysis
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Notes (Optional)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (selectedCategory?.name == "Meal") {
                                    TextButton(
                                        onClick = {
                                            if (notes.isBlank()) {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Please enter some notes first.")
                                                }
                                                return@TextButton
                                            }
                                            
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Starting analysis...")
                                                isAnalyzing = true
                                                val result = viewModel.analyzeMealNotes(notes)
                                                analysisResult = result
                                                isAnalyzing = false
                                                
                                                if (result != null) {
                                                    if (result.status == "success") {
                                                        showAnalysisDialog = true
                                                    } else if (result.status == "invalid") {
                                                        snackbarHostState.showSnackbar("Invalid note data: ${result.reason}")
                                                    } else {
                                                        snackbarHostState.showSnackbar("Analysis failed: ${result.message}")
                                                    }
                                                } else {
                                                    snackbarHostState.showSnackbar("Analysis failed: Unknown error")
                                                }
                                            }
                                        },
                                        enabled = !isAnalyzing && notes.isNotBlank()
                                    ) {
                                        if (isAnalyzing) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }
                                        Text("Detect Metrics (AI)")
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = { Text("Describe your activity...") },
                                maxLines = 5
                            )
                        }
                    }

                    // Update Button
                    item {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSaving = true

                                    // Combine template metrics and custom fields
                                    val allMetrics = mutableMapOf<String, Any>()

                                    // Parse template metrics
                                    selectedCategory?.let { category ->
                                        val templateFields = editParseTemplateFields(category.templateJson)
                                        metrics.forEach { (key: String, value: String) ->
                                            if (value.isNotBlank()) {
                                                val fieldType = templateFields[key] ?: "str"
                                                allMetrics[key] = when (fieldType.lowercase()) {
                                                    "int" -> value.toIntOrNull() ?: 0
                                                    "float" -> value.toDoubleOrNull() ?: 0.0
                                                    else -> value
                                                }
                                            }
                                        }
                                    }

                                    // Parse custom fields
                                    customFields.forEach { field ->
                                        if (field.name.isNotBlank() && field.value.isNotBlank()) {
                                            val key = field.name.lowercase().replace(" ", "_")
                                            // Try to parse as number, otherwise keep as string
                                            allMetrics[key] = field.value.toIntOrNull()
                                                ?: field.value.toDoubleOrNull()
                                                ?: field.value
                                        }
                                    }

                                    // Create updated log
                                    val logToUpdate = log
                                    if (logToUpdate != null) {
                                        val updatedLog = logToUpdate.copy(
                                            categoryId = selectedCategory?.id ?: logToUpdate.categoryId,
                                            activityName = activityName.ifBlank { selectedCategory?.name ?: "" },
                                            metricsJson = Gson().toJson(allMetrics),
                                            notes = notes.ifBlank { null },
                                            activityStartTime = activityStartTime,
                                            activityEndTime = activityEndTime
                                        )

                                        viewModel.updateLog(updatedLog)
                                        isSaving = false
                                        onNavigateBack()
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Activity updated!")
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text(
                                    text = "Update Activity",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
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
                            Text("Updating...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Analysis Result Dialog
        if (showAnalysisDialog && analysisResult != null) {
            val analysisMetrics = analysisResult?.metrics ?: emptyMap()
            
            AlertDialog(
                onDismissRequest = { showAnalysisDialog = false },
                title = { Text("AI Analysis Result") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Detected nutritional metrics from your notes:")
                        
                        analysisMetrics.forEach { (key, value) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    fontWeight = FontWeight.Bold
                                )
                                Text(text = value.toString())
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "How would you like to update your log?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            // Merge: Keep existing, update/add new from AI
                            analysisMetrics.forEach { (key, value) ->
                                // Note: We only update if it's a known metric for this category
                                // or we can add it as custom if it's not.
                                // But for simplicity, we'll update the main metrics map if key exists.
                                if (metrics.containsKey(key)) {
                                    metrics[key] = value.toString()
                                }
                            }
                            showAnalysisDialog = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Metrics updated (Merged)")
                            }
                        }
                    ) {
                        Text("Merge")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            onClick = {
                                // Replace: Clear and use AI values
                                // Clear existing template metrics
                                metrics.keys.forEach { metrics[it] = "" }
                                analysisMetrics.forEach { (key, value) ->
                                    if (metrics.containsKey(key)) {
                                        metrics[key] = value.toString()
                                    }
                                }
                                showAnalysisDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar("Metrics updated (Replaced)")
                                }
                            }
                        ) {
                            Text("Replace")
                        }
                        TextButton(onClick = { showAnalysisDialog = false }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }
    }
}

/**
 * Parse metrics JSON string to a map.
 * Named uniquely to avoid conflict during partial edits.
 */
private fun editParseMetrics(metricsJson: String): Map<String, Any> {
    return try {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        Gson().fromJson(metricsJson, type) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

/**
 * Parse template fields JSON string to a map.
 * Named uniquely to avoid conflict during partial edits.
 */
private fun editParseTemplateFields(templateFieldsJson: String): Map<String, String> {
    return try {
        val type = object : TypeToken<Map<String, String>>() {}.type
        Gson().fromJson(templateFieldsJson, type) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

// Helper functions for Date/Time Picker

private fun formatDateTime(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

private fun showDateTimePicker(
    context: android.content.Context,
    initialTimestamp: Long?,
    onDateTimeSelected: (Long) -> Unit
) {
    val calendar = java.util.Calendar.getInstance()
    if (initialTimestamp != null) {
        calendar.timeInMillis = initialTimestamp
    }

    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(java.util.Calendar.YEAR, year)
            calendar.set(java.util.Calendar.MONTH, month)
            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)

            android.app.TimePickerDialog(
                context,
                { _, hourOfDay, minute ->
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                    calendar.set(java.util.Calendar.MINUTE, minute)
                    onDateTimeSelected(calendar.timeInMillis)
                },
                calendar.get(java.util.Calendar.HOUR_OF_DAY),
                calendar.get(java.util.Calendar.MINUTE),
                false // 12-hour format preferred
            ).show()
        },
        calendar.get(java.util.Calendar.YEAR),
        calendar.get(java.util.Calendar.MONTH),
        calendar.get(java.util.Calendar.DAY_OF_MONTH)
    ).show()
}
