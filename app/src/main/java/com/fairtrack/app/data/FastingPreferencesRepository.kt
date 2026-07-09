package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.fastingDataStore by preferencesDataStore(name = "fasting_prefs")

private val protocolKey = stringPreferencesKey("fasting_protocol")
private val startMillisKey = longPreferencesKey("fasting_start_millis")
private val targetHoursKey = intPreferencesKey("fasting_target_hours")
private val reminderEnabledKey = booleanPreferencesKey("fasting_reminder_enabled")

/**
 * Persistiert den laufenden Fasten-Zustand (gewähltes Preset, Startzeit,
 * Zielstunden, Erinnerung) im Stil von [WaterPreferencesRepository] (v0.13.0).
 *
 * Ein laufendes Fasten braucht keine Room-Zeile: Start-Millis + Zielstunden
 * überstehen im DataStore auch einen Prozess-Neustart; die verstrichene Zeit
 * wird in der UI aus `System.currentTimeMillis() - start` berechnet. Erst der
 * Abschluss schreibt eine Verlaufs-Zeile über [FastingRepository].
 *
 * [startMillisKey] == 0 bedeutet "kein Fasten läuft". Das Preset wird als
 * [Enum.name] gespeichert und defensiv über
 * [FastingProtocol.fromNameOrDefault] gelesen.
 */
@Singleton
class FastingPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Gewähltes Preset (Default [FastingProtocol.DEFAULT]). */
    val protocol: Flow<FastingProtocol> = context.fastingDataStore.data.map { prefs ->
        FastingProtocol.fromNameOrDefault(prefs[protocolKey])
    }

    /** Start-Zeitpunkt des laufenden Fastens in Millis (0 = kein Fasten aktiv). */
    val startMillis: Flow<Long> = context.fastingDataStore.data.map { prefs ->
        prefs[startMillisKey] ?: 0L
    }

    /** Angepeiltes Fastenfenster in Stunden (des laufenden Fastens). */
    val targetHours: Flow<Int> = context.fastingDataStore.data.map { prefs ->
        prefs[targetHoursKey] ?: FastingProtocol.DEFAULT.fastingHours
    }

    /** Ob eine Erinnerung zum Fastenende gewünscht ist. */
    val reminderEnabled: Flow<Boolean> = context.fastingDataStore.data.map { prefs ->
        prefs[reminderEnabledKey] ?: false
    }

    suspend fun setProtocol(protocol: FastingProtocol) {
        context.fastingDataStore.edit { prefs -> prefs[protocolKey] = protocol.name }
    }

    /** Startet ein Fasten: Startzeit + Zielstunden festhalten. */
    suspend fun startFast(startMillis: Long, targetHours: Int) {
        context.fastingDataStore.edit { prefs ->
            prefs[startMillisKey] = startMillis
            prefs[targetHoursKey] = targetHours
        }
    }

    /** Beendet das laufende Fasten (Startzeit auf 0). */
    suspend fun stopFast() {
        context.fastingDataStore.edit { prefs -> prefs[startMillisKey] = 0L }
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.fastingDataStore.edit { prefs -> prefs[reminderEnabledKey] = enabled }
    }

    /** Löscht alle Fasten-Präferenzen (für "Daten löschen"). */
    suspend fun clear() {
        context.fastingDataStore.edit { prefs -> prefs.clear() }
    }
}
