package com.example.healthlogops

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthlogops.ui.navigation.Routes
import com.example.healthlogops.ui.screens.AboutScreen
import com.example.healthlogops.ui.screens.AddLogScreen
import com.example.healthlogops.ui.screens.CategoriesScreen
import com.example.healthlogops.ui.screens.EditLogScreen
import com.example.healthlogops.ui.screens.HomeScreen
import com.example.healthlogops.ui.theme.HealthLogOpsTheme
import com.example.healthlogops.ui.viewmodel.MainViewModel
import com.example.healthlogops.ui.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val repository = (application as HealthLogApplication).repository
        
        setContent {
                    val viewModel: MainViewModel = viewModel(
                        factory = MainViewModelFactory(repository)
                    )
                    
                    val isDarkMode by viewModel.isDarkMode.collectAsState()
                    
                    HealthLogOpsTheme(darkTheme = isDarkMode) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                    
                    val navController = rememberNavController()
                    
                    NavHost(
                        navController = navController,
                        startDestination = Routes.HOME,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(400)
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToAddLog = {
                                    navController.navigate(Routes.ADD_LOG)
                                },
                                onNavigateToEditLog = { logId ->
                                    navController.navigate(Routes.editLog(logId))
                                },
                                onNavigateToAbout = {
                                    navController.navigate(Routes.ABOUT)
                                },
                                onNavigateToCategories = {
                                    navController.navigate(Routes.CATEGORIES)
                                }
                            )
                        }
                        
                        composable(Routes.ADD_LOG) {
                            AddLogScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(
                            route = Routes.EDIT_LOG,
                            arguments = listOf(
                                navArgument("logId") { type = NavType.IntType }
                            )
                        ) { backStackEntry ->
                            val logId = backStackEntry.arguments?.getInt("logId") ?: return@composable
                            EditLogScreen(
                                logId = logId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Routes.ABOUT) {
                            AboutScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Routes.CATEGORIES) {
                            CategoriesScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}