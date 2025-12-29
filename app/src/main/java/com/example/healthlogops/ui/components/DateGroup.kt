package com.example.healthlogops.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthlogops.data.model.LogWithCategory
import com.example.healthlogops.ui.viewmodel.ViewMode
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.*

/**
 * A component that groups logs by date with a date header.
 *
 * @param date The date for this group
 * @param logs List of logs for this date
 * @param onEdit Callback when a log is swiped to edit
 * @param onDelete Callback when a log is swiped to delete
 */
@Composable
fun DateGroup(
    date: Date,
    logs: List<LogWithCategory>,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewMode: ViewMode = ViewMode.BALANCED,
    onLongPress: ((Int) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(true) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = formatDateHeader(date),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    text = "${logs.size} log${if (logs.size > 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        // Log cards
        if (isExpanded) {
            logs.forEach { logWithCategory ->
                SwipeableLogCard(
                    logWithCategory = logWithCategory,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    viewMode = viewMode,
                    onLongPress = { onLongPress?.invoke(logWithCategory.log.id) }
                )
            }
        }
    }
}

/**
 * Format date for the header (e.g., "Today", "Yesterday", "Dec 25, 2024")
 */
private fun formatDateHeader(date: Date): String {
    val calendar = Calendar.getInstance()
    val today = calendar.time
    
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val yesterday = calendar.time
    
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    return when {
        isSameDay(date, today) -> "Today"
        isSameDay(date, yesterday) -> "Yesterday"
        else -> dateFormat.format(date)
    }
}

/**
 * Check if two dates are on the same day.
 */
private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
