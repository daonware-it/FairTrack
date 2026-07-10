package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Ein importiertes Bewegungs-Tagesaggregat je Quelle. [date] ist der epochDay
 * (LocalDate.toEpochDay). Der eindeutige Index auf (date, source) macht den
 * Sync idempotent: ein erneuter Lauf ersetzt denselben Tag/Quelle-Datensatz,
 * statt Duplikate zu erzeugen. So können mehrere Quellen (Health Connect, später
 * Google Fit) parallel denselben Tag halten.
 */
@Entity(
    tableName = "activity_entries",
    indices = [Index(value = ["date", "source"], unique = true)]
)
@Serializable
data class ActivityEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val steps: Int,
    val activeKcal: Int,
    val source: String,
    val lastSyncEpochMillis: Long
)
