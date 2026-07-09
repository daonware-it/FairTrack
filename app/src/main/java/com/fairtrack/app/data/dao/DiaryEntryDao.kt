package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fairtrack.app.data.DailyMicronutrients
import com.fairtrack.app.data.DailyNutrition
import com.fairtrack.app.data.entity.DiaryEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Query("SELECT * FROM diary_entries WHERE epochDay = :epochDay ORDER BY id ASC")
    fun observeByDay(epochDay: Long): Flow<List<DiaryEntry>>

    /**
     * Die zuletzt gegessenen Einträge, je Produktname nur der jüngste, neueste
     * zuerst (für "Kürzlich gegessen" im Such-Tab).
     */
    @Query(
        "SELECT * FROM diary_entries WHERE id IN " +
            "(SELECT MAX(id) FROM diary_entries GROUP BY foodName) " +
            "ORDER BY id DESC LIMIT :limit"
    )
    fun observeRecent(limit: Int): Flow<List<DiaryEntry>>

    /**
     * Aggregierte Tages-Nährwerte (Kalorien + Makros) ab einem Stichtag,
     * je Tag eine Zeile. Basis für Wochen-/Monats-Statistik, Makro-Donut und
     * Streak (v0.8.0). Tage ohne Einträge fehlen und werden im ViewModel
     * mit 0 aufgefüllt.
     */
    @Query(
        "SELECT epochDay AS epochDay, SUM(calories) AS calories, " +
            "SUM(protein) AS protein, SUM(carbs) AS carbs, SUM(fat) AS fat " +
            "FROM diary_entries WHERE epochDay >= :from " +
            "GROUP BY epochDay ORDER BY epochDay ASC"
    )
    fun observeDailyNutritionSince(from: Long): Flow<List<DailyNutrition>>

    /**
     * Aggregierte Tages-Mikronährstoffe ab einem Stichtag, je Tag eine Zeile.
     * Basis für die Mikronährstoff-Deckung in der Statistik (v0.10.0). SUM über
     * lauter NULL ergibt NULL -> Nährstoffe ganz ohne Angabe bleiben null.
     */
    @Query(
        "SELECT epochDay AS epochDay, " +
            "SUM(vitaminA) AS vitaminA, SUM(vitaminD) AS vitaminD, " +
            "SUM(vitaminE) AS vitaminE, SUM(vitaminC) AS vitaminC, " +
            "SUM(vitaminB6) AS vitaminB6, SUM(vitaminB12) AS vitaminB12, " +
            "SUM(folate) AS folate, SUM(calcium) AS calcium, " +
            "SUM(iron) AS iron, SUM(magnesium) AS magnesium, " +
            "SUM(zinc) AS zinc, SUM(potassium) AS potassium, " +
            "SUM(phosphorus) AS phosphorus, SUM(iodine) AS iodine " +
            "FROM diary_entries WHERE epochDay >= :from " +
            "GROUP BY epochDay ORDER BY epochDay ASC"
    )
    fun observeDailyMicronutrientsSince(from: Long): Flow<List<DailyMicronutrients>>

    @Insert
    suspend fun insert(entry: DiaryEntry): Long

    @Update
    suspend fun update(entry: DiaryEntry)

    @Delete
    suspend fun delete(entry: DiaryEntry)
}
