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
 * Beendet das laufende Fasten über die Action der Ongoing-Notification
 * (v0.13.0). Nicht exported – nur der App-eigene PendingIntent löst ihn aus.
 * Spiegelt [com.fairtrack.app.ui.fasting.FastingViewModel.stopFast].
 */
class FastingStopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            FastingEntryPoint::class.java
        )
        val preferences = entryPoint.fastingPreferencesRepository()
        val repository = entryPoint.fastingRepository()
        val scheduler = entryPoint.fastingReminderScheduler()
        val notifier = entryPoint.fastingOngoingNotifier()

        // Der Prozess kann kalt gestartet sein; goAsync + NonCancellable stellen
        // sicher, dass Room-/DataStore-Schreiben vollständig durchlaufen (analog
        // zum WaterAddAction-Glance-Bugfix).
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                withContext(NonCancellable) {
                    val start = preferences.startMillis.first()
                    if (start > 0L) {
                        val target = preferences.targetHours.first()
                        val protocol = preferences.protocol.first()
                        scheduler.cancel()
                        repository.record(
                            startEpochMillis = start,
                            endEpochMillis = System.currentTimeMillis(),
                            targetHours = target,
                            presetName = protocol.name
                        )
                        preferences.stopFast()
                    } else {
                        // Sicherheitshalber trotzdem aufräumen.
                        scheduler.cancel()
                    }
                    notifier.hide()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
