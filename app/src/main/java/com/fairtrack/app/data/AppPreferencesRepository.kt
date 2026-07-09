package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore by preferencesDataStore(name = "app_prefs")

private val themeModeKey = stringPreferencesKey("theme_mode")
private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
private val unitSystemKey = stringPreferencesKey("unit_system")
private val appLanguageKey = stringPreferencesKey("app_language")

/**
 * Persistiert die App-Präferenzen (Theme, Material You, Maßsystem, Sprache) in
 * einem eigenen DataStore, im Stil von [WaterPreferencesRepository] (v0.11.0).
 *
 * Enums werden als [Enum.name] gespeichert und defensiv gelesen; unbekannte oder
 * beschädigte Werte fallen auf den jeweiligen Default zurück. Dieser DataStore
 * wird bewusst NICHT von [DataResetRepository] gelöscht.
 */
@Singleton
class AppPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Gewählter Theme-Modus (Default [ThemeMode.AUTO]). */
    val themeMode: Flow<ThemeMode> = context.appDataStore.data.map { prefs ->
        prefs[themeModeKey].toEnumOrDefault(ThemeMode.AUTO)
    }

    /** Ob dynamische Material-You-Farben aktiv sind (Default false). */
    val dynamicColor: Flow<Boolean> = context.appDataStore.data.map { prefs ->
        prefs[dynamicColorKey] ?: false
    }

    /** Gewähltes Maßsystem (Default [UnitSystem.METRIC]). */
    val unitSystem: Flow<UnitSystem> = context.appDataStore.data.map { prefs ->
        prefs[unitSystemKey].toEnumOrDefault(UnitSystem.METRIC)
    }

    /** Gewählte App-Sprache (Default [AppLanguage.SYSTEM]). */
    val appLanguage: Flow<AppLanguage> = context.appDataStore.data.map { prefs ->
        prefs[appLanguageKey].toEnumOrDefault(AppLanguage.SYSTEM)
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.appDataStore.edit { prefs -> prefs[themeModeKey] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.appDataStore.edit { prefs -> prefs[dynamicColorKey] = enabled }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.appDataStore.edit { prefs -> prefs[unitSystemKey] = system.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        context.appDataStore.edit { prefs -> prefs[appLanguageKey] = language.name }
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
