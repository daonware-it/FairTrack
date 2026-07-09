package com.fairtrack.app.data

/**
 * Aggregierte Tages-Nährwerte, projiziert aus einer SUM/GROUP-BY-Query über die
 * `diary_entries` (v0.8.0 Statistiken). Ein Eintrag existiert nur für Tage mit
 * mindestens einem Tagebuch-Eintrag.
 */
data class DailyNutrition(
    val epochDay: Long,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)
