package com.fairtrack.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Ziel eines neuen Eintrags: Tag (epochDay) und Mahlzeit. */
data class AddEntryTarget(
    val epochDay: Long,
    val mealType: MealType
)

/**
 * Merkt sich das Ziel (Datum + Mahlzeit) für einen neuen Eintrag über
 * Screen-Grenzen hinweg. Wird gesetzt, wenn der Nutzer in der Tagesübersicht
 * auf das "+" einer Mahlzeit tippt, und von Suche/Scanner gelesen, damit der
 * Eintrag am richtigen Tag (auch in der Vergangenheit) und in der richtigen
 * Mahlzeit landet.
 */
@Singleton
class AddEntryContext @Inject constructor() {

    private val _target = MutableStateFlow(
        AddEntryTarget(LocalDate.now().toEpochDay(), defaultMealForNow())
    )
    val target: StateFlow<AddEntryTarget> = _target.asStateFlow()

    /** Merkt Tag und Mahlzeit (z. B. beim "+"-Tippen in der Tagesübersicht). */
    fun set(epochDay: Long, mealType: MealType) {
        _target.value = AddEntryTarget(epochDay, mealType)
    }

    /** Ändert nur die Mahlzeit, behält den gemerkten Tag. */
    fun setMeal(mealType: MealType) {
        _target.update { it.copy(mealType = mealType) }
    }

    companion object {
        /** Standard-Mahlzeit nach Tageszeit (bis 10 Frühstück, 15 Mittag, 21 Abend). */
        fun defaultMealForNow(): MealType {
            val hour = LocalTime.now().hour
            return when {
                hour < 10 -> MealType.BREAKFAST
                hour < 15 -> MealType.LUNCH
                hour < 21 -> MealType.DINNER
                else -> MealType.SNACK
            }
        }
    }
}
