package com.example.healthlogops.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Category::class, HealthLog::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun healthLogDao(): HealthLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE health_logs ADD COLUMN activity_start_time INTEGER")
                database.execSQL("ALTER TABLE health_logs ADD COLUMN activity_end_time INTEGER")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health_log_ops_db"
                )
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Keep as fallback
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
            // Seeding will be handled by the first time the database is accessed
            // or we can use the db instance here to insert raw SQL if needed,
            // but using DAOs is easier.
        }

        /**
         * Called every time the database is opened.
         */
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            // Ensure categories exist. This is fast and ensures defaults are always there.
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val categoryDao = database.categoryDao()
                    seedDefaultCategories(categoryDao)
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

            // Use IGNORE conflict strategy internally or check count
            for (category in defaultCategories) {
                categoryDao.insertCategory(category)
            }
        }
    }
}

