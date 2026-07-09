package com.fairtrack.app.data

import com.fairtrack.app.data.dao.WaterEntryDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf die getrunkene Tages-Wassermenge (v0.7.0). */
@Singleton
class WaterRepository @Inject constructor(
    private val dao: WaterEntryDao
) {
    /** Getrunkene Menge eines Tages in ml (0, solange kein Eintrag existiert). */
    fun observeForDay(epochDay: Long): Flow<Int> =
        dao.observeAmount(epochDay).map { it ?: 0 }

    /** Addiert [deltaMl] auf den Tageswert (Schnell-Buttons). */
    suspend fun addWater(epochDay: Long, deltaMl: Int) = dao.addWater(epochDay, deltaMl)

    /** Setzt den Tageswert direkt (Korrektur / Zurücksetzen). */
    suspend fun setAmount(epochDay: Long, amountMl: Int) = dao.setAmount(epochDay, amountMl)
}
