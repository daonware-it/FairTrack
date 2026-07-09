package com.fairtrack.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantische Zusatzfarben außerhalb des Material-Farbschemas: Makronährstoffe,
 * Mahlzeiten-Akzente und der Ring-Track. Werden auch von späteren Screens
 * wiederverwendet (Statistik-Donut v0.8.0, Wasser-Tracker v0.7.0).
 */
@Immutable
data class FairTrackColors(
    val protein: Color,
    val proteinContainer: Color,
    val carbs: Color,
    val carbsContainer: Color,
    val fat: Color,
    val fatContainer: Color,
    val accentBreakfast: Color,
    val accentLunch: Color,
    val accentDinner: Color,
    val accentSnack: Color,
    val ringTrack: Color
)

val LightFairTrackColors = FairTrackColors(
    protein = Color(0xFFC2453A),
    proteinContainer = Color(0xFFFFDAD5),
    carbs = Color(0xFFA16B00),
    carbsContainer = Color(0xFFFFE0A6),
    fat = Color(0xFF3A6EA5),
    fatContainer = Color(0xFFD3E4FF),
    accentBreakfast = Color(0xFFA16B00),
    accentLunch = Color(0xFF3B6939),
    accentDinner = Color(0xFF5B5891),
    accentSnack = Color(0xFF8B4A62),
    ringTrack = Color(0xFFEBEDEB)
)

val DarkFairTrackColors = FairTrackColors(
    protein = Color(0xFFFFB4A9),
    proteinContainer = Color(0xFF5F150E),
    carbs = Color(0xFFF0BF48),
    carbsContainer = Color(0xFF553F00),
    fat = Color(0xFFA7C8FF),
    fatContainer = Color(0xFF1E4876),
    accentBreakfast = Color(0xFFF0BF48),
    accentLunch = Color(0xFFA1D39A),
    accentDinner = Color(0xFFC5C0FF),
    accentSnack = Color(0xFFFFB0CC),
    ringTrack = Color(0xFF363636)
)

val LocalFairTrackColors = staticCompositionLocalOf { LightFairTrackColors }

/** Zugriff analog zu `MaterialTheme.colorScheme`. */
val MaterialTheme.fairTrackColors: FairTrackColors
    @Composable
    @ReadOnlyComposable
    get() = LocalFairTrackColors.current
