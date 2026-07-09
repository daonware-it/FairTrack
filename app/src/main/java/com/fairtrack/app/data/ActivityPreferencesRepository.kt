package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fairtrack.app.data.activity.ActivitySourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.activityDataStore by preferencesDataStore(name = "activity_prefs")

private val selectedSourceKey = stringPreferencesKey("activity_source")
private val addToBudgetKey = booleanPreferencesKey("add_activity_calories_to_budget")
private val selectedSourceAppsKey = stringSetPreferencesKey("activity_source_apps")

/**
 * Persistiert die Bewegungs-Präferenzen (gewählte Quelle + Budget-Aufschlag) in
 * einem eigenen DataStore, im Stil von [WaterPreferencesRepository] /
 * [AppPreferencesRepository] (v0.14.0).
 *
 * Der Budget-Aufschlag ist standardmäßig AUS: Aktivkalorien werden nur dann auf
 * das Tagesbudget addiert, wenn der Nutzer das bewusst aktiviert (der TDEE
 * enthält über den Aktivitätsfaktor bereits Alltagsbewegung).
 */
@Singleton
class ActivityPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Gewählte Bewegungsquelle (Default [ActivitySourceType.HEALTH_CONNECT]). */
    val selectedSource: Flow<ActivitySourceType> = context.activityDataStore.data.map { prefs ->
        prefs[selectedSourceKey].toEnumOrDefault(ActivitySourceType.HEALTH_CONNECT)
    }

    /** Ob Aktivkalorien auf das Tagesbudget aufgeschlagen werden (Default false). */
    val addActivityCaloriesToBudget: Flow<Boolean> = context.activityDataStore.data.map { prefs ->
        prefs[addToBudgetKey] ?: false
    }

    /**
     * Paketnamen der einzahlenden Apps, die berücksichtigt werden. Semantik:
     * LEERES Set = ALLE Quellen (Default) – so sieht ein Nutzer ohne Auswahl nie
     * plötzlich 0 Schritte. Sonst zählen nur die angehakten Apps.
     */
    val selectedSourceApps: Flow<Set<String>> = context.activityDataStore.data.map { prefs ->
        prefs[selectedSourceAppsKey] ?: emptySet()
    }

    suspend fun setSelectedSource(source: ActivitySourceType) {
        context.activityDataStore.edit { prefs -> prefs[selectedSourceKey] = source.name }
    }

    suspend fun setSelectedSourceApps(apps: Set<String>) {
        context.activityDataStore.edit { prefs -> prefs[selectedSourceAppsKey] = apps }
    }

    suspend fun setAddActivityCaloriesToBudget(enabled: Boolean) {
        context.activityDataStore.edit { prefs -> prefs[addToBudgetKey] = enabled }
    }

    /** Löscht alle Bewegungs-Präferenzen (für "Daten löschen"). */
    suspend fun clear() {
        context.activityDataStore.edit { prefs -> prefs.clear() }
    }
}

/** Liest einen persistierten Enum-Namen defensiv; Default bei null/unbekannt. */
private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
    if (this == null) return default
    return try {
        enumValueOf<T>(this)
    } catch (e: IllegalArgumentException) {
        default
    }
}
