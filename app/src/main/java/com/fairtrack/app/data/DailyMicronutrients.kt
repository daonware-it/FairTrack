package com.fairtrack.app.data

/**
 * Aggregierte Tages-Mikronährstoffe, projiziert aus einer SUM/GROUP-BY-Query über
 * die `diary_entries` (analog [DailyNutrition]). Jede Spalte ist die Tagessumme
 * des jeweiligen Mikronährstoffs in seiner Referenzeinheit (mg bzw. µg). SUM über
 * lauter NULL-Werte ergibt NULL -> ein Nährstoff ohne jede Angabe bleibt null.
 */
data class DailyMicronutrients(
    val epochDay: Long,
    val vitaminA: Double? = null,
    val vitaminD: Double? = null,
    val vitaminE: Double? = null,
    val vitaminC: Double? = null,
    val vitaminB6: Double? = null,
    val vitaminB12: Double? = null,
    val folate: Double? = null,
    val calcium: Double? = null,
    val iron: Double? = null,
    val magnesium: Double? = null,
    val zinc: Double? = null,
    val potassium: Double? = null,
    val phosphorus: Double? = null,
    val iodine: Double? = null
) {
    fun toMicronutrients(): Micronutrients = Micronutrients(
        vitaminA = vitaminA,
        vitaminD = vitaminD,
        vitaminE = vitaminE,
        vitaminC = vitaminC,
        vitaminB6 = vitaminB6,
        vitaminB12 = vitaminB12,
        folate = folate,
        calcium = calcium,
        iron = iron,
        magnesium = magnesium,
        zinc = zinc,
        potassium = potassium,
        phosphorus = phosphorus,
        iodine = iodine
    )
}
