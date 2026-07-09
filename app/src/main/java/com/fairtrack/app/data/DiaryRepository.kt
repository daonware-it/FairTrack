package com.fairtrack.app.data

import com.fairtrack.app.data.dao.DiaryEntryDao
import com.fairtrack.app.data.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zentraler Zugriff auf Tagebuch-Einträge. Kapselt die DAOs, damit ViewModels
 * nicht direkt gegen Room arbeiten (saubere MVVM-Trennung).
 */
@Singleton
class DiaryRepository @Inject constructor(
    private val diaryEntryDao: DiaryEntryDao
) {

    /** Alle Einträge eines Tages als beobachtbarer Stream. */
    fun observeDay(epochDay: Long): Flow<List<DiaryEntry>> =
        diaryEntryDao.observeByDay(epochDay)

    /** Die zuletzt gegessenen Einträge (distinct nach Name), neueste zuerst. */
    fun observeRecent(limit: Int = 20): Flow<List<DiaryEntry>> =
        diaryEntryDao.observeRecent(limit)

    /**
     * Aggregierte Tages-Nährwerte ab einem Stichtag (epochDay), je Tag eine
     * Zeile. Für die Statistiken (v0.8.0). Tage ohne Einträge fehlen.
     */
    fun observeDailyNutritionSince(fromEpochDay: Long): Flow<List<DailyNutrition>> =
        diaryEntryDao.observeDailyNutritionSince(fromEpochDay)

    /**
     * Aggregierte Tages-Mikronährstoffe ab einem Stichtag (epochDay), je Tag eine
     * Zeile. Für die Mikronährstoff-Deckung in der Statistik (v0.10.0).
     */
    fun observeDailyMicronutrientsSince(fromEpochDay: Long): Flow<List<DailyMicronutrients>> =
        diaryEntryDao.observeDailyMicronutrientsSince(fromEpochDay)

    /**
     * Fügt einen Eintrag hinzu. Nährwerte werden aus den Angaben pro 100 g und
     * der Menge berechnet und fix gespeichert.
     */
    suspend fun addEntry(
        epochDay: Long,
        mealType: MealType,
        foodName: String,
        amountGrams: Double,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        unit: MeasureUnit = MeasureUnit.GRAMS,
        microsPer100g: Micronutrients = Micronutrients()
    ) {
        val factor = amountGrams / 100.0
        diaryEntryDao.insert(
            DiaryEntry(
                epochDay = epochDay,
                mealType = mealType,
                foodName = foodName,
                amountGrams = amountGrams,
                unit = unit,
                calories = caloriesPer100g * factor,
                protein = proteinPer100g * factor,
                carbs = carbsPer100g * factor,
                fat = fatPer100g * factor,
                micros = microsPer100g.scale(factor)
            )
        )
    }

    /**
     * Aktualisiert einen bestehenden Eintrag. Nährwerte werden – wie beim
     * Anlegen – aus den Angaben pro 100 g und der neuen Menge neu berechnet.
     * Tag ([DiaryEntry.epochDay]) und ID bleiben erhalten.
     */
    suspend fun updateEntry(
        original: DiaryEntry,
        mealType: MealType,
        foodName: String,
        amountGrams: Double,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        unit: MeasureUnit
    ) {
        val factor = amountGrams / 100.0
        diaryEntryDao.update(
            original.copy(
                mealType = mealType,
                foodName = foodName,
                amountGrams = amountGrams,
                unit = unit,
                calories = caloriesPer100g * factor,
                protein = proteinPer100g * factor,
                carbs = carbsPer100g * factor,
                fat = fatPer100g * factor
            )
        )
    }

    suspend fun deleteEntry(entry: DiaryEntry) = diaryEntryDao.delete(entry)
}
