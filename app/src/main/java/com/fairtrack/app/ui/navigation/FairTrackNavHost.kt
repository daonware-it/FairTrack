package com.fairtrack.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fairtrack.app.ui.fasting.FastingScreen
import com.fairtrack.app.ui.home.HomeScreen
import com.fairtrack.app.ui.myfoods.EditDishScreen
import com.fairtrack.app.ui.myfoods.EditFoodScreen
import com.fairtrack.app.ui.myfoods.MyFoodsScreen
import com.fairtrack.app.ui.onboarding.OnboardingScreen
import com.fairtrack.app.ui.produce.ProduceScreen
import com.fairtrack.app.ui.scanner.ScannerScreen
import com.fairtrack.app.ui.about.AboutScreen
import com.fairtrack.app.ui.diary.DiaryScreen
import com.fairtrack.app.ui.search.SearchScreen
import com.fairtrack.app.ui.settings.SettingsScreen
import com.fairtrack.app.ui.statistics.StatisticsScreen

/**
 * Zentraler NavHost. Home zeigt ab v0.2.0 die Tagesübersicht, die übrigen Ziele
 * folgen in späteren Meilensteinen und nutzen bis dahin den Platzhalter.
 */
@Composable
fun FairTrackNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.HOME.route,
        modifier = modifier
    ) {
        TopLevelDestination.entries.forEach { destination ->
            composable(destination.route) {
                when (destination) {
                    TopLevelDestination.HOME -> HomeScreen(
                        onAddFood = {
                            // Wie ein Bottom-Nav-Wechsel zum Such-Tab (Zustand erhalten).
                            navController.navigate(TopLevelDestination.SEARCH.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onOpenFasting = { navController.navigate(Routes.FASTING) }
                    )
                    TopLevelDestination.SEARCH -> SearchScreen(
                        onScanClick = { navController.navigate(Routes.SCANNER) },
                        onProduceClick = { navController.navigate(Routes.PRODUCE) },
                        onMyFoodsClick = { navController.navigate(Routes.MY_FOODS) }
                    )
                    TopLevelDestination.SETTINGS -> SettingsScreen(
                        onEditGoals = { navController.navigate(Routes.ONBOARDING) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) }
                    )
                    TopLevelDestination.STATISTICS -> StatisticsScreen()
                    TopLevelDestination.DIARY -> DiaryScreen(
                        // Das Datum reicht der SelectedDayCoordinator durch; hier
                        // wird nur wie bei einem Bottom-Nav-Wechsel zum Home-Tab
                        // gesprungen, damit dessen Zustand erhalten bleibt.
                        onOpenDay = {
                            navController.navigate(TopLevelDestination.HOME.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }

        // Kein Bottom-Nav-Ziel: modaler Scanner-Screen, per popBackStack schließbar.
        // forResult=true => gescanntes Produkt wird zurückgegeben (z. B. als Zutat),
        // statt ins Tagebuch geschrieben zu werden.
        composable(
            route = "${Routes.SCANNER}?forResult={forResult}",
            arguments = listOf(navArgument("forResult") { type = NavType.BoolType; defaultValue = false })
        ) {
            ScannerScreen(onClose = { navController.popBackStack() })
        }

        // Kein Bottom-Nav-Ziel: Obst/Gemüse-Auswahl, per popBackStack schließbar.
        composable(Routes.PRODUCE) {
            ProduceScreen(onClose = { navController.popBackStack() })
        }

        // Kein Bottom-Nav-Ziel: Fasten-Timer, per popBackStack schließbar.
        composable(Routes.FASTING) {
            FastingScreen(onClose = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(onClose = { navController.popBackStack() })
        }

        // Onboarding erneut aus den Einstellungen: bearbeitet Ziele/Profil.
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onDone = { navController.popBackStack() })
        }

        // Meine Lebensmittel: eigene Lebensmittel/Gerichte verwalten und eintragen.
        composable(Routes.MY_FOODS) {
            MyFoodsScreen(
                onClose = { navController.popBackStack() },
                onCreateFood = { navController.navigate(editFoodRoute(null)) },
                onEditFood = { id -> navController.navigate(editFoodRoute(id)) },
                onCreateDish = { navController.navigate(editDishRoute(null)) },
                onEditDish = { id -> navController.navigate(editDishRoute(id)) }
            )
        }

        // Editor für ein eigenes Lebensmittel (foodId == -1 => neu anlegen).
        composable(
            route = "${Routes.EDIT_FOOD}?foodId={foodId}",
            arguments = listOf(navArgument("foodId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            EditFoodScreen(onClose = { navController.popBackStack() })
        }

        // Editor für ein eigenes Gericht (dishId == -1 => neu anlegen).
        // EditDishScreen wird von einem anderen Agenten erstellt.
        composable(
            route = "${Routes.EDIT_DISH}?dishId={dishId}",
            arguments = listOf(navArgument("dishId") { type = NavType.LongType; defaultValue = -1L })
        ) {
            EditDishScreen(
                onClose = { navController.popBackStack() },
                onScanIngredient = { navController.navigate(scannerRoute(forResult = true)) }
            )
        }
    }
}
