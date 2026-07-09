package com.fairtrack.app.work

import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.ActivityRepository
import com.fairtrack.app.data.activity.ActivitySourceRegistry
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-Zugang für den [ActivitySyncWorker]. Der Worker wird von WorkManager
 * (nicht von Hilt) instanziiert, deshalb lösen wir die Singletons über diesen
 * EntryPoint auf – Muster wie [com.fairtrack.app.ui.widget.WidgetEntryPoint].
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ActivityEntryPoint {
    fun activityRepository(): ActivityRepository
    fun activityPreferencesRepository(): ActivityPreferencesRepository
    fun activitySourceRegistry(): ActivitySourceRegistry
}
