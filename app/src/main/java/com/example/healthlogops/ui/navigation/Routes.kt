package com.example.healthlogops.ui.navigation

/**
 * Navigation routes for the app screens.
 */
object Routes {
    const val HOME = "home"
    const val ADD_LOG = "add_log"
    const val EDIT_LOG = "edit_log/{logId}"
    const val ABOUT = "about"
    const val CATEGORIES = "categories"
    
    fun editLog(logId: Int) = "edit_log/$logId"
}
