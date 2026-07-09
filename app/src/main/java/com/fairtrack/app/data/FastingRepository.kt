package com.fairtrack.app.data

import com.fairtrack.app.data.dao.FastingSessionDao
import com.fairtrack.app.data.entity.FastingSession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf den Verlauf abgeschlossener Fasten (v0.13.0). */
@Singleton
class FastingRepository @Inject constructor(
    private val dao: FastingSessionDao
) {
    fun observeRecent(limit: Int = 10): Flow<List<FastingSession>> = dao.observeRecent(limit)

    fun observeCount(): Flow<Int> = dao.observeCount()

    /** Legt eine abgeschlossene Fasten-Zeile an. */
    suspend fun record(
        startEpochMillis: Long,
        endEpochMillis: Long,
        targetHours: Int,
        presetName: String
    ) {
        dao.insert(
            FastingSession(
                startEpochMillis = startEpochMillis,
                endEpochMillis = endEpochMillis,
                targetHours = targetHours,
                presetName = presetName
            )
        )
    }
}
