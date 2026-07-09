package com.fairtrack.app.data

import kotlin.math.roundToInt

/**
 * Berechnet die täglichen Ziel-Nährwerte aus dem Nutzerprofil.
 *
 * - BMR nach Mifflin-St Jeor.
 * - TDEE = BMR * Aktivitätsfaktor.
 * - Kalorien mit Ziel-Delta, untere Schranke 1200 kcal; ein manuelles
 *   Kalorienziel ersetzt den berechneten Wert und ist Basis aller Makros.
 * - Protein/Fett zielabhängig (g/kg), Protein zusätzlich mit Aktivitäts-Zuschlag.
 *   Basis ist das "angepasste" Gewicht: liegt ein Zielgewicht unter dem
 *   Ist-Gewicht, zählt Zielgewicht + 25 % der Differenz — sonst bekämen stark
 *   Übergewichtige unerreichbar hohe Protein-/Fett-Ziele. Das Zielgewicht wird
 *   dabei nie unter das minimal gesunde Gewicht (BMI 18,5) angesetzt.
 * - Kohlenhydrate = restliche Kalorien. Reicht das Kalorienbudget nicht für
 *   Protein + Fett, werden beide proportional gekürzt statt die KH negativ
 *   werden zu lassen.
 *
 * Manuelle Overrides (manualCalories/-Protein/-Carbs/-Fat) haben Vorrang,
 * werden nie skaliert und fließen in die Restrechnung der übrigen Makros ein.
 */
fun UserProfile.toNutritionGoals(): NutritionGoals {
    val bmr = 10 * weightKg + 6.25 * heightCm - 5 * age + (if (sex == Sex.MALE) 5 else -161)
    val tdee = bmr * activity.factor

    val autoCalories = (tdee + goal.kcalDelta).roundToInt().coerceAtLeast(1200)
    val calories = manualCalories ?: autoCalories

    val minHealthyKg = BodyMetrics.minHealthyWeightKg(heightCm)
    val target = targetWeightKg?.coerceAtLeast(minHealthyKg)
    val macroBasisKg = if (target != null && target < weightKg) {
        target + 0.25 * (weightKg - target)
    } else {
        weightKg
    }

    var proteinGrams = ((goal.proteinPerKg + activity.proteinBonusPerKg) * macroBasisKg).roundToInt()
    var fatGrams = (goal.fatPerKg * macroBasisKg).roundToInt()

    // Passen die berechneten Protein-/Fett-Kalorien nicht ins Budget (nach Abzug
    // manuell fixierter Makros), proportional kürzen — Overrides bleiben unberührt.
    val fixedKcal = (manualProteinGrams ?: 0) * 4 + (manualFatGrams ?: 0) * 9
    val autoKcal = (if (manualProteinGrams == null) proteinGrams * 4 else 0) +
        (if (manualFatGrams == null) fatGrams * 9 else 0)
    val budgetKcal = calories - fixedKcal
    if (autoKcal > budgetKcal && autoKcal > 0) {
        val scale = budgetKcal.coerceAtLeast(0).toDouble() / autoKcal
        if (manualProteinGrams == null) proteinGrams = (proteinGrams * scale).roundToInt()
        if (manualFatGrams == null) fatGrams = (fatGrams * scale).roundToInt()
    }

    val effectiveProtein = manualProteinGrams ?: proteinGrams
    val effectiveFat = manualFatGrams ?: fatGrams
    val restKcal = calories - effectiveProtein * 4 - effectiveFat * 9
    val carbsGrams = manualCarbsGrams ?: (restKcal / 4.0).roundToInt().coerceAtLeast(0)

    return NutritionGoals(
        calories = calories,
        proteinGrams = effectiveProtein,
        carbsGrams = carbsGrams,
        fatGrams = effectiveFat
    )
}
