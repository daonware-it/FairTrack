package com.fairtrack.app.di

import android.content.Context
import com.fairtrack.app.data.activity.ActivitySource
import com.fairtrack.app.data.activity.HealthConnectActivitySource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Stellt die verfügbaren [ActivitySource]s als Hilt-Multibinding-Set bereit.
 * Ein späterer Google-Fit-Agent ergänzt hier lediglich ein weiteres
 * `@Provides @IntoSet` – die [com.fairtrack.app.data.activity.ActivitySourceRegistry]
 * sammelt automatisch alle ein.
 */
@Module
@InstallIn(SingletonComponent::class)
object ActivityModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideHealthConnectSource(
        @ApplicationContext context: Context
    ): ActivitySource = HealthConnectActivitySource(context)
}
