package com.fairtrack.app.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Plant bzw. verwirft die Fastenende-Erinnerung über WorkManager (v0.13.0).
 *
 * Anders als die periodische Trink-Erinnerung ist dies ein PUNKTGENAUER
 * Trigger: ein [OneTimeWorkRequest] mit [setInitialDelay] auf die verbleibende
 * Fastenzeit. Liegt das Ziel in der Vergangenheit (oder max. wenige Sekunden
 * entfernt), wird nichts geplant.
 */
@Singleton
class FastingReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /** Plant die Erinnerung auf [remainingMillis] ab jetzt. */
    fun schedule(remainingMillis: Long) {
        if (remainingMillis <= 0L) return
        FastingReminderWorker.ensureChannel(context)
        val request = OneTimeWorkRequestBuilder<FastingReminderWorker>()
            .setInitialDelay(remainingMillis, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Verwirft eine geplante Erinnerung (Fasten beendet/abgebrochen). */
    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "fasting_reminder_work"
    }
}
