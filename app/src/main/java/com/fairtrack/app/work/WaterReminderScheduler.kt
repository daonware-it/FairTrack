package com.fairtrack.app.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plant bzw. verwirft die periodische Trink-Erinnerung über WorkManager (v0.7.0).
 * WorkManager erzwingt eine Mindestperiode von 15 Minuten – kürzere Intervalle
 * werden entsprechend angehoben.
 */
@Singleton
class WaterReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Aktiviert (mit Intervall in Stunden) oder deaktiviert die Erinnerung. */
    fun update(enabled: Boolean, intervalHours: Int) {
        if (enabled) schedule(intervalHours) else cancel()
    }

    private fun schedule(intervalHours: Int) {
        WaterReminderWorker.ensureChannel(context)
        val minutes = (intervalHours * 60L).coerceAtLeast(15L)
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(
            minutes, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "water_reminder_work"
    }
}
