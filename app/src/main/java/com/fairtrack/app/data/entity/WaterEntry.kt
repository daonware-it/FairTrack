package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Getrunkene Wassermenge eines Tages (v0.7.0). epochDay ist Primärschlüssel,
 * damit pro Tag genau ein aggregierter Datensatz existiert; neue Schlucke werden
 * auf den bestehenden Tageswert addiert (Muster analog zu [WeightEntry]).
 */
@Entity(tableName = "water_entries")
@Serializable
data class WaterEntry(
    @PrimaryKey val epochDay: Long,
    val amountMl: Int
)
