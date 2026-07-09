package com.fairtrack.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Stellt die Fasten-Ongoing-Notification nach einem Geräteneustart wieder her
 * (v0.13.0). Notifications überleben einen Reboot nicht, deshalb posten wir sie
 * neu, falls im DataStore noch ein Fasten aktiv ist.
 *
 * Die Fastenende-Erinnerung ([FastingReminderScheduler]) planen wir zusätzlich
 * neu: WorkManager stellt seine Jobs zwar über einen eigenen Reboot-Receiver
 * selbst wieder her, aber das erneute (eindeutige, REPLACE-)Einreihen ist
 * idempotent und deckt Grenzfälle ab.
 */
class FastingBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FastingEntryPoint::class.java
        )
        val preferences = entryPoint.fastingPreferencesRepository()
        val scheduler = entryPoint.fastingReminderScheduler()
        val notifier = entryPoint.fastingOngoingNotifier()

        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withContext(NonCancellable) {
                    val start = preferences.startMillis.first()
                    if (start <= 0L) return@withContext
                    val target = preferences.targetHours.first()
                    val protocol = preferences.protocol.first()

                    notifier.show(start, target, protocol)

                    if (preferences.reminderEnabled.first()) {
                        val remaining = start + target * 3_600_000L - System.currentTimeMillis()
                        scheduler.schedule(remaining)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }
}
