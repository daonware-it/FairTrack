package com.fairtrack.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plant den periodischen Bewegungs-Sync und stößt bei Bedarf einen sofortigen
 * Lauf an (z. B. beim Öffnen des Home-Screens). Muster wie
 * [WaterReminderScheduler]; WorkManager erzwingt eine Mindestperiode von
 * 15 Minuten – hier bewusst 3 Stunden, da Bewegungsdaten nicht minutengenau
 * gebraucht werden.
 */
@Singleton
class ActivitySyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Aktiviert den periodischen Sync (idempotent; behält vorhandene Planung). */
    fun schedulePeriodic() {
        val request = PeriodicWorkRequestBuilder<ActivitySyncWorker>(
            PERIOD_HOURS, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Stößt einen einmaligen Sofort-Sync an (z. B. beim Öffnen des Home-Screens). */
    fun syncNow() {
        val request = OneTimeWorkRequestBuilder<ActivitySyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private companion object {
        const val PERIODIC_WORK_NAME = "activity_sync_periodic"
        const val ONE_TIME_WORK_NAME = "activity_sync_now"
        const val PERIOD_HOURS = 3L
    }
}
