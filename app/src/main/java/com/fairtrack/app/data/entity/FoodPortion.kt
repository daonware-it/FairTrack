package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Eine benannte Portion für ein Lebensmittel (z.B. "Scheibe" = 25 g).
 * Wird gelöscht, wenn das zugehörige [FoodItem] gelöscht wird (CASCADE).
 */
@Entity(
    tableName = "food_portions",
    foreignKeys = [ForeignKey(entity = FoodItem::class, parentColumns = ["id"], childColumns = ["foodItemId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("foodItemId")]
)
@Serializable
data class FoodPortion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodItemId: Long,
    val label: String,      // z.B. "Scheibe", "Portion", "Glas"
    val grams: Double
)
