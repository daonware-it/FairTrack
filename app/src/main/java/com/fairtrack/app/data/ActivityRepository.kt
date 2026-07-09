package com.fairtrack.app.data

import com.fairtrack.app.data.activity.ActivitySourceType
import com.fairtrack.app.data.activity.DailyActivity
import com.fairtrack.app.data.dao.ActivityEntryDao
import com.fairtrack.app.data.entity.ActivityEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf die importierten Bewegungs-Tagesaggregate. */
@Singleton
class ActivityRepository @Inject constructor(
    private val dao: ActivityEntryDao
) {

    /** Beobachtet den Datensatz eines Tages für die gewählte Quelle (oder null). */
    fun observeForDay(epochDay: Long, source: ActivitySourceType): Flow<ActivityEntry?> =
        dao.observeForDay(epochDay, source.sourceId)

    /** Aktivkalorien eines Tages für die gewählte Quelle (0, falls kein Datensatz). */
    fun observeActiveKcal(epochDay: Long, source: ActivitySourceType): Flow<Int> =
        observeForDay(epochDay, source).map { it?.activeKcal ?: 0 }

    /**
     * Idempotenter Upsert eines Tagesaggregats: bei vorhandenem (date, source)
     * wird der Datensatz ersetzt statt dupliziert. Die vorhandene id bleibt
     * erhalten.
     */
    suspend fun upsert(daily: DailyActivity, source: ActivitySourceType) {
        val epochDay = daily.date.toEpochDay()
        val existing = dao.findForDay(epochDay, source.sourceId)
        dao.upsert(
            ActivityEntry(
                id = existing?.id ?: 0,
                date = epochDay,
                steps = daily.steps,
                activeKcal = daily.activeKcal,
                source = source.sourceId,
                lastSyncEpochMillis = System.currentTimeMillis()
            )
        )
    }
}
