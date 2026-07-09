package com.fairtrack.app.data.activity

import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.ActivityRepository
import com.fairtrack.app.data.NutritionGoals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/**
 * Schlägt Aktivkalorien auf ein Kalorien-Tagesziel auf. Der TDEE bleibt in
 * [com.fairtrack.app.data.GoalsCalculator] rein profilbasiert – der bewegungs-
 * abhängige Aufschlag passiert erst hier in der Konsum-Schicht.
 */
fun NutritionGoals.withActivityBonus(activeKcal: Int): NutritionGoals =
    if (activeKcal <= 0) this else copy(calories = calories + activeKcal)

/**
 * Bewegungs-Aufschlag (kcal) für einen Tag, abhängig vom Budget-Flag und der
 * gewählten Quelle. Liefert 0, solange der Aufschlag deaktiviert ist (Default).
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun ActivityRepository.observeBudgetBonus(
    preferences: ActivityPreferencesRepository,
    epochDay: Long
): Flow<Int> =
    combine(
        preferences.addActivityCaloriesToBudget,
        preferences.selectedSource
    ) { enabled, source -> enabled to source }
        .flatMapLatest { (enabled, source) ->
            if (!enabled) flowOf(0) else observeActiveKcal(epochDay, source)
        }

/**
 * Kalorienziel inkl. Bewegungs-Aufschlag als Flow. Zentraler, überall
 * wiederverwendeter Einstieg (Home, Statistik, Widget), damit alle Screens
 * dasselbe Budget zeigen.
 */
fun activityAdjustedGoals(
    goals: Flow<NutritionGoals>,
    activityRepository: ActivityRepository,
    activityPreferences: ActivityPreferencesRepository,
    epochDay: Long
): Flow<NutritionGoals> =
    combine(
        goals,
        activityRepository.observeBudgetBonus(activityPreferences, epochDay)
    ) { g, bonus -> g.withActivityBonus(bonus) }
