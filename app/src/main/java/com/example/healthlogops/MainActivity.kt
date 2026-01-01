package com.example.healthlogops

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.healthlogops.util.ErrorManager
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.os.Looper
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

        val application = (application as HealthLogApplication)
        val repository = application.repository
        val prefsRepository = application.userPreferencesRepository
        val userRepository = application.userRepository
        val sessionManager = application.sessionManager

        setupGlobalErrorHandler()
        setupErrorListener()
        checkAndRequestPermissions()

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository, prefsRepository, sessionManager)
            )
            val authViewModel: com.example.healthlogops.ui.viewmodel.AuthViewModel = viewModel(
                factory = com.example.healthlogops.ui.viewmodel.AuthViewModelFactory(userRepository, sessionManager)
            )

            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val isPreferencesLoaded by viewModel.isPreferencesLoaded.collectAsState()
            val currentUserId by authViewModel.currentUserId.collectAsState()

            HealthLogOpsTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (!isPreferencesLoaded) {
                        // Splash/Loading screen while preferences load to avoid flicker
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        MainNavigation(viewModel, authViewModel, currentUserId)
                    }
                }
            }
        }
    }

    private fun setupGlobalErrorHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // Emit to ErrorManager so it shows a toast
            ErrorManager.emitError("Fatal Error: ${throwable.localizedMessage ?: throwable.toString()}")

            // Log it
            throwable.printStackTrace()

            // We still want to let it crash if it's truly unrecoverable, 
            // but we delay a bit so the toast can be posted (Handler might not work if app is dying)
            // Actually, for uncaught exceptions, the app is going down. 
            // Most reliable is to show a toast and then call the default handler.
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun setupErrorListener() {
        lifecycleScope.launch {
            ErrorManager.errors.collect { message ->
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val deniedPermissions = permissions.indices.filter {
                grantResults[it] != PackageManager.PERMISSION_GRANTED
            }
            if (deniedPermissions.isNotEmpty()) {
                Toast.makeText(this, "Some permissions were denied. Certain features may not work.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}

@Composable
fun MainNavigation(
    viewModel: MainViewModel, 
    authViewModel: com.example.healthlogops.ui.viewmodel.AuthViewModel,
    currentUserId: String?
) {
    val navController = rememberNavController()
    
    // Auth-based start destination
    val startDestination = if (currentUserId == null) Routes.LOGIN else Routes.HOME

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
        composable(Routes.LOGIN) {
            com.example.healthlogops.ui.screens.LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

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
                },
                onNavigateToProfile = {
                    navController.navigate(Routes.PROFILE)
                }
            )
        }
        
        composable(Routes.PROFILE) {
            com.example.healthlogops.ui.screens.ProfileScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    if (currentUserId == null) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0)
                        }
                    } else {
                        navController.popBackStack()
                    }
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