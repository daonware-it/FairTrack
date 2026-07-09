package com.fairtrack.app.data

import com.fairtrack.app.data.dao.WeightEntryDao
import com.fairtrack.app.data.entity.WeightEntry
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf die Gewichts-Historie (v0.9.0). */
@Singleton
class WeightRepository @Inject constructor(
    private val dao: WeightEntryDao
) {
    /** Alle Einträge chronologisch aufsteigend. */
    val history: Flow<List<WeightEntry>> = dao.observeAll()

    /** Trägt das Gewicht für einen Tag ein (ersetzt vorhandenen Tageswert). */
    suspend fun logWeight(epochDay: Long, weightKg: Double) =
        dao.upsert(WeightEntry(epochDay, weightKg))

    suspend fun delete(entry: WeightEntry) = dao.delete(entry)
}
