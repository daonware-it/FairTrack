package com.fairtrack.app.data

/**
 * Vom Nutzer gewähltes Maßsystem (v0.11.0). Room speichert weiterhin metrisch;
 * [IMPERIAL] betrifft ausschließlich Anzeige und Eingabe (siehe [UnitFormatter]).
 */
enum class UnitSystem {
    METRIC,
    IMPERIAL
}
