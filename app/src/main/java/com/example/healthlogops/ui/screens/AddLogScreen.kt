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

                // Notes Section
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Notes (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
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

                                    val healthLog = HealthLog(
                                        categoryId = category.id,
                                        activityName = activityName.ifBlank { category.name },
                                        timestamp = System.currentTimeMillis(),
                                        metricsJson = metricsJson,
                                        notes = notes.ifBlank { null }
                                    )

                                    viewModel.insertLog(healthLog)
                                    snackbarHostState.showSnackbar("Activity logged!")
                                    onNavigateBack()
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
    }
}
