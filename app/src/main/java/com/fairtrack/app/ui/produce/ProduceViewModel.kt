package com.fairtrack.app.ui.produce

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.AddEntryContext
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.bls.BlsMicronutrientCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel für den Obst/Gemüse-Screen (Produce). Legt ausgewählte Sorten aus
 * dem Offline-Katalog via [DiaryRepository] am gemerkten Tag ([AddEntryContext],
 * auch in der Vergangenheit) und in der gemerkten Mahlzeit ab. Spiegelt das
 * Verhalten des [com.fairtrack.app.ui.scanner.ScannerViewModel].
 */
@HiltViewModel
class ProduceViewModel @Inject constructor(
    private val diaryRepository: DiaryRepository,
    private val addEntryContext: AddEntryContext
) : ViewModel() {

    /** Gemerktes Ziel (Tag + Mahlzeit) für Vorauswahl und Speicherung. */
    val target = addEntryContext.target

    private val _finished = Channel<Unit>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()

    /** Speichert die gewählte Sorte am gemerkten Tag und signalisiert Abschluss. */
    fun addProduce(
        mealType: MealType,
        name: String,
        amountGrams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: MeasureUnit
    ) {
        viewModelScope.launch {
            // Zweite Quelle: Mikronährstoffe pro 100 g aus dem kuratierten
            // BLS-Offlinekatalog (Basislebensmittel ohne Barcode).
            val micros = BlsMicronutrientCatalog.forFood(name) ?: Micronutrients()
            diaryRepository.addEntry(
                epochDay = target.value.epochDay,
                mealType = mealType,
                foodName = name,
                amountGrams = amountGrams,
                caloriesPer100g = kcal,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                unit = unit,
                microsPer100g = micros
            )
            _finished.send(Unit)
        }
    }
}
