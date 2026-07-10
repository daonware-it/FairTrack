package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Eine Zutat eines Gerichts. Nährwerte werden pro 100 g gespeichert, damit
 * spätere Änderungen an einem FoodItem das Rezept nicht verfälschen.
 * Wird gelöscht, wenn das zugehörige [Dish] gelöscht wird (CASCADE).
 */
@Entity(
    tableName = "dish_ingredients",
    foreignKeys = [ForeignKey(entity = Dish::class, parentColumns = ["id"], childColumns = ["dishId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dishId")]
)
@Serializable
data class DishIngredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dishId: Long,
    val name: String,
    val amountGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
)
