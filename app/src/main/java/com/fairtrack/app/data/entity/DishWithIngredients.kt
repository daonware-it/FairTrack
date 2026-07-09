package com.fairtrack.app.data.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Room-Relation: ein Gericht mit allen zugehörigen Zutaten.
 */
data class DishWithIngredients(
    @Embedded val dish: Dish,
    @Relation(parentColumn = "id", entityColumn = "dishId") val ingredients: List<DishIngredient>
)
