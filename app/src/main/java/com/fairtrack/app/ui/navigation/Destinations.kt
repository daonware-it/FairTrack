package com.fairtrack.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.fairtrack.app.R

/**
 * Die Top-Level-Ziele der Bottom Navigation.
 * Reihenfolge = Reihenfolge in der Navigationsleiste.
 */
enum class TopLevelDestination(
    val route: String,
    @get:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home, Icons.Outlined.Home),
    SEARCH("search", R.string.nav_search, Icons.Rounded.Search, Icons.Outlined.Search),
    DIARY(
        "diary",
        R.string.nav_diary,
        Icons.AutoMirrored.Rounded.MenuBook,
        Icons.AutoMirrored.Outlined.MenuBook
    ),
    STATISTICS("statistics", R.string.nav_statistics, Icons.Rounded.BarChart, Icons.Outlined.BarChart),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings)
}

/**
 * Routen, die kein Top-Level-Ziel der Bottom Navigation sind (z. B. modale/
 * verschachtelte Screens wie der Barcode-Scanner).
 */
object Routes {
    const val SCANNER = "scanner"
    const val PRODUCE = "produce"
    const val ONBOARDING = "onboarding"
    const val MY_FOODS = "my_foods"
    const val EDIT_FOOD = "edit_food"
    const val EDIT_DISH = "edit_dish"
    const val FASTING = "fasting"
    const val ABOUT = "about"
}

/** Baut die Scanner-Route. forResult=true => Produkt zurückgeben statt ins Tagebuch. */
fun scannerRoute(forResult: Boolean = false): String = "${Routes.SCANNER}?forResult=$forResult"

/** Baut die Route zum Lebensmittel-Editor (null = neu anlegen). */
fun editFoodRoute(id: Long?): String = "${Routes.EDIT_FOOD}?foodId=${id ?: -1L}"

/** Baut die Route zum Gericht-Editor (null = neu anlegen). */
fun editDishRoute(id: Long?): String = "${Routes.EDIT_DISH}?dishId=${id ?: -1L}"
