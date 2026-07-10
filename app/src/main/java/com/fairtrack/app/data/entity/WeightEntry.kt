package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Ein Gewichtseintrag für einen Tag. epochDay ist Primärschlüssel, damit pro Tag
 * genau ein Gewicht existiert (erneutes Eintragen ersetzt den Wert).
 */
@Entity(tableName = "weight_entries")
@Serializable
data class WeightEntry(
    @PrimaryKey val epochDay: Long,
    val weightKg: Double
)
