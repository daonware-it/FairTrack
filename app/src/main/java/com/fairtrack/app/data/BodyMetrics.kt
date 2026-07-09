package com.fairtrack.app.data

/**
 * Körpermaß-Berechnungen rund um den BMI.
 *
 * Liefert neben dem BMI selbst einen altersabhängigen wünschenswerten
 * Gewichtsbereich und eine harte Untergrenze (BMI 18,5), unter die kein
 * Zielgewicht gesetzt werden darf — Schutz vor gesundheitsschädlichen Zielen.
 */
object BodyMetrics {

    /** WHO-Untergrenze für Normalgewicht; darunter beginnt Untergewicht. */
    const val MIN_HEALTHY_BMI = 18.5

    /** BMI = Gewicht (kg) / Größe (m)². */
    fun bmi(weightKg: Double, heightCm: Double): Double {
        val heightM = heightCm / 100.0
        return weightKg / (heightM * heightM)
    }

    /**
     * Wünschenswerter BMI-Bereich nach Alter (NRC-Tabelle): mit zunehmendem
     * Alter verschiebt sich der günstige Bereich leicht nach oben.
     */
    fun desirableBmiRange(age: Int): ClosedFloatingPointRange<Double> = when {
        age < 25 -> 19.0..24.0
        age < 35 -> 20.0..25.0
        age < 45 -> 21.0..26.0
        age < 55 -> 22.0..27.0
        age < 65 -> 23.0..28.0
        else -> 24.0..29.0
    }

    /** Minimal gesundes Gewicht (BMI 18,5) — harte Untergrenze fürs Zielgewicht. */
    fun minHealthyWeightKg(heightCm: Double): Double {
        val heightM = heightCm / 100.0
        return MIN_HEALTHY_BMI * heightM * heightM
    }

    /** Gewicht zum unteren/oberen Rand des altersabhängigen BMI-Bereichs. */
    fun desirableWeightRangeKg(heightCm: Double, age: Int): ClosedFloatingPointRange<Double> {
        val heightM = heightCm / 100.0
        val range = desirableBmiRange(age)
        return (range.start * heightM * heightM)..(range.endInclusive * heightM * heightM)
    }

    /** Empfohlenes "optimales" Gewicht: Mitte des altersabhängigen BMI-Bereichs. */
    fun optimalWeightKg(heightCm: Double, age: Int): Double {
        val range = desirableBmiRange(age)
        val midBmi = (range.start + range.endInclusive) / 2
        val heightM = heightCm / 100.0
        return midBmi * heightM * heightM
    }
}
