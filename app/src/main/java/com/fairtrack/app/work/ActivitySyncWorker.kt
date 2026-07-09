package com.fairtrack.app.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Importiert Schritte + Aktivkalorien der gewählten Quelle in die lokale DB
 * (v0.14.0). Bewusst als schlichter [CoroutineWorker] ohne Hilt-Injection, damit
 * die WorkManager-Auto-Initialisierung greift; die Repositories kommen über
 * [ActivityEntryPoint].
 *
 * Es wird ein Rückwärtsfenster von [SYNC_WINDOW_DAYS] Tagen synchronisiert, weil
 * Health Connect Daten nachträglich ergänzt. Der Upsert ist idempotent
 * (Unique-Index auf (date, source)), wiederholte Läufe erzeugen keine Duplikate.
 */
class ActivitySyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            ActivityEntryPoint::class.java
        )
        val preferences = entryPoint.activityPreferencesRepository()
        val registry = entryPoint.activitySourceRegistry()
        val repository = entryPoint.activityRepository()

        return try {
            val type = preferences.selectedSource.first()
            val source = registry.forType(type) ?: return Result.success()
            if (!source.isAvailable() || !source.hasPermissions()) return Result.success()

            // Leeres Set = alle einzahlenden Apps (Default).
            val sourceApps = preferences.selectedSourceApps.first()
            val end = LocalDate.now()
            val start = end.minusDays((SYNC_WINDOW_DAYS - 1).toLong())
            source.readDailyActivity(start..end, sourceApps).forEach { daily ->
                repository.upsert(daily, type)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val SYNC_WINDOW_DAYS = 7
    }
}
