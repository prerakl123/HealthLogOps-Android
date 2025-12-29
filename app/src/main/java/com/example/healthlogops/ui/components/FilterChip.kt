package com.example.healthlogops.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A filter chip button for filtering logs by category.
 *
 * Displays a rounded chip with text that can be in active or inactive state.
 * Active chips have a teal background with white text, while inactive chips
 * have a light gray background with dark text.
 *
 * @param text The filter label text (e.g., "All", "Cardio", "Meal")
 * @param isActive Whether this filter is currently active/selected
 * @param onClick Callback invoked when the chip is tapped
 * @param modifier Optional modifier for the chip container
 */
@Composable
fun FilterChip(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isActive) {
        Color(0xFF00897B) // Teal - matches Kivy app active color
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = if (isActive) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}
