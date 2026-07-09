package com.fairtrack.app.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.R
import com.fairtrack.app.data.AddEntryContext
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.FoodPickBus
import com.fairtrack.app.data.FoodRepository
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.ProductLookupResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel für den Barcode-Scanner (v0.3.0). Schlägt gescannte Barcodes über
 * das [FoodRepository] nach und legt bestätigte Produkte via [DiaryRepository]
 * am gemerkten Tag ([AddEntryContext], auch in der Vergangenheit) ab.
 *
 * Im "für Ergebnis"-Modus (`forResult=true`, z. B. Zutat für ein Gericht) wird
 * nichts ins Tagebuch geschrieben: das gefundene Produkt geht über den
 * [FoodPickBus] an den aufrufenden Screen zurück und der Scanner schließt sich.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val diaryRepository: DiaryRepository,
    private val addEntryContext: AddEntryContext,
    private val foodPickBus: FoodPickBus,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** true = gescanntes Produkt zurückgeben statt ins Tagebuch schreiben. */
    private val forResult: Boolean = savedStateHandle.get<Boolean>("forResult") ?: false

    /** Gemerktes Ziel (Tag + Mahlzeit) für Vorauswahl und Speicherung. */
    val target = addEntryContext.target

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Scanning)
    val uiState = _uiState.asStateFlow()

    private val _finished = Channel<Unit>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()

    /**
     * Wird von der Kamera bei jedem erkannten Barcode aufgerufen. Ignoriert
     * Aufrufe, solange nicht aktiv gescannt wird (Debounce gegen Mehrfach-Lookups).
     */
    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value != ScannerUiState.Scanning) return
        _uiState.value = ScannerUiState.LookingUp
        viewModelScope.launch {
            val r = foodRepository.lookupByBarcode(barcode)
            if (forResult) {
                // Zutat-Modus: gefundenes Produkt zurückgeben und schließen.
                when (r) {
                    is ProductLookupResult.Found -> {
                        foodPickBus.emit(r.item)
                        _finished.send(Unit)
                    }
                    is ProductLookupResult.NotFound ->
                        _uiState.value = ScannerUiState.Error(R.string.scanner_not_found)
                    ProductLookupResult.NetworkError ->
                        _uiState.value = ScannerUiState.Error(R.string.scanner_error_network)
                }
            } else {
                _uiState.value = when (r) {
                    is ProductLookupResult.Found -> ScannerUiState.Confirm(r.item)
                    is ProductLookupResult.NotFound -> ScannerUiState.ManualEntry(r.barcode)
                    ProductLookupResult.NetworkError -> ScannerUiState.Error(R.string.scanner_error_network)
                }
            }
        }
    }

    /** Zurück zur Kamera (nach Abbruch, Fehler oder manueller Eingabe). */
    fun resumeScanning() {
        _uiState.value = ScannerUiState.Scanning
    }

    /** Speichert den bestätigten Eintrag am gemerkten Tag und signalisiert Abschluss. */
    fun addToDiary(
        mealType: MealType,
        name: String,
        amountGrams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: MeasureUnit = MeasureUnit.GRAMS,
        microsPer100g: Micronutrients = Micronutrients()
    ) {
        viewModelScope.launch {
            diaryRepository.addEntry(
                epochDay = addEntryContext.target.value.epochDay,
                mealType = mealType,
                foodName = name,
                amountGrams = amountGrams,
                caloriesPer100g = kcal,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                fatPer100g = fat,
                unit = unit,
                microsPer100g = microsPer100g
            )
            _finished.send(Unit)
        }
    }
}
