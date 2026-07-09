package com.fairtrack.app.data

/**
 * Maßeinheit einer Mengenangabe. Feste Lebensmittel werden in Gramm erfasst,
 * Getränke in Millilitern. Die Nährwerte von Open Food Facts sind pro 100 g
 * angegeben; für Getränke wird eine Dichte von ~1 g/ml angenommen, sodass die
 * Rechnung pro 100 ml praktisch identisch bleibt.
 */
enum class MeasureUnit(val symbol: String) {
    GRAMS("g"),
    MILLILITERS("ml")
}
