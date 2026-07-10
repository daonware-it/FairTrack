package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Ein eigenes Gericht (Rezept), das aus mehreren Zutaten besteht.
 */
@Entity(tableName = "dishes")
@Serializable
data class Dish(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val servings: Double = 1.0   // wie viele Portionen das ganze Gericht ergibt
)
