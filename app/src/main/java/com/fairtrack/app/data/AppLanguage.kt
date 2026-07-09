package com.fairtrack.app.data

import androidx.core.os.LocaleListCompat

/**
 * Vom Nutzer gewählte App-Sprache (v0.11.0). [SYSTEM] folgt der Geräte-Sprache.
 * Die tatsächliche Locale-Umschaltung erfolgt über [toLocaleListCompat] zusammen
 * mit `AppCompatDelegate.setApplicationLocales(...)`.
 */
enum class AppLanguage {
    SYSTEM,
    DE,
    EN,
    ES
}

/**
 * Wandelt die gewählte Sprache in eine [LocaleListCompat] für
 * `AppCompatDelegate.setApplicationLocales(...)` um. [AppLanguage.SYSTEM] liefert
 * eine leere Liste, sodass wieder die Geräte-Sprache greift.
 */
fun AppLanguage.toLocaleListCompat(): LocaleListCompat = when (this) {
    AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
    AppLanguage.DE -> LocaleListCompat.forLanguageTags("de")
    AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
    AppLanguage.ES -> LocaleListCompat.forLanguageTags("es")
}
