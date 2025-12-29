package com.example.healthlogops.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Category::class, HealthLog::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun healthLogDao(): HealthLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_tracker.db"
                )
                // Remove destructive migration to prevent data loss
                // .fallbackToDestructiveMigration() 
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        /**
         * Called when the database is created for the first time.
         */
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    // Start by seeding categories
                    seedDefaultCategories(database.categoryDao())
                    // On first creation only, we add some sample logs
                    seedSampleLogs(database.categoryDao(), database.healthLogDao())
                }
            }
        }

        /**
         * Called every time the database is opened.
         * Ensures that default categories exist even if the DB was updated or wiped.
         */
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    seedDefaultCategories(database.categoryDao())
                }
            }
        }

        private suspend fun seedDefaultCategories(categoryDao: CategoryDao) {
            val defaultCategories = listOf(
                Category(
                    id = 1,
                    name = "Strength Training",
                    icon = "weight_lifter",
                    color = "#E91E63",
                    templateJson = "{\"sets\": \"int\", \"reps\": \"int\", \"weight_kg\": \"float\"}"
                ),
                Category(
                    id = 2,
                    name = "Cardio",
                    icon = "run",
                    color = "#FF5722",
                    templateJson = "{\"duration_min\": \"int\", \"distance_km\": \"float\", \"incline_percent\": \"float\", \"speed_kmh\": \"float\", \"avg_heart_rate\": \"int\"}"
                ),
                Category(
                    id = 3,
                    name = "Meal",
                    icon = "food",
                    color = "#4CAF50",
                    templateJson = "{\"calories\": \"int\", \"protein_g\": \"float\", \"carbs_g\": \"float\", \"fat_g\": \"float\", \"fibers_g\": \"float\", \"good_fat_g\": \"float\", \"supplements\": \"str\"}"
                ),
                Category(
                    id = 4,
                    name = "Water Intake",
                    icon = "water",
                    color = "#2196F3",
                    templateJson = "{\"glasses\": \"int\"}"
                ),
                Category(
                    id = 5,
                    name = "Sleep",
                    icon = "sleep",
                    color = "#3F51B5",
                    templateJson = "{\"hours\": \"float\", \"quality_1_10\": \"int\"}"
                ),
                Category(
                    id = 6,
                    name = "Weight Log",
                    icon = "scale",
                    color = "#9C27B0",
                    templateJson = "{\"weight_kg\": \"float\"}"
                ),
                Category(
                    id = 7,
                    name = "Daily Steps",
                    icon = "walk",
                    color = "#FF9800",
                    templateJson = "{\"steps\": \"int\", \"distance_km\": \"float\", \"calories_burned\": \"int\"}"
                ),
                Category(
                    id = 8,
                    name = "Bowel Movement",
                    icon = "sailing", // Representing 'log' or movement
                    color = "#795548", // Brown
                    templateJson = "{\"bristol_type_1_7\": \"int\", \"consistency\": \"str\", \"color\": \"str\", \"ease\": \"str\", \"urgency\": \"str\", \"bloat_1_10\": \"int\"}"
                )
            )

            for (category in defaultCategories) {
                // insertCategory uses OnConflictStrategy.REPLACE, so this will
                // update existing categories with defaults if they matches IDs,
                // or just ensure they exist.
                categoryDao.insertCategory(category)
            }
        }

        private suspend fun seedSampleLogs(categoryDao: CategoryDao, healthLogDao: HealthLogDao) {
            // Only seed if there are no logs
            // (Wait, we can't easily check count here without a DAO query, but let's just do it)
            
            val now = System.currentTimeMillis()
            val hour = 60 * 60 * 1000L
            val day = 24 * hour

            val sampleLogs = listOf(
                HealthLog(
                    categoryId = 1,
                    activityName = "Bench Press",
                    timestamp = now,
                    metricsJson = "{\"sets\": 3, \"reps\": 10, \"weight_kg\": 60.0}",
                    notes = "Felt strong today! Increased weight."
                ),
                HealthLog(
                    categoryId = 2,
                    activityName = "Morning Run",
                    timestamp = now - hour,
                    metricsJson = "{\"duration_min\": 30, \"distance_km\": 5.2, \"avg_heart_rate\": 145}",
                    notes = "Beautiful weather"
                ),
                HealthLog(
                    categoryId = 4,
                    activityName = "Hydration",
                    timestamp = now - 2 * hour,
                    metricsJson = "{\"glasses\": 8}",
                    notes = null
                ),
                HealthLog(
                    categoryId = 3,
                    activityName = "Lunch",
                    timestamp = now - day,
                    metricsJson = "{\"calories\": 650, \"protein_g\": 45.0, \"carbs_g\": 60.0, \"fat_g\": 20.0}",
                    notes = "Grilled chicken"
                )
            )

            for (log in sampleLogs) {
                healthLogDao.insertLog(log)
            }
        }
    }
}

