package com.fairtrack.app.data.activity

/**
 * Auswählbare Bewegungsquelle. Wird per [Enum.name] im DataStore persistiert
 * ([com.fairtrack.app.data.ActivityPreferencesRepository]). [sourceId] verbindet
 * den Enum-Wert mit der stabilen [ActivitySource.id].
 *
 * Google Fit dockt später an, indem hier ein weiterer Wert (z. B.
 * `GOOGLE_FIT("google_fit")`) ergänzt und eine passende [ActivitySource]
 * registriert wird.
 */
enum class ActivitySourceType(val sourceId: String) {
    HEALTH_CONNECT("health_connect")
}
