package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Eine gespeicherte Mahlzeiten-Vorlage (z. B. "Mein Frühstück"), die aus
 * mehreren Einträgen besteht und mit einem Tap komplett ins Tagebuch
 * übernommen werden kann (v0.6.0).
 */
@Entity(tableName = "meal_templates")
@Serializable
data class MealTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)
