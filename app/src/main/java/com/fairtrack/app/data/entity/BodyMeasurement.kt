package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Körpermaße für einen Tag (v0.12.0). epochDay ist Primärschlüssel, damit pro Tag
 * genau ein Satz Maße existiert (erneutes Eintragen ersetzt bzw. ergänzt die Werte).
 * Alle Maße sind nullable – es werden selten alle Werte an einem Tag gemessen.
 * Persistenz erfolgt metrisch (cm); die Umrechnung nach Zoll geschieht nur in der
 * Anzeige-/Eingabeschicht (siehe [com.fairtrack.app.data.UnitFormatter]).
 */
@Entity(tableName = "body_measurements")
@Serializable
data class BodyMeasurement(
    @PrimaryKey val epochDay: Long,
    val waistCm: Double? = null,
    val bodyFatPercent: Double? = null,
    val chestCm: Double? = null,
    val armCm: Double? = null
)
