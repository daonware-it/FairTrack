package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fairtrack.app.data.MeasureUnit

/**
 * Ein einzelnes Element einer Mahlzeiten-Vorlage. Nährwerte werden – wie bei
 * [DishIngredient] – als Snapshot pro 100 g gespeichert, damit die Vorlage
 * unabhängig vom Katalog bleibt. Wird gelöscht, wenn die zugehörige
 * [MealTemplate] gelöscht wird (CASCADE).
 */
@Entity(
    tableName = "meal_template_items",
    foreignKeys = [
        ForeignKey(
            entity = MealTemplate::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class MealTemplateItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val templateId: Long,
    val name: String,
    /** Erfasste Menge im Wert der [unit] (Gramm bei Speisen, Milliliter bei Getränken). */
    val amountGrams: Double,
    val unit: MeasureUnit = MeasureUnit.GRAMS,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val isBeverage: Boolean = false
)
