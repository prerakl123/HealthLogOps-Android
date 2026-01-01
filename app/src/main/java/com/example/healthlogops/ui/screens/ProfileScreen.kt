package com.example.healthlogops.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.healthlogops.ui.viewmodel.AuthViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.currentUser.collectAsState()
    val scope = rememberCoroutineScope()
    val gson = remember { Gson() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // View state to toggle between Dashboard and Form
    var showMetadataForm by remember { mutableStateOf(false) }

    // Editable states (synced with user data)
    var age by remember(user) { mutableStateOf(user?.age?.toString() ?: "") }
    var height by remember(user) { mutableStateOf(user?.height?.toString() ?: "") }
    var weight by remember(user) { mutableStateOf(user?.weight?.toString() ?: "") }
    
    // Other parameters as a Map
    var otherParams by remember(user) {
        mutableStateOf(
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                gson.fromJson<Map<String, String>>(user?.profileParameters ?: "{}", type) ?: emptyMap()
            } catch (_: Exception) {
                emptyMap<String, String>()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showMetadataForm) "Lifecycle Metadata" else "My Profile") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showMetadataForm) showMetadataForm = false else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (showMetadataForm) {
                        IconButton(onClick = {
                            scope.launch {
                                val updatedUser = user?.copy(
                                    age = age.toIntOrNull(),
                                    height = height.toFloatOrNull(),
                                    weight = weight.toFloatOrNull(),
                                    profileParameters = gson.toJson(otherParams)
                                )
                                if (updatedUser != null) {
                                    viewModel.updateUser(updatedUser)
                                    android.widget.Toast.makeText(context, "Life metadata updated!", android.widget.Toast.LENGTH_SHORT).show()
                                    showMetadataForm = false
                                }
                            }
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { padding ->
        // Use AnimatedVisibility for smooth transition
        Box(modifier = Modifier.padding(padding)) {
            // Dashboard View
            AnimatedVisibility(
                visible = !showMetadataForm,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ProfileDashboard(
                    userEmail = user?.email ?: "",
                    userId = user?.userId ?: "User",
                    onLogout = {
                        scope.launch {
                            viewModel.logout()
                            onNavigateBack()
                        }
                    },
                    onOpenForm = { showMetadataForm = true },
                    onStartAnalysis = {
                        android.widget.Toast.makeText(context, "Analysis started on server...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }

            // Metadata Form View
            AnimatedVisibility(
                visible = showMetadataForm,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                MetadataFormView(
                    age = age,
                    onAgeChange = { age = it },
                    height = height,
                    onHeightChange = { height = it },
                    weight = weight,
                    onWeightChange = { weight = it },
                    otherParams = otherParams,
                    onParamChange = { key, value ->
                        otherParams = otherParams.toMutableMap().apply { put(key, value) }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileDashboard(
    userEmail: String,
    userId: String,
    onLogout: () -> Unit,
    onOpenForm: () -> Unit,
    onStartAnalysis: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Profile Header with Placeholder Pic
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(70.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            // Edit Badge icon
            Surface(
                onClick = {}, // Placeholder for pic edit
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                tonalElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile Pic",
                    modifier = Modifier.padding(6.dp),
                    tint = Color.White
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "@$userId",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Quick Actions (Logout up here)
        PremiumButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(0.9f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Logout", fontWeight = FontWeight.Bold)
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        // Large Dashboard Cards
        DashboardCard(
            title = "Personal Lifestyle Metadata",
            description = "Manage 60+ biological, habitual, and environmental markers for AI analysis.",
            icon = Icons.AutoMirrored.Filled.FactCheck,
            onClick = onOpenForm,
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )

        DashboardCard(
            title = "Start Advanced Health Analysis",
            description = "Trigger predictive modeling to identify trends and risks.",
            icon = Icons.Default.Analytics,
            onClick = onStartAnalysis,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )

        Spacer(modifier = Modifier.height(20.dp))
        
        TextButton(onClick = {}, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.DeleteForever, contentDescription = null)
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete Account & All Data")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataFormView(
    age: String,
    onAgeChange: (String) -> Unit,
    height: String,
    onHeightChange: (String) -> Unit,
    weight: String,
    onWeightChange: (String) -> Unit,
    otherParams: Map<String, String>,
    onParamChange: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Basic Info Header
        SectionHeader(title = "Physical Baselines", icon = Icons.Default.AccessibilityNew)
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = age,
                onValueChange = onAgeChange,
                label = { Text("Age") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
            OutlinedTextField(
                value = height,
                onValueChange = onHeightChange,
                label = { Text("Height (cm)") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }
        
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        // Tiered Metadata Sections from config
        USER_METADATA_CONFIG.forEach { category ->
            SectionHeader(title = category.title, icon = category.icon)
            
            category.questions.forEach { question ->
                MetadataDropdown(
                    label = question.label,
                    description = question.description,
                    selectedOption = otherParams[question.key] ?: "",
                    options = question.options,
                    onOptionSelected = { option -> onParamChange(question.key, option) }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DashboardCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    containerColor: Color
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataDropdown(
    label: String,
    description: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedOption,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Select $label") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                shape = RoundedCornerShape(12.dp)
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PremiumButton(onClick: () -> Unit, modifier: Modifier = Modifier, variant: ButtonVariant = ButtonVariant.Filled, colors: ButtonColors? = null, content: @Composable RowScope.() -> Unit) {
    if (variant == ButtonVariant.Tonal) {
        FilledTonalButton(onClick = onClick, modifier = modifier, content = content)
    } else {
        Button(onClick = onClick, modifier = modifier, colors = colors ?: ButtonDefaults.buttonColors(), content = content)
    }
}

enum class ButtonVariant { Filled, Tonal }
