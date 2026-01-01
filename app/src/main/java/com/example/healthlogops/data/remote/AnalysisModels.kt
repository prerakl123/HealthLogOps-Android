package com.example.healthlogops.data.remote

data class AnalysisRequest(
    val notes: String
)

data class MealAnalysisResponse(
    val status: String,
    val metrics: Map<String, Any>? = null,
    val reason: String? = null,
    val message: String? = null
)
