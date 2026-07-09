package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fairtrack.app.data.entity.WaterEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterEntryDao {

    /** Getrunkene Menge eines Tages als Flow (null, solange kein Eintrag existiert). */
    @Query("SELECT amountMl FROM water_entries WHERE epochDay = :epochDay")
    fun observeAmount(epochDay: Long): Flow<Int?>

    @Query("SELECT amountMl FROM water_entries WHERE epochDay = :epochDay")
    suspend fun getAmount(epochDay: Long): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WaterEntry)

    /**
     * Addiert [deltaMl] auf den Tageswert (legt bei fehlendem Eintrag an).
     * Negatives Delta ist erlaubt, der Tageswert bleibt aber >= 0.
     */
    @Transaction
    suspend fun addWater(epochDay: Long, deltaMl: Int) {
        val current = getAmount(epochDay) ?: 0
        upsert(WaterEntry(epochDay, (current + deltaMl).coerceAtLeast(0)))
    }

    /** Setzt den Tageswert direkt (z. B. zum Korrigieren oder Zurücksetzen). */
    suspend fun setAmount(epochDay: Long, amountMl: Int) =
        upsert(WaterEntry(epochDay, amountMl.coerceAtLeast(0)))
}
