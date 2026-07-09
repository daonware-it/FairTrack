package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.DishWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {

    @Transaction
    @Query("SELECT * FROM dishes ORDER BY name ASC")
    fun observeAllWithIngredients(): Flow<List<DishWithIngredients>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE id = :id LIMIT 1")
    suspend fun getWithIngredients(id: Long): DishWithIngredients?

    /** REPLACE: neues Gericht (id==0) oder Update eines bestehenden (id!=0). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDish(dish: Dish): Long

    @Insert
    suspend fun insertIngredients(ingredients: List<DishIngredient>)

    @Query("DELETE FROM dish_ingredients WHERE dishId = :dishId")
    suspend fun deleteIngredients(dishId: Long)

    @Query("DELETE FROM dishes WHERE id = :id")
    suspend fun deleteDish(id: Long)
}
