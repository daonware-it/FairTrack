package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fairtrack.app.data.entity.ActivityEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEntryDao {

    @Query("SELECT * FROM activity_entries WHERE date = :date AND source = :source LIMIT 1")
    fun observeForDay(date: Long, source: String): Flow<ActivityEntry?>

    @Query("SELECT * FROM activity_entries WHERE date = :date AND source = :source LIMIT 1")
    suspend fun findForDay(date: Long, source: String): ActivityEntry?

    /**
     * REPLACE greift auch bei Kollision mit dem eindeutigen (date, source)-Index –
     * damit ist der Upsert idempotent. Der Aufrufer übernimmt die vorhandene id,
     * damit REPLACE nicht unnötig eine neue Zeilen-id vergibt.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ActivityEntry)
}
