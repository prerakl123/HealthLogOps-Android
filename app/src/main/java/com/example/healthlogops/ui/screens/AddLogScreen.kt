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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlogops.data.local.Category
import com.example.healthlogops.data.local.HealthLog
import com.example.healthlogops.ui.components.KeyValueField
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.google.gson.Gson
import androidx.core.graphics.toColorInt
import kotlinx.coroutines.launch

/**
 * Screen for adding new health log entries.
 *
 * Provides a form interface with:
 * - Category selection dropdown
 * - Activity name input
 * - Dynamic form fields based on category template
 * - Custom field addition capability
 * - Notes section
 * - Save functionality
 *
 * @param viewModel The ViewModel providing data and save operations
 * @param onNavigateBack Callback to navigate back to previous screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLogScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.allCategories.collectAsState()
    val logsWithCategories by viewModel.logsWithCategories.collectAsState()
    val scope = rememberCoroutineScope()

    // Form state
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var activityName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Template field values (from category template)
    var templateFieldValues by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    // Custom fields state
    var customFields by remember { mutableStateOf<List<CustomFieldData>>(emptyList()) }
    var customFieldCounter by remember { mutableIntStateOf(0) }

    // Category dropdown state
    var showCategoryDropdown by remember { mutableStateOf(false) }

    // Activity suggestions state
    var showActivitySuggestions by remember { mutableStateOf(false) }
    val activitySuggestions = remember(selectedCategory, logsWithCategories) {
        selectedCategory?.let { viewModel.getActivitySuggestions(it.id) } ?: emptyList()
    }

    // AI Analysis states
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<com.example.healthlogops.data.remote.MealAnalysisResponse?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isProcessing by viewModel.isProcessing.collectAsState()

    // Initialize template fields when category changes
    LaunchedEffect(selectedCategory) {
        selectedCategory?.let { category ->
            val template = try {
                Gson().fromJson(category.templateJson, Map::class.java) as? Map<String, String>
            } catch (e: Exception) {
                null
            }

            templateFieldValues = template?.keys?.associateWith { "" } ?: emptyMap()
        }
    }

    // Set first category as default
    LaunchedEffect(categories) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.first()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Add Log", fontWeight = FontWeight.Bold) },
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Category Selection
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
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
                                            // Reset custom fields when category changes
                                            customFields = emptyList()
                                            customFieldCounter = 0
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Activity Name
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Activity Name",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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
                                placeholder = { Text("e.g., Morning Run, Bench Press") },
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

                // Template Fields Section
                selectedCategory?.let { category ->
                    val template = try {
                        Gson().fromJson(category.templateJson, Map::class.java) as? Map<String, String>
                    } catch (_: Exception) {
                        null
                    }

                    template?.let { templateMap ->
                        if (templateMap.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Metrics",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }

                            items(templateMap.entries.toList()) { (fieldName, fieldType) ->
                                KeyValueField(
                                    fieldName = fieldName.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    fieldType = fieldType,
                                    value = templateFieldValues[fieldName] ?: "",
                                    onValueChange = { newValue ->
                                        templateFieldValues = templateFieldValues.toMutableMap().apply {
                                            put(fieldName, newValue)
                                        }
                                    },
                                    isCustom = false
                                )
                            }
                        }
                    }
                }

                // Custom Fields
                if (customFields.isNotEmpty()) {
                    item {
                        Text(
                            text = "Custom Fields",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
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

                // Notes Section with AI Analysis
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notes (Optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
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

                // Save Button
                item {
                    Button(
                        onClick = {
                            selectedCategory?.let { category ->
                                scope.launch {
                                    // Build metrics JSON
                                    val metrics = mutableMapOf<String, Any>()

                                    // Add template field values
                                    templateFieldValues.forEach { (key, value) ->
                                        if (value.isNotBlank()) {
                                            val template = try {
                                                Gson().fromJson(
                                                    category.templateJson,
                                                    Map::class.java
                                                ) as? Map<String, String>
                                            } catch (_: Exception) {
                                                null
                                            }
                                            val fieldType = template?.get(key) ?: "str"

                                            metrics[key] = when (fieldType.lowercase()) {
                                                "int" -> value.toIntOrNull() ?: 0
                                                "float" -> value.toDoubleOrNull() ?: 0.0
                                                else -> value
                                            }
                                        }
                                    }

                                    // Add custom field values
                                    customFields.forEach { customField ->
                                        if (customField.name.isNotBlank() && customField.value.isNotBlank()) {
                                            val key = customField.name.lowercase().replace(" ", "_")
                                            // Try to parse as number, otherwise store as string
                                            metrics[key] = customField.value.toIntOrNull()
                                                ?: customField.value.toDoubleOrNull()
                                                        ?: customField.value
                                        }
                                    }

                                    val metricsJson = Gson().toJson(metrics)

                                    // Determine timestamp based on selected date
                                    val selectedDate = viewModel.selectedDate.value
                                    val timestamp = if (selectedDate != null) {
                                        // Use selected date but keep current time of day
                                        val now = java.util.Calendar.getInstance()
                                        val cal = java.util.Calendar.getInstance().apply { time = selectedDate }
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, now.get(java.util.Calendar.HOUR_OF_DAY))
                                        cal.set(java.util.Calendar.MINUTE, now.get(java.util.Calendar.MINUTE))
                                        cal.set(java.util.Calendar.SECOND, now.get(java.util.Calendar.SECOND))
                                        cal.timeInMillis
                                    } else {
                                        System.currentTimeMillis()
                                    }

                                    val healthLog = HealthLog(
                                        categoryId = category.id,
                                        activityName = activityName.ifBlank { category.name },
                                        timestamp = timestamp,
                                        metricsJson = metricsJson,
                                        notes = notes.ifBlank { null }
                                    )

                                    viewModel.insertLog(healthLog)
                                    // Navigate back immediately so the UI feels responsive
                                    onNavigateBack()
                                    // Launch snackbar in a separate job so it doesn't block (though it might be cut off by navigation)
                                    scope.launch { 
                                        snackbarHostState.showSnackbar("Activity logged!") 
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = selectedCategory != null
                    ) {
                        Text("Save Log", style = MaterialTheme.typography.titleMedium)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Loading context overlay
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) {}, // Intercept clicks
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text("Saving...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }

        // Analysis Result Dialog
        if (showAnalysisDialog && analysisResult != null) {
            val metrics = analysisResult?.metrics ?: emptyMap()
            
            AlertDialog(
                onDismissRequest = { showAnalysisDialog = false },
                title = { Text("AI Analysis Result") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Detected nutritional metrics from your notes:")
                        
                        metrics.forEach { (key, value) ->
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
                            val newValues = templateFieldValues.toMutableMap()
                            metrics.forEach { (key, value) ->
                                if (newValues.containsKey(key)) {
                                    newValues[key] = value.toString()
                                }
                            }
                            templateFieldValues = newValues
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
                                val newValues = templateFieldValues.toMutableMap()
                                // Clear existing first (optional, but 'replace' implies this)
                                // Actually, 'replace' in this context usually means replace the specific fields detected.
                                // If the user wants a full replace, we should clear the map first.
                                newValues.keys.forEach { newValues[it] = "" }
                                metrics.forEach { (key, value) ->
                                    if (newValues.containsKey(key)) {
                                        newValues[key] = value.toString()
                                    }
                                }
                                templateFieldValues = newValues
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
