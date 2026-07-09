package com.fairtrack.app.data

/**
 * Tägliche Ziel-Nährwerte.
 *
 * In v0.2.0 fest verdrahtet über [DEFAULT]. Ab v0.9.0 (Ziele) werden diese
 * Werte durch den Nutzer konfigurierbar und persistiert.
 */
data class NutritionGoals(
    val calories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
) {
    companion object {
        val DEFAULT = NutritionGoals(
            calories = 2000,
            proteinGrams = 100,
            carbsGrams = 250,
            fatGrams = 70
        )
    }
}
