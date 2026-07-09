package com.fairtrack.app.data

import com.fairtrack.app.data.dao.BodyMeasurementDao
import com.fairtrack.app.data.entity.BodyMeasurement
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf die Körpermaße-Historie (v0.12.0). */
@Singleton
class BodyMeasurementRepository @Inject constructor(
    private val dao: BodyMeasurementDao
) {
    /** Alle Einträge chronologisch aufsteigend. */
    val history: Flow<List<BodyMeasurement>> = dao.observeAll()

    /**
     * Trägt Körpermaße für einen Tag ein. Nur die übergebenen (nicht-null) Maße
     * werden gesetzt; bereits vorhandene Werte anderer Maße bleiben per
     * Read-modify-write erhalten. Alle Werte sind metrisch (cm bzw. %).
     */
    suspend fun saveMeasurements(
        epochDay: Long,
        waistCm: Double? = null,
        bodyFatPercent: Double? = null,
        chestCm: Double? = null,
        armCm: Double? = null
    ) {
        val existing = dao.findByDay(epochDay)
        val merged = BodyMeasurement(
            epochDay = epochDay,
            waistCm = waistCm ?: existing?.waistCm,
            bodyFatPercent = bodyFatPercent ?: existing?.bodyFatPercent,
            chestCm = chestCm ?: existing?.chestCm,
            armCm = armCm ?: existing?.armCm
        )
        dao.upsert(merged)
    }

    suspend fun delete(entry: BodyMeasurement) = dao.delete(entry)
}
