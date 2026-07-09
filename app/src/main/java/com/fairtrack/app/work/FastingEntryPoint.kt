package com.fairtrack.app.work

import com.fairtrack.app.data.FastingPreferencesRepository
import com.fairtrack.app.data.FastingRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-Zugang für die Fasten-BroadcastReceiver (v0.13.0). Receiver werden nicht
 * von Hilt instanziiert, deshalb lösen wir die Singletons über diesen EntryPoint
 * via [dagger.hilt.android.EntryPointAccessors.fromApplication] auf (Muster wie
 * [com.fairtrack.app.ui.widget.WidgetEntryPoint]).
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FastingEntryPoint {
    fun fastingPreferencesRepository(): FastingPreferencesRepository
    fun fastingRepository(): FastingRepository
    fun fastingReminderScheduler(): FastingReminderScheduler
    fun fastingOngoingNotifier(): FastingOngoingNotifier
}
