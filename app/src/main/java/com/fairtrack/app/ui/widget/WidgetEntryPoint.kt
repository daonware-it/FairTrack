package com.fairtrack.app.ui.widget

import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.ActivityRepository
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.UserProfileRepository
import com.fairtrack.app.data.WaterPreferencesRepository
import com.fairtrack.app.data.WaterRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt-Zugang für die Glance-Widgets (v0.10.0). Glance-Widgets und
 * [androidx.glance.appwidget.action.ActionCallback]s werden nicht von Hilt
 * instanziiert, weshalb wir die Singleton-Repositories über diesen EntryPoint
 * via [dagger.hilt.android.EntryPointAccessors.fromApplication] auflösen.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun diaryRepository(): DiaryRepository
    fun userProfileRepository(): UserProfileRepository
    fun waterRepository(): WaterRepository
    fun waterPreferencesRepository(): WaterPreferencesRepository
    fun activityRepository(): ActivityRepository
    fun activityPreferencesRepository(): ActivityPreferencesRepository
}
