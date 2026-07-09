package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.fairtrack.app.data.entity.FoodPortion
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodPortionDao {

    @Query("SELECT * FROM food_portions WHERE foodItemId = :foodId ORDER BY grams ASC")
    fun observeForFood(foodId: Long): Flow<List<FoodPortion>>

    @Query("SELECT * FROM food_portions WHERE foodItemId = :foodId ORDER BY grams ASC")
    suspend fun getForFood(foodId: Long): List<FoodPortion>

    @Insert
    suspend fun insert(portion: FoodPortion): Long

    @Query("DELETE FROM food_portions WHERE foodItemId = :foodId")
    suspend fun deleteForFood(foodId: Long)
}
