package com.example.healthlogops.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

data class MetadataCategory(
    val title: String,
    val icon: ImageVector,
    val questions: List<MetadataQuestion>
)

data class MetadataQuestion(
    val key: String,
    val label: String,
    val description: String,
    val options: List<String>
)

val USER_METADATA_CONFIG = listOf(
    MetadataCategory(
        title = "Professional & Occupational",
        icon = Icons.Default.Work,
        questions = listOf(
            MetadataQuestion(
                "work_environment",
                "Work Environment",
                "Primary physical setting.",
                listOf("Desk-bound/Office", "Remote (Home)", "Field Work", "Industrial/Factory", "Healthcare/Clinical", "Outdoor/Nature", "Retail/Service")
            ),
            MetadataQuestion(
                "occupational_physicality",
                "Occupational Physicality",
                "Level of movement at work.",
                listOf("Sedentary (Sitting all day)", "Light (Standing/Walking)", "Moderate (Lifting/Active)", "Heavy (Manual labor/Construction)")
            ),
            MetadataQuestion(
                "shift_pattern",
                "Shift Pattern",
                "Timing of work hours.",
                listOf("Fixed Morning", "Fixed Evening", "Night Shift", "Rotating Shifts", "Irregular/On-call", "Flexible")
            ),
            MetadataQuestion(
                "commute_mode",
                "Commute Primary Mode",
                "How you get to work.",
                listOf("Walking", "Cycling", "Public Transport (Active)", "Public Transport (Seated)", "Driving", "No Commute")
            ),
            MetadataQuestion(
                "daily_screen_time",
                "Daily Screen Time",
                "Estimated work screen use.",
                listOf("Low (<3 hrs)", "Moderate (3–6 hrs)", "High (6–9 hrs)", "Extreme (9+ hrs)")
            )
        )
    ),
    MetadataCategory(
        title = "Dietary & Nutritional Baseline",
        icon = Icons.Default.Restaurant,
        questions = listOf(
            MetadataQuestion(
                "diet_type",
                "Primary Diet Type",
                "General eating philosophy.",
                listOf("Omnivore", "Vegetarian", "Vegan", "Pescetarian", "Keto", "Paleo", "Mediterranean", "Carnivore")
            ),
            MetadataQuestion(
                "meal_frequency",
                "Meal Frequency",
                "Typical daily meal count.",
                listOf("1 (OMAD)", "2 meals", "3 meals", "3 meals + snacks", "Grazing (Small frequent meals)")
            ),
            MetadataQuestion(
                "caffeine_sensitivity",
                "Caffeine Sensitivity",
                "Self-reported reaction.",
                listOf("Highly Sensitive", "Moderate", "High Tolerance (No effect)", "Don't Consume")
            ),
            MetadataQuestion(
                "hydration_baseline",
                "Hydration Baseline",
                "Typical daily water intake.",
                listOf("Low (<1L)", "Moderate (1–2L)", "High (2–3L)", "Very High (3L+)")
            ),
            MetadataQuestion(
                "home_cooking_ratio",
                "Home-Cooking Ratio",
                "Frequency of home meals.",
                listOf("Mostly Home-cooked", "Balanced", "Mostly Restaurant/Takeout", "100% Meal-prepped")
            ),
            MetadataQuestion(
                "primary_sugar_source",
                "Primary Sugar Source",
                "Where you get most sugar from.",
                listOf("Added (Sodas/Sweets)", "Natural (Fruit/Honey)", "Complex Carbs", "No/Low Sugar", "Mixed Sources")
            ),
            MetadataQuestion(
                "food_sensitivity",
                "Food Sensitivity",
                "Non-allergic reactions.",
                listOf("None", "Lactose", "Gluten", "Nightshades (Peppers/Toms)", "High-FODMAP", "Cruciferous (Bloating)")
            ),
            MetadataQuestion(
                "cravings_profile",
                "Cravings Profile",
                "Blood sugar/hormonal indicators.",
                listOf("Salty/Savory", "Sweet/Sugary", "Creamy/Fatty", "Starchy/Carbs", "None/Neutral")
            ),
            MetadataQuestion(
                "fasting_baseline",
                "Fasting Baseline",
                "Metabolic window.",
                listOf("None (Grazing)", "12:12", "16:8 (Leangains)", "20:4 (Warrior)", "Multi-day (Occasional)")
            ),
            MetadataQuestion(
                "cooking_oil_base",
                "Cooking Oil Base",
                "Inflammatory marker baseline.",
                listOf("Seed Oils (Canola/Veg)", "Olive/Avocado Oil", "Ghee/Butter", "Coconut Oil", "No Oil (Steamed)")
            ),
            MetadataQuestion(
                "spice_tolerance",
                "Spice Tolerance",
                "Digestive/Thermic impact.",
                listOf("None (Bland)", "Mild", "Medium", "Hot", "Nuclear")
            ),
            MetadataQuestion(
                "late_night_eating",
                "Late Night Eating",
                "Circadian disruption.",
                listOf("Never", "1–2 times/week", "Regular (Daily)", "Only on Weekends")
            )
        )
    ),
    MetadataCategory(
        title = "Physical Activity & Bio-Structure",
        icon = Icons.Default.FitnessCenter,
        questions = listOf(
            MetadataQuestion(
                "dominant_hand",
                "Dominant Hand",
                "Neurological/Action bias.",
                listOf("Right", "Left", "Ambidextrous")
            ),
            MetadataQuestion(
                "joint_health",
                "Joint Health Status",
                "Movement limitations.",
                listOf("No issues", "Clicking/Popping (non-painful)", "Occasional Stiffness", "Chronic Pain (Arthritis)", "Post-Surgical")
            ),
            MetadataQuestion(
                "foot_arch_type",
                "Foot Arch Type",
                "Impact on walking/running data.",
                listOf("High Arch", "Neutral", "Flat Feet (Pronated)")
            ),
            MetadataQuestion(
                "core_stability",
                "Core Stability",
                "Strength baseline for posture.",
                listOf("High (Athlete)", "Average", "Weak (Back pain prone)", "Post-partum")
            ),
            MetadataQuestion(
                "vision_status",
                "Vision Status",
                "Eye strain/Screen time impact.",
                listOf("20/20 (Natural)", "Corrected (Glasses/Contacts)", "Lasik-corrected", "Uncorrected Impairment")
            ),
            MetadataQuestion(
                "activity_level_neat",
                "Activity Level (NEAT)",
                "Non-Exercise Activity Thermogenesis.",
                listOf("Very Low (Bed/Couch)", "Low (Office work)", "Moderate (Active lifestyle)", "High (Always on feet)")
            ),
            MetadataQuestion(
                "primary_exercise_type",
                "Primary Exercise Type",
                "Most frequent activity.",
                listOf("Strength Training", "Cardio/Running", "Yoga/Flexibility", "HIIT", "Sports (Football/Tennis)", "Swimming", "Walking")
            ),
            MetadataQuestion(
                "training_frequency",
                "Training Frequency",
                "Days per week of exercise.",
                listOf("0", "1-2 days", "3-4 days", "5-6 days", "Daily")
            ),
            MetadataQuestion(
                "injury_history",
                "Injury History Status",
                "Current physical limitations.",
                listOf("No Injuries", "Recovered (No limits)", "Chronic (Back/Knee)", "Acute (Recent/Limited)")
            ),
            MetadataQuestion(
                "body_shape_profile",
                "Body Shape Profile",
                "General fat distribution.",
                listOf("Ectomorph (Lean)", "Mesomorph (Athletic)", "Endomorph (Sturdy)", "Apple", "Pear")
            )
        )
    ),
    MetadataCategory(
        title = "Sleep & Circadian Profile",
        icon = Icons.Default.Bedtime,
        questions = listOf(
            MetadataQuestion(
                "chronotype",
                "Chronotype",
                "Internal biological clock.",
                listOf("Early Bird (Morning)", "Night Owl (Evening)", "Neutral/Intermediate")
            ),
            MetadataQuestion(
                "sleep_environment",
                "Sleep Environment",
                "Quality of sleeping space.",
                listOf("Optimized (Cool/Dark/Quiet)", "Urban (Light/Noise)", "Shared Bed (Disturbance)", "Variable")
            ),
            MetadataQuestion(
                "sleep_regularity",
                "Sleep Regularity",
                "Consistency of bedtimes.",
                listOf("Consistent (Same time ±30m)", "Mostly Stable", "Irregular (Weekends vary)", "Highly Chaotic")
            ),
            MetadataQuestion(
                "sleep_latency",
                "Sleep Latency",
                "Time taken to fall asleep.",
                listOf("Fast (<15m)", "Normal (15–30m)", "Slow (30–60m)", "Very Slow (1h+)")
            ),
            MetadataQuestion(
                "napping_habits",
                "Napping Habits",
                "Frequency of day sleep.",
                listOf("Never", "Occasional", "Regular (Daily <30m)", "Regular (Daily >1hr)")
            ),
            MetadataQuestion(
                "mattress_firmness",
                "Mattress Firmness",
                "Physical recovery/back health.",
                listOf("Ultra-Soft", "Medium-Soft", "Firm", "Orthopedic", "Floor/Japanese Mat")
            ),
            MetadataQuestion(
                "bedroom_temp",
                "Bedroom Temp",
                "Circadian regulation.",
                listOf("Cold (<18°C)", "Cool (18–22°C)", "Warm (>22°C)", "Uncontrolled")
            ),
            MetadataQuestion(
                "blue_light_habits",
                "Blue Light Habits",
                "Melatonin suppression.",
                listOf("None (No screens)", "Use Filters (Night Shift)", "Computer Glasses", "Heavy usage until bed")
            ),
            MetadataQuestion(
                "wakeup_method",
                "Wake-up Method",
                "Stress on waking.",
                listOf("Natural (No alarm)", "Sunrise Lamp", "Gentle Audio", "Loud/Shock Alarm", "Multiple Snoozes")
            ),
            MetadataQuestion(
                "bedtime_ritual",
                "Bedtime Ritual",
                "Parasympathetic activation.",
                listOf("Reading", "Meditation", "Hot Shower", "Stretching", "Screens", "None")
            )
        )
    ),
    MetadataCategory(
        title = "Environmental & Psychosocial",
        icon = Icons.Default.Public,
        questions = listOf(
            MetadataQuestion(
                "living_density",
                "Living Density",
                "Noise and air quality context.",
                listOf("High-Rise (City)", "Low-Rise (Urban)", "Single Family (Suburban)", "Rural/Farm")
            ),
            MetadataQuestion(
                "animal_exposure",
                "Animal Exposure",
                "Allergens/Microbiome diversity.",
                listOf("No Pets", "Indoor Cat", "Indoor Dog", "Multiple Pets", "Livestock/Farm Animals")
            ),
            MetadataQuestion(
                "primary_stressor",
                "Primary Stressor",
                "The source of high cortisol.",
                listOf("Work/Career", "Finances", "Family/Relationships", "Health Anxiety", "General/Unspecified")
            ),
            MetadataQuestion(
                "social_battery",
                "Social Battery",
                "Energy baseline.",
                listOf("Extreme Introvert", "Lean Introvert", "Ambivert", "Lean Extrovert", "Extreme Extrovert")
            ),
            MetadataQuestion(
                "digital_consumption",
                "Digital Consumption",
                "Dopamine/Mental fatigue.",
                listOf("News-heavy", "Social Media (Passive)", "Gaming (Active)", "Educational", "Entertainment/Movies")
            ),
            MetadataQuestion(
                "living_environment",
                "Living Environment",
                "Geography and surroundings.",
                listOf("Inner City", "Suburban", "Rural", "Coastal", "High Altitude")
            ),
            MetadataQuestion(
                "climate_preference",
                "Climate Preference",
                "How you handle weather.",
                listOf("Cold-tolerant", "Heat-tolerant", "Sensitive to Humidity", "Dry Air Preferred", "No Preference")
            ),
            MetadataQuestion(
                "stress_baseline",
                "Stress Baseline",
                "General everyday stress level.",
                listOf("Low (Minimal)", "Moderate (Manageable)", "High (Constant)", "Burnout Risk")
            ),
            MetadataQuestion(
                "social_environment",
                "Social Environment",
                "Living arrangements.",
                listOf("Living Alone", "With Partner", "With Family (Kids)", "Roommates")
            ),
            MetadataQuestion(
                "social_interaction",
                "Social Interaction",
                "Daily human engagement.",
                listOf("Solitary (Introverted)", "Moderate", "Social (Extroverted)", "High-Contact (Public-facing)")
            )
        )
    ),
    MetadataCategory(
        title = "Habits & Miscellaneous",
        icon = Icons.Default.Favorite,
        questions = listOf(
            MetadataQuestion(
                "logging_accuracy",
                "Logging Accuracy",
                "Self-reported bias.",
                listOf("Perfectionist (Gram-level)", "Generalist (Estimates)", "Sporadic (Forgetful)", "Automatic (Wearable only)")
            ),
            MetadataQuestion(
                "health_literacy",
                "Health Literacy",
                "User's knowledge level.",
                listOf("Beginner", "Informed", "Advanced (Bio-hacker)", "Medical Professional")
            ),
            MetadataQuestion(
                "supplement_load",
                "Supplement Load",
                "Chemical baseline.",
                listOf("0-1 daily", "2-5 daily", "5+ (Stacking)", "Targeted (Cycling on/off)")
            ),
            MetadataQuestion(
                "medication_type",
                "Medication Type",
                "Biological baseline.",
                listOf("None", "Daily Maintenance (BP/Thyroid)", "Periodic (Pain/Allergy)", "Hormone Replacement (TRT/HRT)")
            ),
            MetadataQuestion(
                "app_goal",
                "App Goal",
                "The North Star for the AI.",
                listOf("Longevity", "Weight Loss", "Muscle Gain", "Disease Management", "Mental Performance")
            ),
            MetadataQuestion(
                "tobacco_nicotine",
                "Tobacco/Nicotine",
                "Current usage.",
                listOf("Never", "Former Smoker", "Social", "Daily (Cigarettes)", "Daily (Vaping/Zyn)")
            ),
            MetadataQuestion(
                "alcohol_frequency",
                "Alcohol Frequency",
                "Weekly consumption.",
                listOf("Abstinent", "Rare (<1/month)", "Occasional (1-2/week)", "Regular (3+/week)")
            ),
            MetadataQuestion(
                "supplementation",
                "Supplementation",
                "Use of vitamins/minerals.",
                listOf("None", "Basic (Multivitamin)", "Performance (Protein/Creatine)", "Targeted (Health-specific)")
            ),
            MetadataQuestion(
                "tech_connectivity",
                "Tech Connectivity",
                "Primary health data source.",
                listOf("Manual Input Only", "Smartphone (Passive)", "Wearable (Watch/Ring)", "Multi-device Sync")
            )
        )
    ),
    MetadataCategory(
        title = "Development History (Legacy)",
        icon = Icons.Default.History,
        questions = listOf(
            MetadataQuestion(
                "birth_history",
                "Birth History",
                "Biological baseline.",
                listOf("Full-term", "Premature", "C-Section", "Natural")
            ),
            MetadataQuestion(
                "childhood_activity",
                "Childhood Activity",
                "Historical metabolic rate.",
                listOf("Highly Active (Sports)", "Moderate", "Sedentary/Bookworm")
            ),
            MetadataQuestion(
                "antibiotic_history",
                "Antibiotic History",
                "Microbiome status.",
                listOf("Rare/Never", "Standard", "Frequent (Chronic childhood infections)")
            )
        )
    )
)
