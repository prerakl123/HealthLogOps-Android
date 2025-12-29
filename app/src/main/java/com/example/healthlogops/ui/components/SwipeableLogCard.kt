package com.example.healthlogops.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.healthlogops.data.model.LogWithCategory
import com.example.healthlogops.ui.viewmodel.ViewMode
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * A swipeable wrapper for LogCard that provides Gmail-style swipe gestures.
 *
 * Features:
 * - Swipe right (15-20% of screen width) to edit
 * - Swipe left (15-20% of screen width) to delete
 * - Visual feedback with colored background and icons during swipe
 * - Snap-back animation when released without completing swipe
 *
 * The action only triggers when:
 * 1. User has swiped past the threshold (17.5% screen width)
 * 2. User has lifted their finger
 *
 * @param logWithCategory The log entry with its associated category
 * @param onEdit Callback when swipe-to-edit is completed
 * @param onDelete Callback when swipe-to-delete is completed
 * @param modifier Modifier for the swipeable card
 */
@Composable
fun SwipeableLogCard(
    logWithCategory: LogWithCategory,
    onEdit: (Int) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewMode: ViewMode = ViewMode.BALANCED,
    onLongPress: (() -> Unit)? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidthPx = with(LocalDensity.current) { configuration.screenWidthDp.dp.toPx() }
    
    // Swipe threshold as percentage of screen width (17.5%)
    val swipeThreshold = screenWidthPx * 0.175f
    
    // Track the current offset
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    
    // Colors for swipe backgrounds
    val editColor = Color(0xFF2196F3) // Blue for edit
    val deleteColor = Color(0xFFE84A3F) // Red for delete
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val currentOffset = offsetX.value
                        
                        // Check if threshold was reached
                        if (currentOffset.absoluteValue >= swipeThreshold) {
                            // Complete the swipe animation
                            scope.launch {
                                val target = if (currentOffset > 0) screenWidthPx else -screenWidthPx
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = tween(durationMillis = 150)
                                )
                                
                                // Trigger the action
                                if (currentOffset > 0) {
                                    onEdit(logWithCategory.log.id)
                                } else {
                                    onDelete(logWithCategory.log.id)
                                }
                                
                                // Reset position
                                offsetX.snapTo(0f)
                            }
                        } else {
                            // Snap back to original position
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = tween(durationMillis = 200)
                                )
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch {
                            val newOffset = (offsetX.value + dragAmount).coerceIn(
                                -swipeThreshold * 1.2f,
                                swipeThreshold * 1.2f
                            )
                            offsetX.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        // Background action areas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .matchParentSize()
        ) {
            // Edit action (left side, shown on right swipe)
            if (offsetX.value > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(LocalDensity.current) { offsetX.value.toDp() })
                        .background(editColor, RoundedCornerShape(12.dp))
                        .align(Alignment.CenterStart),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .size(24.dp)
                    )
                }
            }
            
            // Delete action (right side, shown on left swipe)
            if (offsetX.value < 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(with(LocalDensity.current) { offsetX.value.absoluteValue.toDp() })
                        .background(deleteColor, RoundedCornerShape(12.dp))
                        .align(Alignment.CenterEnd),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp)
                    )
                }
            }
        }
        
        // The actual log card (on top)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
        ) {
            LogCard(
                logWithCategory = logWithCategory,
                viewMode = viewMode,
                onLongPress = onLongPress
            )
        }
    }
}
