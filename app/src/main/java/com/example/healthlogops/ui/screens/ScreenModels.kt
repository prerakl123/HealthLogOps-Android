package com.example.healthlogops.ui.screens

/**
 * Data class for custom field state management.
 * Used in both AddLogScreen and EditLogScreen.
 */
data class CustomFieldData(
    val id: Int,
    val name: String,
    val value: String
)
