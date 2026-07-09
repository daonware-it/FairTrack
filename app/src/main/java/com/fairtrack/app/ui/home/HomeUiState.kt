package com.fairtrack.app.ui.home

import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.NutritionGoals
import com.fairtrack.app.data.entity.DiaryEntry
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Ein Makronährstoff für die Übersicht: konsumierte Gramm, der prozentuale
 * Fortschritt zum Tagesziel (kann 100 % übersteigen) und das Tagesziel in Gramm.
 */
data class MacroValue(
    val grams: Int,
    val percent: Int,
    val goalGrams: Int
) {
    /** Fortschritt Richtung Tagesziel, auf 0..1 begrenzt. */
    val goalFraction: Float
        get() = if (goalGrams > 0) (grams.toFloat() / goalGrams).coerceIn(0f, 1f) else 0f
}

/**
 * Zustand der Wasser-Karte (v0.7.0): getrunkene Menge und Tagesziel in ml.
 */
data class WaterUiState(
    val consumedMl: Int = 0,
    val goalMl: Int = com.fairtrack.app.data.DEFAULT_WATER_GOAL_ML
) {
    /** Fortschritt Richtung Tagesziel, auf 0..1 begrenzt. */
    val fraction: Float
        get() = if (goalMl > 0) (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f
}

/** Ein-/Aus-Zustand und Intervall der Trink-Erinnerung (v0.7.0). */
data class WaterReminderState(
    val enabled: Boolean = false,
    val intervalHours: Int = com.fairtrack.app.data.DEFAULT_REMINDER_INTERVAL_HOURS
)

/** Zustand einer Mahlzeiten-Gruppe (Frühstück, Mittag, …). */
data class MealSectionState(
    val mealType: MealType,
    val entries: List<DiaryEntry>,
    val calories: Int
)

/** Kompletter Zustand des Home-/Tagesübersicht-Screens. */
data class HomeUiState(
    val date: LocalDate,
    val isToday: Boolean,
    val consumedCalories: Int,
    val goalCalories: Int,
    val protein: MacroValue,
    val carbs: MacroValue,
    val fat: MacroValue,
    val meals: List<MealSectionState>
) {
    /** Verbleibende Kalorien bis zum Ziel (kann negativ werden). */
    val remainingCalories: Int get() = goalCalories - consumedCalories

    companion object {
        fun from(
            date: LocalDate,
            today: LocalDate,
            entries: List<DiaryEntry>,
            goals: NutritionGoals
        ): HomeUiState {
            val totalProtein = entries.sumOf { it.protein }
            val totalCarbs = entries.sumOf { it.carbs }
            val totalFat = entries.sumOf { it.fat }
            val totalCalories = entries.sumOf { it.calories }

            // Prozent = Fortschritt zum Tagesziel (kann über 100 % gehen).
            fun percentOfGoal(consumed: Double, goalGrams: Int): Int =
                if (goalGrams > 0) (consumed / goalGrams * 100).roundToInt() else 0

            // Immer alle Mahlzeiten-Gruppen in fester Reihenfolge anzeigen.
            val meals = MealType.entries.map { type ->
                val forMeal = entries.filter { it.mealType == type }
                MealSectionState(
                    mealType = type,
                    entries = forMeal,
                    calories = forMeal.sumOf { it.calories }.roundToInt()
                )
            }

            return HomeUiState(
                date = date,
                isToday = date == today,
                consumedCalories = totalCalories.roundToInt(),
                goalCalories = goals.calories,
                protein = MacroValue(totalProtein.roundToInt(), percentOfGoal(totalProtein, goals.proteinGrams), goals.proteinGrams),
                carbs = MacroValue(totalCarbs.roundToInt(), percentOfGoal(totalCarbs, goals.carbsGrams), goals.carbsGrams),
                fat = MacroValue(totalFat.roundToInt(), percentOfGoal(totalFat, goals.fatGrams), goals.fatGrams),
                meals = meals
            )
        }
    }
}
