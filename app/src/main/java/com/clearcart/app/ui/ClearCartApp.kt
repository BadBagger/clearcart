package com.clearcart.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.screens.CompareScreen
import com.clearcart.app.ui.screens.HistoryScreen
import com.clearcart.app.ui.screens.HomeScreen
import com.clearcart.app.ui.screens.ManualEntryScreen
import com.clearcart.app.ui.screens.OcrLabelScanScreen
import com.clearcart.app.ui.screens.PreferencesScreen
import com.clearcart.app.ui.screens.PrivacyScreen
import com.clearcart.app.ui.screens.ProductResultScreen
import com.clearcart.app.ui.screens.ScannerScreen
import com.clearcart.app.ui.screens.SearchScreen
import com.clearcart.app.ui.screens.SettingsScreen

@Composable
fun ClearCartApp(container: AppContainer) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(container, navController) }
        composable("scanner") { ScannerScreen(container, navController) }
        composable("search") { SearchScreen(container, navController) }
        composable(
            "product/{barcode}",
            arguments = listOf(navArgument("barcode") { type = NavType.StringType }),
        ) { ProductResultScreen(container, navController, it.arguments?.getString("barcode").orEmpty()) }
        composable("history") { HistoryScreen(container, navController) }
        composable("preferences") { PreferencesScreen(container, navController) }
        composable("compare") { CompareScreen(container, navController) }
        composable(
            "manual?barcode={barcode}&name={name}&ingredients={ingredients}",
            arguments = listOf(
                navArgument("barcode") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("ingredients") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) {
            ManualEntryScreen(
                container = container,
                navController = navController,
                initialBarcode = it.arguments?.getString("barcode").orEmpty(),
                initialName = it.arguments?.getString("name").orEmpty(),
                initialIngredients = it.arguments?.getString("ingredients").orEmpty(),
            )
        }
        composable("ocr") { OcrLabelScanScreen(container, navController) }
        composable("privacy") { PrivacyScreen(container, navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
