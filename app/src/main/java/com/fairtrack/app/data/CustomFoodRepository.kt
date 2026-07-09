package com.fairtrack.app.data

import com.fairtrack.app.data.dao.FoodItemDao
import com.fairtrack.app.data.dao.FoodPortionDao
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf eigene Lebensmittel des Nutzers samt Portionen (v0.5.0). */
@Singleton
class CustomFoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val foodPortionDao: FoodPortionDao
) {
    fun observeCustomFoods(): Flow<List<FoodItem>> = foodItemDao.observeCustom()

    fun searchCustomFoods(query: String): Flow<List<FoodItem>> =
        if (query.isBlank()) foodItemDao.observeCustom() else foodItemDao.searchCustom(query.trim())

    suspend fun getFood(id: Long): FoodItem? = foodItemDao.findById(id)

    fun observePortions(foodId: Long): Flow<List<FoodPortion>> = foodPortionDao.observeForFood(foodId)

    suspend fun getPortions(foodId: Long): List<FoodPortion> = foodPortionDao.getForFood(foodId)

    /** Legt ein eigenes Lebensmittel an oder aktualisiert es (id==0 => neu). Portionen werden ersetzt. Gibt die Food-ID zurück. */
    suspend fun saveFood(food: FoodItem, portions: List<FoodPortion>): Long {
        val id = foodItemDao.upsert(food.copy(isCustom = true))   // REPLACE = insert/update
        // Room upsert(REPLACE) mit autoGenerate: bei id==0 liefert upsert die neue rowId; bei id!=0 die gleiche id.
        val foodId = if (food.id == 0L) id else food.id
        foodPortionDao.deleteForFood(foodId)
        portions.forEach { foodPortionDao.insert(it.copy(id = 0, foodItemId = foodId)) }
        return foodId
    }

    suspend fun deleteFood(id: Long) = foodItemDao.deleteById(id)   // CASCADE löscht Portionen
}
