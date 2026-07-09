package com.fairtrack.app.data

import com.fairtrack.app.data.dao.DishDao
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.DishWithIngredients
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Zugriff auf eigene Gerichte (Rezepte) samt Zutaten (v0.5.0). */
@Singleton
class DishRepository @Inject constructor(private val dishDao: DishDao) {

    fun observeDishes(): Flow<List<DishWithIngredients>> = dishDao.observeAllWithIngredients()

    suspend fun getDish(id: Long): DishWithIngredients? = dishDao.getWithIngredients(id)

    /** Speichert ein Gericht inkl. Zutaten (id==0 => neu). Zutaten werden ersetzt. Gibt die Dish-ID zurück. */
    suspend fun saveDish(dish: Dish, ingredients: List<DishIngredient>): Long {
        // insertDish nutzt @Insert(REPLACE): neu bei id==0, sonst Ersetzen des bestehenden Gerichts.
        // REPLACE eines bestehenden PK liefert dessen rowId zurück; wir behalten trotzdem die bekannte id.
        val dishId = dishDao.insertDish(dish).let { if (dish.id == 0L) it else dish.id }
        dishDao.deleteIngredients(dishId)
        dishDao.insertIngredients(ingredients.map { it.copy(id = 0, dishId = dishId) })
        return dishId
    }

    suspend fun deleteDish(id: Long) = dishDao.deleteDish(id)
}
