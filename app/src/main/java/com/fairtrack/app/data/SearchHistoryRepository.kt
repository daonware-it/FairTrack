package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore by preferencesDataStore(name = "search_prefs")

private val recentSearchesKey = stringPreferencesKey("recent_searches")

/** Wie viele Suchbegriffe maximal vorgehalten werden. */
private const val MAX_RECENT = 10

/**
 * Speichert die zuletzt verwendeten Suchbegriffe (neueste zuerst) in einem
 * eigenen DataStore. Persistiert als einzelner JSON-String.
 */
@Singleton
class SearchHistoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val listSerializer = ListSerializer(String.serializer())

    /** Die zuletzt gesuchten Begriffe, neueste zuerst (max. [MAX_RECENT]). */
    val recent: Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        prefs[recentSearchesKey]?.let { raw ->
            runCatching { Json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    /** Fügt einen Begriff hinzu (Move-to-front, dedupliziert, gekappt auf [MAX_RECENT]). */
    suspend fun add(term: String) {
        val trimmed = term.trim()
        if (trimmed.isBlank()) return
        context.searchDataStore.edit { prefs ->
            val current = prefs[recentSearchesKey]?.let { raw ->
                runCatching { Json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
            } ?: emptyList()
            val updated = (listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) })
                .take(MAX_RECENT)
            prefs[recentSearchesKey] = Json.encodeToString(listSerializer, updated)
        }
    }

    /** Löscht die gesamte Suchhistorie. */
    suspend fun clear() {
        context.searchDataStore.edit { prefs ->
            prefs.remove(recentSearchesKey)
        }
    }
}
