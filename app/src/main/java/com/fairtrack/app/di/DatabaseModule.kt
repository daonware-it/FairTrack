package com.fairtrack.app.di

import android.content.Context
import androidx.room.Room
import com.fairtrack.app.data.ALL_MIGRATIONS
import com.fairtrack.app.data.FairTrackDatabase
import com.fairtrack.app.data.dao.ActivityEntryDao
import com.fairtrack.app.data.dao.BackupDao
import com.fairtrack.app.data.dao.BodyMeasurementDao
import com.fairtrack.app.data.dao.DiaryEntryDao
import com.fairtrack.app.data.dao.DishDao
import com.fairtrack.app.data.dao.FastingSessionDao
import com.fairtrack.app.data.dao.FoodItemDao
import com.fairtrack.app.data.dao.FoodPortionDao
import com.fairtrack.app.data.dao.MealTemplateDao
import com.fairtrack.app.data.dao.WaterEntryDao
import com.fairtrack.app.data.dao.WeightEntryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt-Modul, das die Room-Datenbank und die DAOs bereitstellt.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FairTrackDatabase =
        Room.databaseBuilder(
            context,
            FairTrackDatabase::class.java,
            FairTrackDatabase.NAME
        )
            // Echte Migrationen erhalten die Nutzerdaten bei Schema-Änderungen.
            // Nur ein Downgrade (niedrigere Version) löscht noch destruktiv.
            .addMigrations(*ALL_MIGRATIONS)
            .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
            .build()

    @Provides
    fun provideFoodItemDao(database: FairTrackDatabase): FoodItemDao =
        database.foodItemDao()

    @Provides
    fun provideDiaryEntryDao(database: FairTrackDatabase): DiaryEntryDao =
        database.diaryEntryDao()

    @Provides
    fun provideWeightEntryDao(database: FairTrackDatabase): WeightEntryDao =
        database.weightEntryDao()

    @Provides
    fun provideFoodPortionDao(database: FairTrackDatabase): FoodPortionDao =
        database.foodPortionDao()

    @Provides
    fun provideDishDao(database: FairTrackDatabase): DishDao =
        database.dishDao()

    @Provides
    fun provideMealTemplateDao(database: FairTrackDatabase): MealTemplateDao =
        database.mealTemplateDao()

    @Provides
    fun provideWaterEntryDao(database: FairTrackDatabase): WaterEntryDao =
        database.waterEntryDao()

    @Provides
    fun provideBodyMeasurementDao(database: FairTrackDatabase): BodyMeasurementDao =
        database.bodyMeasurementDao()

    @Provides
    fun provideFastingSessionDao(database: FairTrackDatabase): FastingSessionDao =
        database.fastingSessionDao()

    @Provides
    fun provideActivityEntryDao(database: FairTrackDatabase): ActivityEntryDao =
        database.activityEntryDao()

    @Provides
    fun provideBackupDao(database: FairTrackDatabase): BackupDao =
        database.backupDao()
}
