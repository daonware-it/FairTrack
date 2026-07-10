package com.fairtrack.app.data

import kotlinx.serialization.Serializable

/**
 * Mahlzeiten-Gruppen für die Tagesübersicht (v0.2.0).
 */
@Serializable
enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}
