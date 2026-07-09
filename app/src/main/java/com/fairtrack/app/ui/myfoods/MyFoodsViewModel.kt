package com.fairtrack.app.ui.myfoods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.AddEntryContext
import com.fairtrack.app.data.CustomFoodRepository
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.FavoritesRepository
import com.fairtrack.app.data.DishRepository
import com.fairtrack.app.data.DishTotals
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.densityPer100g
import com.fairtrack.app.data.entity.DishWithIngredients
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import com.fairtrack.app.data.perServing
import com.fairtrack.app.data.totals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel für "Meine Lebensmittel". Bündelt die eigenen Lebensmittel und
 * Gerichte des Nutzers, filtert sie nach dem Suchtext und legt bei Auswahl einen
 * Tagebuch-Eintrag am gemerkten Ziel (Tag + Mahlzeit aus [AddEntryContext]) an.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MyFoodsViewModel @Inject constructor(
    private val customFoodRepository: CustomFoodRepository,
    private val dishRepository: DishRepository,
    private val diaryRepository: DiaryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val addEntryContext: AddEntryContext
) : ViewModel() {

    /** Gemerktes Ziel (Tag + Mahlzeit) – wie auf dem Such-Screen. */
    val target = addEntryContext.target

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Eigene Lebensmittel, live gefiltert nach dem Suchtext. */
    val foods: StateFlow<List<FoodItem>> = _query
        .flatMapLatest { q -> customFoodRepository.searchCustomFoods(q) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Eigene Gerichte, live gefiltert nach dem Suchtext (auf dem Namen). */
    val dishes: StateFlow<List<DishWithIngredients>> =
        combine(dishRepository.observeDishes(), _query) { list, q ->
            val term = q.trim()
            if (term.isBlank()) list
            else list.filter { it.dish.name.contains(term, ignoreCase = true) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Übernimmt neue Sucheingabe. */
    fun onQueryChange(q: String) {
        _query.value = q
    }

    /** Ändert die gemerkte Mahlzeit (Tag bleibt erhalten). */
    fun setMeal(mealType: MealType) = addEntryContext.setMeal(mealType)

    /** Portionen eines Lebensmittels für den Hinzufügen-Dialog. */
    suspend fun portionsFor(foodId: Long): List<FoodPortion> =
        customFoodRepository.getPortions(foodId)

    /** Trägt ein Lebensmittel mit gewählter Menge/Einheit in das Tagebuch ein. */
    fun addFood(food: FoodItem, amountGrams: Double, unit: MeasureUnit, meal: MealType) {
        if (amountGrams <= 0) return
        viewModelScope.launch {
            diaryRepository.addEntry(
                epochDay = target.value.epochDay,
                mealType = meal,
                foodName = food.name,
                amountGrams = amountGrams,
                caloriesPer100g = food.caloriesPer100g,
                proteinPer100g = food.proteinPer100g,
                carbsPer100g = food.carbsPer100g,
                fatPer100g = food.fatPer100g,
                unit = unit
            )
        }
    }

    /** Trägt N Portionen eines Gerichts als einen Eintrag in das Tagebuch ein. */
    fun addDish(dish: DishWithIngredients, servings: Double, meal: MealType) {
        if (servings <= 0) return
        val totals = dish.ingredients.totals()
        val per = totals.perServing(dish.dish.servings)
        val add = DishTotals(
            grams = per.grams * servings,
            kcal = per.kcal * servings,
            protein = per.protein * servings,
            carbs = per.carbs * servings,
            fat = per.fat * servings
        )
        if (add.grams <= 0) return
        val density = add.densityPer100g()
        viewModelScope.launch {
            diaryRepository.addEntry(
                epochDay = target.value.epochDay,
                mealType = meal,
                foodName = dish.dish.name,
                amountGrams = add.grams,
                caloriesPer100g = density.kcal,
                proteinPer100g = density.protein,
                carbsPer100g = density.carbs,
                fatPer100g = density.fat
            )
        }
    }

    /** Schaltet den Favoriten-Status eines eigenen Lebensmittels um. */
    fun toggleFavorite(food: FoodItem) {
        viewModelScope.launch { favoritesRepository.setFavorite(food.id, !food.isFavorite) }
    }

    fun deleteFood(id: Long) {
        viewModelScope.launch { customFoodRepository.deleteFood(id) }
    }

    fun deleteDish(id: Long) {
        viewModelScope.launch { dishRepository.deleteDish(id) }
    }
}
