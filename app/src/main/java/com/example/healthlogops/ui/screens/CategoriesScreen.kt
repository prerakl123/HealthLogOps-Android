package com.example.healthlogops.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.example.healthlogops.data.local.Category
import com.example.healthlogops.ui.components.getCategoryIcon
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch

/**
 * Categories screen providing a reference document for all health log categories.
 * 
 * Features:
 * - Table of contents for quick navigation
 * - Detailed list of categories with descriptions
 * - Breakdown of parameters for each category
 * - Examples for each category
 * - Uneditable, document-style layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val categories by viewModel.allCategories.collectAsState()
    val scrollState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Health Categories Guide",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Document Header
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Reference Guide",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Learn about the available categories and how to log your data effectively.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Table of Contents
            item {
                TOCSection(categories) { index ->
                    scope.launch {
                        // Offset by 2 because:
                        // 1. Header is at index 0
                        // 2. TOC is at index 1
                        scrollState.animateScrollToItem(index + 2)
                    }
                }
            }

            // Categories Details
            items(categories.size) { index ->
                val category = categories[index]
                CategoryDocumentItem(category)
            }
            
            // Footer spacer
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

@Composable
fun TOCSection(categories: List<Category>, onItemClick: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Table of Contents",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            categories.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(index) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                Color(category.color.toColorInt()),
                                RoundedCornerShape(4.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryDocumentItem(category: Category) {
    val template = try {
        Gson().fromJson(category.templateJson, Map::class.java) as? Map<String, String>
    } catch (_: Exception) {
        null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Surface(
                color = Color(category.color.toColorInt()).copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getCategoryIcon(category.name),
                        contentDescription = null,
                        tint = Color(category.color.toColorInt()),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Text(
            text = getCategoryDescription(category.name),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "PARAMETERS",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            template?.forEach { (key, type) ->
                ParameterItem(key, type, getParameterDescription(category.name, key))
            }
            
            if (template.isNullOrEmpty()) {
                Text(
                    text = "No specific parameters for this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "TYPICAL EXAMPLE",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = getCategoryExample(category.name),
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    }
}

@Composable
fun ParameterItem(name: String, type: String, description: String) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = name.replace("_", " ").split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getCategoryDescription(name: String): String = when (name) {
    "Strength Training" -> "Track your weightlifting and resistance exercises to monitor progress in strength and muscle growth."
    "Cardio" -> "Record your aerobic activities like running, cycling, or swimming to track endurance and cardiovascular health."
    "Meal" -> "Log your nutritional intake to stay mindful of calories and macronutrients throughout the day."
    "Water Intake" -> "Maintain hydration by tracking the amount of water you consume routinely."
    "Sleep" -> "Monitor your rest patterns and sleep quality to ensure proper recovery and cognitive health."
    "Weight Log" -> "Keep a record of your body weight over time to visualize trends and reach your physical goals."
    "Daily Steps" -> "Track your daily movement and activity level through step counts and estimated distance."
    "Bowel Movement" -> "Monitor digestive health by logging consistency, frequency, and other physical characteristics."
    else -> "A custom category for logging specific health data tailored to your needs."
}

private fun getParameterDescription(category: String, param: String): String = when (param) {
    "sets" -> "The number of series performed for this specific exercise."
    "reps" -> "The number of repetitions completed within each individual set."
    "weight_kg" -> "The weight used for the exercise (or your current body weight) in kilograms."
    "duration_min" -> "The total time spent on the activity in minutes."
    "distance_km" -> "The total distance covered during the session in kilometers."
    "incline_percent" -> "The vertical slope setting used during the activity (e.g., on a treadmill)."
    "speed_kmh" -> "The average speed maintained during the session in km/h."
    "avg_heart_rate" -> "Your average pulses per minute during the exercise period."
    "calories" -> "The total energy content of the meal in kilocalories."
    "protein_g" -> "Grams of protein consumed in the meal."
    "carbs_g" -> "Grams of total carbohydrates consumed."
    "fat_g" -> "Total grams of fat consumed."
    "fibers_g" -> "Grams of dietary fiber for digestive health."
    "good_fat_g" -> "Grams of unsaturated / healthy fats (like Omega-3s)."
    "supplements" -> "Any vitamins, minerals, or other supplements taken with the meal."
    "glasses" -> "The number of standard 250ml glasses of water consumed."
    "hours" -> "Total duration of sleep in hours."
    "quality_1_10" -> "Subjective rating of how well you rested (1 being poor, 10 being excellent)."
    "steps" -> "Total step count for the entire day."
    "calories_burned" -> "Estimated energy expenditure from movement throughout the day."
    "bristol_type_1_7" -> "The Bristol Stool Scale type (1-7) indicating stool consistency."
    "consistency" -> "Descriptive text about the texture or form (e.g., hard, soft, loose)."
    "color" -> "The color of the stool (can indicate digestive health)."
    "ease" -> "How easy or difficult the passing was (e.g., strained, effortless)."
    "urgency" -> "The level of need to go quickly (e.g., normal, sudden, urgent)."
    "bloat_1_10" -> "Rating of abdominal bloating sensation (1 being none, 10 being severe)."
    else -> "Additional metric tracked for this category."
}

private fun getCategoryExample(name: String): String = when (name) {
    "Strength Training" -> "Bench Press: 3 sets, 10 reps, 60kg. Notes: Slow controlled reps."
    "Cardio" -> "Morning Run: 30 minutes, 5.2km, 145 bpm avg. heart rate."
    "Meal" -> "Grilled Chicken Salad: 450 kcal, 35g Protein, 15g Carbs, 10g Fiber."
    "Water Intake" -> "Post-Workout: 3 glasses of water."
    "Sleep" -> "Last night: 7.5 hours, Quality 9/10. Woke up feeling refreshed."
    "Weight Log" -> "Morning weigh-in: 75.4kg. Taken before breakfast."
    "Daily Steps" -> "Total for Monday: 10,250 steps, approximately 8.1km."
    "Bowel Movement" -> "Morning: Type 4 on Bristol scale, easy to pass, brown color."
    else -> "Logged activity: 30 minutes of meditation."
}
