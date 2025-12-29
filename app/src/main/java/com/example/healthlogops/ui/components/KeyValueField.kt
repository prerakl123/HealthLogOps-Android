package com.example.healthlogops.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A styled key-value input field with label and text input.
 *
 * Displays a field name in a colored label container on the left
 * and a text input on the right. Optionally includes a remove button
 * for custom fields.
 *
 * @param fieldName The display name for this field
 * @param fieldType The data type of this field (int, float, str)
 * @param value Current value of the field
 * @param onValueChange Callback when value changes
 * @param isCustom Whether this is a custom removable field
 * @param onRemove Callback when remove button is clicked (for custom fields)
 * @param onFieldNameChange Callback when field name changes (for custom fields)
 */
@Composable
fun KeyValueField(
    fieldName: String,
    fieldType: String = "str",
    value: String,
    onValueChange: (String) -> Unit,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    isCustom: Boolean = false,
    onRemove: (() -> Unit)? = null,
    customFieldName: String = "",
    onFieldNameChange: ((String) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCustom) {
            // Editable field name for custom fields
            OutlinedTextField(
                value = customFieldName,
                onValueChange = { onFieldNameChange?.invoke(it) },
                modifier = Modifier.weight(0.35f),
                placeholder = { Text("Field name", fontSize = 14.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors()
            )
        } else {
            // Fixed label for template fields
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .fillMaxHeight()
                    .background(
                        color = Color(0xFF00897B).copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = fieldName,
                    color = Color(0xFF00796B),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
        
        // Value input field
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val filteredValue = when (fieldType.lowercase()) {
                    "int" -> newValue.filter { it.isDigit() || it == '-' }
                    "float" -> newValue.filter { it.isDigit() || it == '.' || it == '-' }
                    else -> newValue
                }
                
                // Allow only one decimal point for float
                if (fieldType.lowercase() == "float" && filteredValue.count { it == '.' } > 1) {
                    return@OutlinedTextField
                }
                
                onValueChange(filteredValue)
            },
            modifier = Modifier.weight(if (isCustom) 0.45f else 0.6f),
            placeholder = { 
                Text(
                    when (fieldType.lowercase()) {
                        "int" -> "Number"
                        "float" -> "Decimal"
                        else -> "Enter value"
                    },
                    fontSize = 14.sp
                )
            },
            singleLine = true,
            keyboardOptions = when (fieldType.lowercase()) {
                "int" -> KeyboardOptions(keyboardType = KeyboardType.Number)
                "float" -> KeyboardOptions(keyboardType = KeyboardType.Decimal)
                else -> KeyboardOptions(keyboardType = KeyboardType.Text)
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00897B),
                unfocusedBorderColor = Color(0xFF00897B).copy(alpha = 0.3f)
            )
        )
        
        // Remove button for custom fields
        if (isCustom && onRemove != null) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove field",
                    tint = Color(0xFFCC4C4C).copy(alpha = 0.8f)
                )
            }
        }
    }
}
