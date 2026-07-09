package com.fairtrack.app.data

import android.content.Context
import androidx.work.WorkManager
import com.fairtrack.app.work.FastingOngoingNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Setzt sämtliche Nutzerdaten zurück (v0.11.0):
 * - alle Room-Tabellen (Tagebuch, Lebensmittel, Gerichte, Vorlagen, Wasser, Gewicht),
 * - die DataStores für Profil, Wasser-Präferenzen und Suchhistorie,
 * - geplante Hintergrund-Jobs (Trink-Erinnerung).
 *
 * Die reinen UI-Präferenzen ("app_prefs": Theme, Maßsystem, Sprache) bleiben
 * bewusst erhalten und werden NICHT gelöscht.
 */
@Singleton
class DataResetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FairTrackDatabase,
    private val userProfileRepository: UserProfileRepository,
    private val waterPreferencesRepository: WaterPreferencesRepository,
    private val fastingPreferencesRepository: FastingPreferencesRepository,
    private val fastingOngoingNotifier: FastingOngoingNotifier,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val activityPreferencesRepository: ActivityPreferencesRepository
) {

    suspend fun resetAll() {
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        userProfileRepository.clear()
        waterPreferencesRepository.clear()
        fastingPreferencesRepository.clear()
        fastingOngoingNotifier.hide()
        searchHistoryRepository.clear()
        activityPreferencesRepository.clear()
        WorkManager.getInstance(context).cancelAllWork()
    }
}
