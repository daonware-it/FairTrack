package com.fairtrack.app.data

enum class Sex { MALE, FEMALE }

/**
 * Aktivitätslevel für die TDEE-Berechnung.
 *
 * - [factor]: PAL-Multiplikator (BMR × factor = TDEE).
 * - [proteinBonusPerKg]: kleiner zusätzlicher Protein-Bedarf (g/kg) für Aktivere;
 *   die Kohlenhydrate steigen ohnehin über die höheren TDEE-Kalorien.
 */
enum class ActivityLevel(val factor: Double, val proteinBonusPerKg: Double) {
    SEDENTARY(1.2, 0.0),
    LIGHT(1.375, 0.05),
    MODERATE(1.55, 0.10),
    ACTIVE(1.725, 0.15),
    VERY_ACTIVE(1.9, 0.20)
}

/**
 * Zielrichtung mit den zugehörigen Makro-Vorgaben.
 *
 * - [kcalDelta]: tägliche Kalorien-Anpassung gegenüber dem Erhaltungsbedarf.
 * - [proteinPerKg]: höher im Defizit (Muskelerhalt), moderat bei Erhalt/Aufbau.
 * - [fatPerKg]: mind. ~0,8 g/kg für den Hormonhaushalt.
 *
 * Kohlenhydrate ergeben sich aus den restlichen Kalorien und skalieren damit
 * automatisch mit Ziel und Aktivitätslevel.
 */
enum class WeightGoal(
    val kcalDelta: Int,
    val proteinPerKg: Double,
    val fatPerKg: Double
) {
    LOSE(-500, 2.2, 0.8),
    MAINTAIN(0, 1.8, 1.0),
    GAIN(300, 2.0, 1.0)
}

data class UserProfile(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val sex: Sex,
    val activity: ActivityLevel,
    val goal: WeightGoal,
    val targetWeightKg: Double? = null,
    val manualCalories: Int? = null,
    val manualProteinGrams: Int? = null,
    val manualCarbsGrams: Int? = null,
    val manualFatGrams: Int? = null
)
