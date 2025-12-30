
package com.example.healthlogops.ui.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * A slide-down calendar view for selecting dates.
 */
@Composable
fun CalendarView(
    selectedDate: Date,
    activityDates: Set<Long>,
    onDateSelected: (Date) -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentMonth by remember { mutableStateOf(selectedDate) }
    val calendar = Calendar.getInstance()
    
    // Update current month when selected date changes externally
    LaunchedEffect(selectedDate) {
        val selCal = Calendar.getInstance().apply { time = selectedDate }
        val curCal = Calendar.getInstance().apply { time = currentMonth }
        if (selCal.get(Calendar.MONTH) != curCal.get(Calendar.MONTH) || 
            selCal.get(Calendar.YEAR) != curCal.get(Calendar.YEAR)) {
            currentMonth = selectedDate
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Month Year + Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    calendar.time = currentMonth
                    calendar.add(Calendar.MONTH, -1)
                    currentMonth = calendar.time
                }) {
                    Icon(Icons.Default.ChevronLeft, "Previous Month")
                }

                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = {
                    calendar.time = currentMonth
                    calendar.add(Calendar.MONTH, 1)
                    currentMonth = calendar.time
                }) {
                    Icon(Icons.Default.ChevronRight, "Next Month")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Days of Week
            Row(modifier = Modifier.fillMaxWidth()) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
    
            // Calendar Grid
            val daysInMonth = getDaysInMonth(currentMonth)
            val rows = daysInMonth.chunked(7)
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Grid Logic
                val monthStart = Calendar.getInstance().apply {
                    time = currentMonth
                    set(Calendar.DAY_OF_MONTH, 1)
                }
                val daysInMonthCount = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)
                val dayOfWeekStart = monthStart.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sun=0)
                
                var currentDay = 1
                val totalCells = (daysInMonthCount + dayOfWeekStart + 6) / 7 * 7 // Round up to full weeks

               for (row in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            if (cellIndex >= dayOfWeekStart && currentDay <= daysInMonthCount) {
                                val date = Calendar.getInstance().apply {
                                    time = currentMonth
                                    set(Calendar.DAY_OF_MONTH, currentDay)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.time

                                val isSelected = isSameDay(selectedDate, date)
                                val isToday = isSameDay(Date(), date)
                                val hasActivity = activityDates.contains(date.time) // date is already normalized to midnight in the loop? Check implementation below. 
                                // Actually the date created in the loop sets hours/min/sec to 0, so 'date.time' is correct for comparison if activityDates are also normalized.
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isToday -> MaterialTheme.colorScheme.primaryContainer
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (hasActivity && !isSelected) {
                                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentDay.toString(),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                        fontWeight = if (isSelected || isToday || hasActivity) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                currentDay++
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Jump to Today
            OutlinedButton(
                onClick = { 
                    onJumpToToday()
                    currentMonth = Date() // Reset view to current month too
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Today, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Jump to Today")
            }
        }
    }
}

private fun getDaysInMonth(date: Date): List<Date> {
    val calendar = Calendar.getInstance()
    calendar.time = date
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val month = calendar.get(Calendar.MONTH)
    val days = mutableListOf<Date>()
    
    while (calendar.get(Calendar.MONTH) == month) {
        days.add(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    return days
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
