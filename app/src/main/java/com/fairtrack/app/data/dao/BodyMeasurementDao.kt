package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fairtrack.app.data.entity.BodyMeasurement
import kotlinx.coroutines.flow.Flow

@Dao
interface BodyMeasurementDao {

    @Query("SELECT * FROM body_measurements ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements WHERE epochDay = :epochDay")
    suspend fun findByDay(epochDay: Long): BodyMeasurement?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: BodyMeasurement)

    @Delete
    suspend fun delete(entry: BodyMeasurement)
}
