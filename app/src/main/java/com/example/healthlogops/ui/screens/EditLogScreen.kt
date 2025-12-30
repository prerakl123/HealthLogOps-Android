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

    // Load the log data
    LaunchedEffect(logId) {
        scope.launch {
            val loadedLog = viewModel.getLogById(logId)
            if (loadedLog != null) {
                log = loadedLog
                activityName = loadedLog.activityName
                notes = loadedLog.notes ?: ""

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

                    // Notes
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notes (Optional)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = notes,
                                onValueChange = { notes = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                placeholder = { Text("Add any additional notes...") },
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
                                            notes = notes.ifBlank { null }
                                        )

                                        viewModel.updateLog(updatedLog)
                                        snackbarHostState.showSnackbar("Activity updated!")
                                        isSaving = false
                                        onNavigateBack()
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
