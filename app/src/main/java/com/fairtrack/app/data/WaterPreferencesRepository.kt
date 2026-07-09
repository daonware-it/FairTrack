package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.waterDataStore by preferencesDataStore(name = "water_prefs")

private val goalMlKey = intPreferencesKey("water_goal_ml")
private val reminderEnabledKey = booleanPreferencesKey("water_reminder_enabled")
private val reminderIntervalHoursKey = intPreferencesKey("water_reminder_interval_hours")

/** Standard-Tagesziel in ml. */
const val DEFAULT_WATER_GOAL_ML = 2000

/** Standard-Intervall der Trink-Erinnerung in Stunden. */
const val DEFAULT_REMINDER_INTERVAL_HOURS = 2

/**
 * Persistiert die Wasser-Präferenzen (Tagesziel + Erinnerungs-Einstellungen)
 * in einem eigenen DataStore, im Stil von [SearchHistoryRepository] (v0.7.0).
 */
@Singleton
class WaterPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Tägliches Wasserziel in ml (Default [DEFAULT_WATER_GOAL_ML]). */
    val goalMl: Flow<Int> = context.waterDataStore.data.map { prefs ->
        prefs[goalMlKey] ?: DEFAULT_WATER_GOAL_ML
    }

    /** Ob die periodische Trink-Erinnerung aktiv ist. */
    val reminderEnabled: Flow<Boolean> = context.waterDataStore.data.map { prefs ->
        prefs[reminderEnabledKey] ?: false
    }

    /** Erinnerungs-Intervall in Stunden (Default [DEFAULT_REMINDER_INTERVAL_HOURS]). */
    val reminderIntervalHours: Flow<Int> = context.waterDataStore.data.map { prefs ->
        prefs[reminderIntervalHoursKey] ?: DEFAULT_REMINDER_INTERVAL_HOURS
    }

    suspend fun setGoalMl(goalMl: Int) {
        context.waterDataStore.edit { prefs ->
            prefs[goalMlKey] = goalMl.coerceAtLeast(1)
        }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.waterDataStore.edit { prefs ->
            prefs[reminderEnabledKey] = enabled
        }
    }

    suspend fun setReminderIntervalHours(hours: Int) {
        context.waterDataStore.edit { prefs ->
            prefs[reminderIntervalHoursKey] = hours.coerceAtLeast(1)
        }
    }

    /** Löscht alle Wasser-Präferenzen (für "Daten löschen", v0.11.0). */
    suspend fun clear() {
        context.waterDataStore.edit { prefs -> prefs.clear() }
    }
}
