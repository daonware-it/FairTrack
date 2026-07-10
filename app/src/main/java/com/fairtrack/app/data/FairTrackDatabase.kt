package com.fairtrack.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.fairtrack.app.data.entity.ActivityEntry
import com.fairtrack.app.data.entity.BodyMeasurement
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.FastingSession
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import com.fairtrack.app.data.entity.MealTemplate
import com.fairtrack.app.data.entity.MealTemplateItem
import com.fairtrack.app.data.entity.WaterEntry
import com.fairtrack.app.data.entity.WeightEntry

@Database(
    entities = [
        FoodItem::class,
        DiaryEntry::class,
        WeightEntry::class,
        FoodPortion::class,
        Dish::class,
        DishIngredient::class,
        MealTemplate::class,
        MealTemplateItem::class,
        WaterEntry::class,
        BodyMeasurement::class,
        FastingSession::class,
        ActivityEntry::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FairTrackDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun weightEntryDao(): WeightEntryDao
    abstract fun foodPortionDao(): FoodPortionDao
    abstract fun dishDao(): DishDao
    abstract fun mealTemplateDao(): MealTemplateDao
    abstract fun waterEntryDao(): WaterEntryDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun fastingSessionDao(): FastingSessionDao
    abstract fun activityEntryDao(): ActivityEntryDao
    abstract fun backupDao(): BackupDao

    companion object {
        const val NAME = "fairtrack.db"
    }
}
