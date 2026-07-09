package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fairtrack.app.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodItemDao {

    @Query("SELECT * FROM food_items ORDER BY name ASC")
    fun observeAll(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): FoodItem?

    @Query("SELECT * FROM food_items WHERE isCustom = 1 ORDER BY name ASC")
    fun observeCustom(): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE isCustom = 1 AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchCustom(query: String): Flow<List<FoodItem>>

    @Query("SELECT * FROM food_items WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): FoodItem?

    /** Als Favorit markierte Lebensmittel (Schnellzugriff im Such-Tab). */
    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun observeFavorites(): Flow<List<FoodItem>>

    @Query("UPDATE food_items SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM food_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FoodItem): Long
}
