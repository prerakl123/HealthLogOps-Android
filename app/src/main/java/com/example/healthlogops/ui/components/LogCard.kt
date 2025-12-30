package com.example.healthlogops.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.example.healthlogops.data.model.LogWithCategory
import com.example.healthlogops.ui.viewmodel.ViewMode
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

/**
 * A beautifully styled card component for displaying a health log entry.
 *
 * Features a category-colored accent strip on the left, a centered category icon,
 * activity name, time, and metric pills that automatically show "+n" for overflow.
 *
 * @param logWithCategory The log entry with its associated category
 * @param onLongPress Optional callback when card is long pressed
 * @param modifier Modifier for the card
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LogCard(
    logWithCategory: LogWithCategory,
    onLongPress: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    viewMode: ViewMode = ViewMode.BALANCED
) {
    val log = logWithCategory.log
    val category = logWithCategory.category
    
    // Get category colors
    val accentColor = Color(category.color.toColorInt())
    val lightColor = accentColor.copy(alpha = 0.15f)
    
    // Parse metrics from JSON
    val metrics = parseMetrics(log.metricsJson)
    
    // Format timestamp
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(log.timestamp))
    
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { /* Default click could open details or edit */ },
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isDark) androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)) else null,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left accent strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )
            
            if (viewMode == ViewMode.COMPACT) {
                // Truly COMPACT mode: Single line side-by-side
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = getCategoryIcon(category.name),
                        contentDescription = category.name,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    
                    Text(
                        text = if (viewMode == ViewMode.DETAILED) "${log.activityName} (${category.name})" else log.activityName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    
                    if (metrics.isNotEmpty()) {
                        val firstMetric = metrics.entries.first()
                        Text(
                            text = formatMetricValue(firstMetric.key, firstMetric.value),
                            fontSize = 13.sp,
                            color = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = timeString,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            } else {
                // BALANCED and DETAILED modes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // Category icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                accentColor.copy(alpha = 0.1f),
                                RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.name),
                            contentDescription = category.name,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Text content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Top row: Activity name + Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (viewMode == ViewMode.DETAILED) "${log.activityName} (${category.name})" else log.activityName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Text(
                                text = timeString,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // Metrics row with pills
                        if (metrics.isNotEmpty()) {
                            MetricsRow(
                                metrics = metrics,
                                accentColor = accentColor,
                                lightColor = lightColor,
                                // Balanced shows 3, Detailed shows all
                                maxVisible = if (viewMode == ViewMode.DETAILED) 10 else 3
                            )
                        }
                        
                        // Notes (if available)
                        log.notes?.let { notes ->
                            if (notes.isNotBlank()) {
                                Text(
                                    text = if (viewMode == ViewMode.DETAILED) notes else (notes.take(100) + if (notes.length > 100) "..." else ""),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * Row displaying metric pills with overflow handling.
 * Uses FlowRow for detailed view to wrap pills.
 */
@OptIn( ExperimentalLayoutApi::class, ExperimentalLayoutApi::class)
@Composable
private fun MetricsRow(
    metrics: Map<String, Any>,
    accentColor: Color,
    lightColor: Color,
    maxVisible: Int = 3
) {
    // Display metrics, show "+n" for overflow
    val visibleMetrics = metrics.entries.take(maxVisible)
    val overflowCount = (metrics.size - maxVisible).coerceAtLeast(0)
    
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        visibleMetrics.forEach { (key, value) ->
            MetricPill(
                metricKey = key,
                metricValue = formatMetricValue(key, value),
                accentColor = accentColor,
                lightColor = lightColor
            )
        }
        
        if (overflowCount > 0) {
            OverflowPill(
                count = overflowCount,
                accentColor = accentColor,
                lightColor = lightColor
            )
        }
    }
}

/**
 * Parse metrics JSON string to a map.
 */
private fun parseMetrics(metricsJson: String): Map<String, Any> {
    return try {
        val type = object : TypeToken<Map<String, Any>>() {}.type
        Gson().fromJson(metricsJson, type) ?: emptyMap()
    } catch (_: Exception) {
        emptyMap()
    }
}

/**
 * Format metric value for display.
 */
@SuppressLint("DefaultLocale")
private fun formatMetricValue(key: String, value: Any): String {
    return when (value) {
        is Double -> {
            // Format floats nicely
            if (value % 1.0 == 0.0) {
                value.toInt().toString()
            } else {
                String.format("%.1f", value)
            }
        }
        is Int -> value.toString()
        is String -> value
        else -> value.toString()
    }
}
