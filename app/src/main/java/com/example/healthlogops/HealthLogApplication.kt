package com.example.healthlogops

import android.app.Application
import com.example.healthlogops.data.local.AppDatabase
import com.example.healthlogops.data.repository.HealthLogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class HealthLogApplication : Application() {
    // No need to cancel this scope as it'll be torn down with the process
    val applicationScope = CoroutineScope(SupervisorJob())

    // Using by lazy so the database and the repository are only created when they're needed
    // rather than when the application starts
    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { HealthLogRepository(database.categoryDao(), database.healthLogDao()) }
}
