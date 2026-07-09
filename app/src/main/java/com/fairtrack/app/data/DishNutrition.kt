package com.fairtrack.app.data

import com.fairtrack.app.data.entity.DishIngredient

/** Aggregierte Nährwerte eines Gerichts (oder einer Portion davon). */
data class DishTotals(
    val grams: Double,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

/** Summiert die Nährwerte aller Zutaten (jede Zutat pro 100 g × Menge). */
fun List<DishIngredient>.totals(): DishTotals {
    var g = 0.0; var k = 0.0; var p = 0.0; var c = 0.0; var f = 0.0
    for (i in this) {
        val factor = i.amountGrams / 100.0
        g += i.amountGrams
        k += i.caloriesPer100g * factor
        p += i.proteinPer100g * factor
        c += i.carbsPer100g * factor
        f += i.fatPer100g * factor
    }
    return DishTotals(g, k, p, c, f)
}

/** Nährwerte pro Portion (Gesamtmenge geteilt durch Anzahl Portionen). */
fun DishTotals.perServing(servings: Double): DishTotals {
    val s = if (servings > 0) servings else 1.0
    return DishTotals(grams / s, kcal / s, protein / s, carbs / s, fat / s)
}

/** kcal/100g-Dichte des Gerichts (für DiaryRepository.addEntry). */
fun DishTotals.densityPer100g(): DishTotals {
    val base = if (grams > 0) 100.0 / grams else 0.0
    return DishTotals(100.0, kcal * base, protein * base, carbs * base, fat * base)
}
