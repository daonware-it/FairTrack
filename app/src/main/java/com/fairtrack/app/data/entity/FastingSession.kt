package com.fairtrack.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Ein abgeschlossenes Fasten-Intervall (v0.13.0) für den Verlauf.
 * [startEpochMillis]/[endEpochMillis] in Wanduhr-Millis (System.currentTimeMillis),
 * [targetHours] = angepeiltes Fastenfenster, [presetName] = [com.fairtrack.app.data.FastingProtocol.name].
 * Ein laufendes Fasten wird NICHT hier gehalten (nur im DataStore), erst der
 * Abschluss erzeugt eine Verlaufs-Zeile.
 */
@Entity(tableName = "fasting_sessions")
data class FastingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val targetHours: Int,
    val presetName: String
)
