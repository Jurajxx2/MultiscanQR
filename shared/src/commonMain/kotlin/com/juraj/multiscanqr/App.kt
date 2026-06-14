package com.juraj.multiscanqr

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.juraj.multiscanqr.history.HistoryScreen
import com.juraj.multiscanqr.history.HistoryViewModel
import com.juraj.multiscanqr.scanner.ScannerScreen
import com.juraj.multiscanqr.scanner.ScannerViewModel
import org.koin.compose.viewmodel.koinViewModel

private object Routes {
    const val Scanner = "scanner"
    const val History = "history"
}

@Composable
fun App() {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
    ) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = Routes.Scanner,
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
        ) {
            composable(Routes.Scanner) {
                val scannerViewModel: ScannerViewModel = koinViewModel()
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onOpenHistory = {
                        navController.navigate(Routes.History) {
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Routes.History) {
                val historyViewModel: HistoryViewModel = koinViewModel()
                HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
