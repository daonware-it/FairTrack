package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fairtrack.app.data.entity.FastingSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FastingSessionDao {

    @Insert
    suspend fun insert(session: FastingSession): Long

    /** Die zuletzt abgeschlossenen Fasten (neueste zuerst). */
    @Query("SELECT * FROM fasting_sessions ORDER BY endEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<FastingSession>>

    /** Anzahl aller abgeschlossenen Fasten. */
    @Query("SELECT COUNT(*) FROM fasting_sessions")
    fun observeCount(): Flow<Int>
}
