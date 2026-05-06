package com.animalitostv.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.animalitostv.ui.config.ConfigScreen
import com.animalitostv.ui.history.HistoryScreen
import com.animalitostv.ui.main.MainScreen

object Routes {
    const val MAIN = "main"
    const val CONFIG = "config"
    const val HISTORY = "history"
}

@Composable
fun AnimalitosNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToConfig = { navController.navigate(Routes.CONFIG) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) }
            )
        }
        composable(Routes.CONFIG) {
            ConfigScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
