package com.fairtrack.app.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.Micronutrients

/**
 * Ein Tagebuch-Eintrag: ein Lebensmittel, das an einem Tag zu einer Mahlzeit
 * in einer bestimmten Menge gegessen wurde.
 *
 * Die Nährwerte werden zum Zeitpunkt des Eintrags berechnet gespeichert, damit
 * spätere Änderungen am FoodItem alte Einträge nicht verfälschen.
 */
@Entity(tableName = "diary_entries")
data class DiaryEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Tag als epochDay (LocalDate.toEpochDay()). */
    val epochDay: Long,
    val mealType: MealType,
    val foodName: String,
    /** Erfasste Menge im Wert der [unit] (Gramm bei Speisen, Milliliter bei Getränken). */
    val amountGrams: Double,
    /** Einheit der Mengenangabe. Bestehende Einträge sind in Gramm. */
    val unit: MeasureUnit = MeasureUnit.GRAMS,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    /**
     * Mikronährstoff-Snapshot für die erfasste Menge (bereits mit amount/100
     * skaliert, in mg/µg). Nullable Spalten -> Einträge ohne Mikronährstoff-
     * Angaben bleiben null und fließen nicht in die Tagesaggregation ein.
     */
    @Embedded val micros: Micronutrients = Micronutrients()
)
