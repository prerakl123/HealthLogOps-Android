package com.example.healthlogops.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface AnalysisApi {
    @POST("analysis/meal")
    suspend fun analyzeMeal(@Body request: AnalysisRequest): MealAnalysisResponse
}
