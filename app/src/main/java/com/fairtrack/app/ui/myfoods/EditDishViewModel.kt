package com.fairtrack.app.ui.myfoods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.CustomFoodRepository
import com.fairtrack.app.data.DishRepository
import com.fairtrack.app.data.DishTotals
import com.fairtrack.app.data.FoodPickBus
import com.fairtrack.app.data.FoodRepository
import com.fairtrack.app.data.SearchOutcome
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.totals
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Zustand der Online-Zutatensuche (Open Food Facts). */
sealed interface OnlineSearchUi {
    data object Idle : OnlineSearchUi
    data object Loading : OnlineSearchUi
    data class Results(val items: List<FoodItem>) : OnlineSearchUi
    data object Empty : OnlineSearchUi
    data object NetworkError : OnlineSearchUi
}

/**
 * ViewModel für den Editor "Gericht bearbeiten"/"Neues Gericht" (v0.5.0).
 * Hält Name, Portionen und die Zutatenliste im Bearbeitungszustand, berechnet
 * Live-Nährwerte und speichert das Gericht über das [DishRepository].
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class EditDishViewModel @Inject constructor(
    private val dishRepository: DishRepository,
    private val customFoodRepository: CustomFoodRepository,
    private val foodRepository: FoodRepository,
    foodPickBus: FoodPickBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** -1 = neues Gericht anlegen, sonst bestehende Dish-ID bearbeiten. */
    val dishId: Long = savedStateHandle.get<Long>("dishId") ?: -1L
    val isNew: Boolean = dishId == -1L

    /** Per Barcode gescannte Zutat (aus dem Scanner im "für Ergebnis"-Modus). */
    val scannedIngredient = foodPickBus.picked

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _servings = MutableStateFlow("1")
    val servings: StateFlow<String> = _servings.asStateFlow()

    private val _ingredients = MutableStateFlow<List<DishIngredient>>(emptyList())
    val ingredients: StateFlow<List<DishIngredient>> = _ingredients.asStateFlow()

    /** Eigene Lebensmittel des Nutzers – Auswahl für "aus meinen Lebensmitteln". */
    val ownFoods: StateFlow<List<FoodItem>> = customFoodRepository.observeCustomFoods()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Live-Summe aller Zutaten (Gesamt). Pro Portion wird in der UI berechnet. */
    val totals: StateFlow<DishTotals> = _ingredients
        .map { it.totals() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DishTotals(0.0, 0.0, 0.0, 0.0, 0.0)
        )

    /** Speicher-Button aktiv, wenn Name gesetzt, Zutaten vorhanden und Portionen > 0. */
    val isValid: StateFlow<Boolean> = combine(_name, _ingredients, _servings) { name, ings, servings ->
        name.isNotBlank() && ings.isNotEmpty() && (parseDecimal(servings)?.let { it > 0 } ?: false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    // --- Online-Zutatensuche (Open Food Facts) ---
    private val _onlineQuery = MutableStateFlow("")
    val onlineQuery: StateFlow<String> = _onlineQuery.asStateFlow()

    private val _onlineResults = MutableStateFlow<OnlineSearchUi>(OnlineSearchUi.Idle)
    val onlineResults: StateFlow<OnlineSearchUi> = _onlineResults.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                dishRepository.getDish(dishId)?.let { loaded ->
                    _name.value = loaded.dish.name
                    _servings.value = formatServings(loaded.dish.servings)
                    _ingredients.value = loaded.ingredients
                }
            }
        }
        // Debounced Auto-Suche, sobald mind. 2 Zeichen eingegeben sind.
        viewModelScope.launch {
            _onlineQuery.debounce(350).collectLatest { q ->
                if (q.trim().length >= 2) runOnlineSearch(q) else _onlineResults.value = OnlineSearchUi.Idle
            }
        }
    }

    fun onOnlineQueryChange(q: String) {
        _onlineQuery.value = q
        if (q.isBlank()) _onlineResults.value = OnlineSearchUi.Idle
    }

    fun searchOnline() {
        val q = _onlineQuery.value
        if (q.isBlank()) return
        viewModelScope.launch { runOnlineSearch(q) }
    }

    private suspend fun runOnlineSearch(query: String) {
        _onlineResults.value = OnlineSearchUi.Loading
        _onlineResults.value = when (val r = foodRepository.searchByText(query)) {
            is SearchOutcome.Success -> OnlineSearchUi.Results(r.items)
            SearchOutcome.Empty -> OnlineSearchUi.Empty
            SearchOutcome.NetworkError -> OnlineSearchUi.NetworkError
        }
    }

    /** Setzt die Online-Suche zurück (beim Schließen des Suchdialogs). */
    fun clearOnlineSearch() {
        _onlineQuery.value = ""
        _onlineResults.value = OnlineSearchUi.Idle
    }

    fun onNameChange(s: String) {
        _name.value = s
    }

    fun onServingsChange(s: String) {
        _servings.value = s
    }

    /** Fügt eine manuell erfasste Zutat an. */
    fun addIngredient(
        name: String,
        amountGrams: Double,
        kcalPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double
    ) {
        _ingredients.value = _ingredients.value + DishIngredient(
            dishId = 0,
            name = name.trim(),
            amountGrams = amountGrams,
            caloriesPer100g = kcalPer100g,
            proteinPer100g = proteinPer100g,
            carbsPer100g = carbsPer100g,
            fatPer100g = fatPer100g
        )
    }

    /** Übernimmt ein eigenes Lebensmittel (Nährwerte pro 100 g) als Zutat. */
    fun addIngredientFromFood(food: FoodItem, amountGrams: Double) {
        _ingredients.value = _ingredients.value + DishIngredient(
            dishId = 0,
            name = food.name,
            amountGrams = amountGrams,
            caloriesPer100g = food.caloriesPer100g,
            proteinPer100g = food.proteinPer100g,
            carbsPer100g = food.carbsPer100g,
            fatPer100g = food.fatPer100g
        )
    }

    fun updateIngredient(index: Int, ingredient: DishIngredient) {
        val current = _ingredients.value
        if (index in current.indices) {
            _ingredients.value = current.toMutableList().also { it[index] = ingredient }
        }
    }

    fun removeIngredient(index: Int) {
        val current = _ingredients.value
        if (index in current.indices) {
            _ingredients.value = current.toMutableList().also { it.removeAt(index) }
        }
    }

    /** Baut Dish + Zutaten und speichert; ruft danach [onDone] auf. */
    fun save(onDone: () -> Unit) {
        val parsedServings = parseDecimal(_servings.value)?.takeIf { it > 0 } ?: 1.0
        val dish = Dish(
            id = if (isNew) 0 else dishId,
            name = _name.value.trim(),
            servings = parsedServings
        )
        val ingredients = _ingredients.value
        viewModelScope.launch {
            dishRepository.saveDish(dish, ingredients)
            onDone()
        }
    }

    companion object {
        /** Parst eine Dezimalzahl; akzeptiert Komma als Trennzeichen. */
        fun parseDecimal(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

        /** Stellt Portionen ohne unnötiges ".0" dar. */
        private fun formatServings(value: Double): String =
            if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}
