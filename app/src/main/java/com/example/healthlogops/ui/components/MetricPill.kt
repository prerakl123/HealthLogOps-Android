package com.example.healthlogops.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.InsertComment
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A pill/badge component for displaying a single metric value.
 *
 * @param metricKey The name of the metric (e.g., "duration_min")
 * @param metricValue The value to display
 * @param accentColor The primary color for the pill border and text
 * @param lightColor The light background color for the pill
 */
@Composable
fun MetricPill(
    metricKey: String,
    metricValue: String,
    accentColor: Color,
    lightColor: Color,
    modifier: Modifier = Modifier
) {
    val icon = getMetricIcon(metricKey)
    
    Row(
        modifier = modifier
            .background(lightColor, RoundedCornerShape(12.dp))
            .border(1.dp, accentColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = metricValue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

/**
 * A pill showing "+n" to indicate additional metrics not displayed.
 *
 * @param count The number of additional metrics
 * @param accentColor The primary color for the pill
 * @param lightColor The light background color for the pill
 */
@Composable
fun OverflowPill(
    count: Int,
    accentColor: Color,
    lightColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(lightColor, RoundedCornerShape(12.dp))
            .border(1.dp, accentColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+$count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

/**
 * Get the appropriate icon for a metric key.
 */
private fun getMetricIcon(metricKey: String): ImageVector {
    return when (metricKey.lowercase()) {
        "sets", "reps" -> Icons.Default.FitnessCenter
        "weight_kg", "weight" -> Icons.Default.MonitorWeight
        "duration_min", "duration", "hours" -> Icons.Default.Timer
        "distance_km", "distance" -> Icons.Default.Straighten
        "calories", "calories_burned" -> Icons.Default.LocalFireDepartment
        "protein_g", "carbs_g", "fat_g", "fibers_g", "good_fat_g" -> Icons.Default.Restaurant
        "quality_1_10", "quality" -> Icons.Default.Star
        "glasses" -> Icons.Default.WaterDrop
        "steps" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "speed_kmh", "speed" -> Icons.Default.Speed
        "incline_percent", "incline" -> Icons.Default.Terrain
        "avg_heart_rate", "heart_rate" -> Icons.Default.Favorite
        "supplements" -> Icons.Default.Medication
        "bristol_type_1_7", "type" -> Icons.Default.BarChart
        "consistency", "ease", "urgency" -> Icons.Default.Description
        "bloat_1_10", "bloat" -> Icons.AutoMirrored.Filled.InsertComment
        else -> Icons.Default.CheckCircle
    }
}

/**
 * Get the appropriate icon for a category.
 */
fun getCategoryIcon(categoryName: String): ImageVector {
    return when (categoryName) {
        "Strength Training" -> Icons.Default.FitnessCenter
        "Cardio" -> Icons.AutoMirrored.Filled.DirectionsRun
        "Meal" -> Icons.Default.Restaurant
        "Sleep" -> Icons.Default.Bedtime
        "Water Intake" -> Icons.Default.WaterDrop
        "Weight Log" -> Icons.Default.MonitorWeight
        "Daily Steps" -> Icons.AutoMirrored.Filled.DirectionsWalk
        "Bowel Movement" -> Icons.Default.HistoryEdu // Stylized 'log' icon
        else -> Icons.Default.Circle
    }
}
