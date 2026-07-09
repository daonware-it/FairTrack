package com.fairtrack.app.ui.myfoods

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.CustomFoodRepository
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Eine bearbeitbare Portionszeile im Editor (Roh-Strings aus den Feldern). */
data class PortionRow(
    val label: String = "",
    val grams: String = ""
)

/** Bearbeitbarer Zustand eines eigenen Lebensmittels. */
data class EditFoodState(
    val name: String = "",
    val brand: String = "",
    val calories: String = "",
    val protein: String = "",
    val carbs: String = "",
    val fat: String = "",
    val isBeverage: Boolean = false,
    val portions: List<PortionRow> = emptyList()
)

/**
 * ViewModel für den Lebensmittel-Editor. Lädt bei bekannter foodId das
 * bestehende Lebensmittel samt Portionen, hält die Eingaben als Strings und
 * baut beim Speichern die Entities für das [CustomFoodRepository].
 */
@HiltViewModel
class EditFoodViewModel @Inject constructor(
    private val customFoodRepository: CustomFoodRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val foodId: Long = savedStateHandle.get<Long>("foodId") ?: -1L

    /** true = neues Lebensmittel (kein Datensatz geladen). */
    val isNew: Boolean = foodId == -1L

    private val _state = MutableStateFlow(EditFoodState())
    val state: StateFlow<EditFoodState> = _state.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch {
                val food = customFoodRepository.getFood(foodId)
                val portions = customFoodRepository.getPortions(foodId)
                if (food != null) {
                    _state.value = EditFoodState(
                        name = food.name,
                        brand = food.brand.orEmpty(),
                        calories = fmt(food.caloriesPer100g),
                        protein = fmt(food.proteinPer100g),
                        carbs = fmt(food.carbsPer100g),
                        fat = fmt(food.fatPer100g),
                        isBeverage = food.isBeverage,
                        portions = portions.map { PortionRow(it.label, fmt(it.grams)) }
                    )
                }
            }
        }
    }

    fun setName(v: String) = _state.update { it.copy(name = v) }
    fun setBrand(v: String) = _state.update { it.copy(brand = v) }
    fun setCalories(v: String) = _state.update { it.copy(calories = v) }
    fun setProtein(v: String) = _state.update { it.copy(protein = v) }
    fun setCarbs(v: String) = _state.update { it.copy(carbs = v) }
    fun setFat(v: String) = _state.update { it.copy(fat = v) }
    fun setBeverage(v: Boolean) = _state.update { it.copy(isBeverage = v) }

    fun addPortionRow() = _state.update { it.copy(portions = it.portions + PortionRow()) }

    fun updatePortion(index: Int, label: String, grams: String) = _state.update { s ->
        val list = s.portions.toMutableList()
        if (index in list.indices) list[index] = PortionRow(label, grams)
        s.copy(portions = list)
    }

    fun removePortion(index: Int) = _state.update { s ->
        val list = s.portions.toMutableList()
        if (index in list.indices) list.removeAt(index)
        s.copy(portions = list)
    }

    /** Gültig, wenn ein Name vorhanden und die Kalorien parsbar (>= 0) sind. */
    val isValid: Boolean
        get() {
            val s = _state.value
            val kcal = parse(s.calories)
            return s.name.isNotBlank() && kcal != null && kcal >= 0
        }

    /** Baut die Entities und speichert; ruft danach [onDone] auf. */
    fun save(onDone: () -> Unit) {
        val s = _state.value
        val kcal = parse(s.calories) ?: return
        if (s.name.isBlank()) return
        val food = FoodItem(
            id = if (isNew) 0L else foodId,
            name = s.name.trim(),
            brand = s.brand.trim().ifBlank { null },
            caloriesPer100g = kcal,
            proteinPer100g = parse(s.protein) ?: 0.0,
            carbsPer100g = parse(s.carbs) ?: 0.0,
            fatPer100g = parse(s.fat) ?: 0.0,
            isBeverage = s.isBeverage,
            isCustom = true
        )
        // Nur vollständige Portionszeilen übernehmen (Label + gültige Grammzahl > 0).
        val portions = s.portions.mapNotNull { row ->
            val grams = parse(row.grams)
            if (row.label.isNotBlank() && grams != null && grams > 0) {
                FoodPortion(foodItemId = 0, label = row.label.trim(), grams = grams)
            } else null
        }
        viewModelScope.launch {
            customFoodRepository.saveFood(food, portions)
            onDone()
        }
    }

    private fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

    private fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()
}
