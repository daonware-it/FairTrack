package com.fairtrack.app.ui.home

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BakeryDining
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.DinnerDining
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.ui.theme.fairTrackColors

/** String-Ressource für den Anzeigenamen einer Mahlzeiten-Gruppe. */
@get:StringRes
val MealType.labelRes: Int
    get() = when (this) {
        MealType.BREAKFAST -> R.string.meal_breakfast
        MealType.LUNCH -> R.string.meal_lunch
        MealType.DINNER -> R.string.meal_dinner
        MealType.SNACK -> R.string.meal_snack
    }

/** Icon einer Mahlzeiten-Gruppe. */
val MealType.icon: ImageVector
    get() = when (this) {
        MealType.BREAKFAST -> Icons.Rounded.BakeryDining
        MealType.LUNCH -> Icons.Rounded.LunchDining
        MealType.DINNER -> Icons.Rounded.DinnerDining
        MealType.SNACK -> Icons.Rounded.Cookie
    }

/** Akzentfarbe einer Mahlzeiten-Gruppe aus den FairTrack-Zusatzfarben. */
val MealType.accentColor: Color
    @Composable
    @ReadOnlyComposable
    get() = when (this) {
        MealType.BREAKFAST -> MaterialTheme.fairTrackColors.accentBreakfast
        MealType.LUNCH -> MaterialTheme.fairTrackColors.accentLunch
        MealType.DINNER -> MaterialTheme.fairTrackColors.accentDinner
        MealType.SNACK -> MaterialTheme.fairTrackColors.accentSnack
    }
