package com.fairtrack.app.data

import java.util.Locale

/**
 * Umrechnungs- und Anzeigeschicht für imperiale Einheiten (v0.11.0).
 *
 * Room speichert grundsätzlich metrisch (Gramm, Milliliter, Kilogramm). Diese
 * Schicht rechnet ausschließlich für Anzeige und Eingabe in das aktive
 * [UnitSystem] um; die Persistenz bleibt unverändert metrisch.
 */
object UnitFormatter {

    /** 1 oz (avoirdupois) in Gramm. */
    const val GRAMS_PER_OUNCE = 28.3495

    /** 1 US fluid ounce in Milliliter. */
    const val ML_PER_FLUID_OUNCE = 29.5735

    /** 1 kg in Pfund (lb). */
    const val POUNDS_PER_KG = 2.20462

    /** 1 Zoll (inch) in Zentimeter. */
    const val CM_PER_INCH = 2.54

    /**
     * Rechnet einen metrisch gespeicherten Mengenwert in den anzuzeigenden Wert
     * des aktiven Systems um (g -> oz bzw. ml -> fl oz; metrisch unverändert).
     */
    fun toDisplayAmount(valueMetric: Double, unit: MeasureUnit, system: UnitSystem): Double =
        when (system) {
            UnitSystem.METRIC -> valueMetric
            UnitSystem.IMPERIAL -> when (unit) {
                MeasureUnit.GRAMS -> valueMetric / GRAMS_PER_OUNCE
                MeasureUnit.MILLILITERS -> valueMetric / ML_PER_FLUID_OUNCE
            }
        }

    /**
     * Rechnet einen im aktiven System eingegebenen Mengenwert zurück in die
     * metrische Speichergröße (oz -> g bzw. fl oz -> ml).
     */
    fun toMetricAmount(displayValue: Double, unit: MeasureUnit, system: UnitSystem): Double =
        when (system) {
            UnitSystem.METRIC -> displayValue
            UnitSystem.IMPERIAL -> when (unit) {
                MeasureUnit.GRAMS -> displayValue * GRAMS_PER_OUNCE
                MeasureUnit.MILLILITERS -> displayValue * ML_PER_FLUID_OUNCE
            }
        }

    /** Einheitssymbol für die Menge im aktiven System ("g"/"ml" bzw. "oz"/"fl oz"). */
    fun amountSymbol(unit: MeasureUnit, system: UnitSystem): String =
        when (system) {
            UnitSystem.METRIC -> unit.symbol
            UnitSystem.IMPERIAL -> when (unit) {
                MeasureUnit.GRAMS -> "oz"
                MeasureUnit.MILLILITERS -> "fl oz"
            }
        }

    /**
     * Formatierte Mengenangabe inkl. Symbol, z. B. "100 g" bzw. "3,5 oz".
     * [valueMetric] ist der gespeicherte metrische Wert.
     */
    fun formatAmount(valueMetric: Double, unit: MeasureUnit, system: UnitSystem): String {
        val display = toDisplayAmount(valueMetric, unit, system)
        val number = when (system) {
            // Metrisch ganzzahlig (wie bisher), imperial mit einer Nachkommastelle.
            UnitSystem.METRIC -> trimDecimals(display, 0)
            UnitSystem.IMPERIAL -> trimDecimals(display, 1)
        }
        return "$number ${amountSymbol(unit, system)}"
    }

    /**
     * Nur der (lokalisierte) Zahlenteil der Menge ohne Symbol – für Eingabefelder,
     * die den metrischen Speicherwert [valueMetric] als Startwert anzeigen.
     */
    fun amountNumber(valueMetric: Double, unit: MeasureUnit, system: UnitSystem): String {
        val display = toDisplayAmount(valueMetric, unit, system)
        return when (system) {
            UnitSystem.METRIC -> trimDecimals(display, 0)
            UnitSystem.IMPERIAL -> trimDecimals(display, 1)
        }
    }

    /** Symbol für Körpergewicht im aktiven System ("kg" bzw. "lb"). */
    fun weightSymbol(system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "lb" else "kg"

    /** Metrisch gespeichertes Gewicht in den Anzeigewert des Systems (kg -> lb). */
    fun toDisplayWeight(kg: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) kg * POUNDS_PER_KG else kg

    /** Anzeigewert des Systems zurück in Kilogramm (lb -> kg). */
    fun toMetricWeight(displayValue: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) displayValue / POUNDS_PER_KG else displayValue

    /** Formatierte Gewichtsangabe inkl. Symbol, z. B. "70 kg" bzw. "154,3 lb". */
    fun weightLabel(kg: Double, system: UnitSystem): String {
        val display = toDisplayWeight(kg, system)
        return "${trimDecimals(display, 1)} ${weightSymbol(system)}"
    }

    /** Nur der (lokalisierte) Zahlenteil des Gewichts ohne Symbol. */
    fun weightNumber(kg: Double, system: UnitSystem): String =
        trimDecimals(toDisplayWeight(kg, system), 1)

    /** Symbol für Körpermaße (Länge) im aktiven System ("cm" bzw. "in"). */
    fun lengthSymbol(system: UnitSystem): String =
        if (system == UnitSystem.IMPERIAL) "in" else "cm"

    /** Metrisch gespeichertes Längenmaß in den Anzeigewert des Systems (cm -> in). */
    fun toDisplayLength(cm: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) cm / CM_PER_INCH else cm

    /** Anzeigewert des Systems zurück in Zentimeter (in -> cm). */
    fun toMetricLength(displayValue: Double, system: UnitSystem): Double =
        if (system == UnitSystem.IMPERIAL) displayValue * CM_PER_INCH else displayValue

    /** Formatierte Längenangabe inkl. Symbol, z. B. "80 cm" bzw. "31,5 in". */
    fun lengthLabel(cm: Double, system: UnitSystem): String {
        val display = toDisplayLength(cm, system)
        return "${trimDecimals(display, 1)} ${lengthSymbol(system)}"
    }

    /** Nur der (lokalisierte) Zahlenteil des Längenmaßes ohne Symbol. */
    fun lengthNumber(cm: Double, system: UnitSystem): String =
        trimDecimals(toDisplayLength(cm, system), 1)

    /**
     * Formatiert eine Zahl mit bis zu [maxDecimals] Nachkommastellen und schneidet
     * überflüssige Nullen ab ("100", "3,5"). Nutzt das Geräte-[Locale].
     */
    private fun trimDecimals(value: Double, maxDecimals: Int): String {
        if (value % 1.0 == 0.0) return value.toLong().toString()
        val rounded = String.format(Locale.getDefault(), "%.${maxDecimals}f", value)
        // Nachlaufende Nullen/Trenner entfernen (locale-unabhängig für '.' und ',').
        return rounded.trimEnd('0').trimEnd('.', ',')
    }
}
