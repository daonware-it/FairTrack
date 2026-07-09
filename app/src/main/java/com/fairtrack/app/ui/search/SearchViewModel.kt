package com.fairtrack.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.AddEntryContext
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.FavoritesRepository
import com.fairtrack.app.data.FoodRepository
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.MealTemplateRepository
import com.fairtrack.app.data.SearchHistoryRepository
import com.fairtrack.app.data.SearchOutcome
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.MealTemplateWithItems
import com.fairtrack.app.ui.home.QuickAddResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Zustand der Lebensmittelsuche für die UI. */
sealed interface SearchUiState {
    /** Noch keine Suche gestartet – zeigt Verlauf und Aktionen. */
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Results(val items: List<FoodItem>) : SearchUiState
    data object Empty : SearchUiState
    data object NetworkError : SearchUiState
}

/**
 * ViewModel für den Such-Einstieg. Stellt das gemerkte Ziel (Datum + Mahlzeit)
 * aus dem [AddEntryContext] bereit, führt die Volltextsuche über das
 * [FoodRepository] aus (mit Debounce-Autosuche und manuellem Auslösen) und
 * speichert Einträge auf genau diesen Tag/diese Mahlzeit – auch für vergangene
 * Tage.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val addEntryContext: AddEntryContext,
    private val diaryRepository: DiaryRepository,
    private val foodRepository: FoodRepository,
    private val searchHistoryRepository: SearchHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val mealTemplateRepository: MealTemplateRepository
) : ViewModel() {

    val target = addEntryContext.target

    /** Als Favorit markierte Lebensmittel für den Schnellzugriff. */
    val favorites: StateFlow<List<FoodItem>> = favoritesRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Zuletzt gegessene Einträge (distinct nach Name), neueste zuerst. */
    val recentEaten: StateFlow<List<DiaryEntry>> = diaryRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Gespeicherte Mahlzeiten-Vorlagen. */
    val templates: StateFlow<List<MealTemplateWithItems>> = mealTemplateRepository.observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    /** Zuletzt gesuchte Begriffe, neueste zuerst. */
    val recentSearches: StateFlow<List<String>> = searchHistoryRepository.recent
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    init {
        // Auto-Suche: Tippen entprellen und ab 2 Zeichen suchen. flatMapLatest/
        // mapLatest bricht eine noch laufende Suche bei neuer Eingabe ab.
        _query
            .debounce(350)
            .filter { it.trim().length >= 2 }
            .mapLatest { term -> runSearch(term) }
            .launchIn(viewModelScope)
    }

    /** Übernimmt neue Eingabe; leert bei leerem Feld sofort das Ergebnis. */
    fun onQueryChange(q: String) {
        _query.value = q
        if (q.isBlank()) {
            _uiState.value = SearchUiState.Idle
        }
    }

    /** Manuelles Auslösen (z. B. Tastatur-Aktion "Suchen"). */
    fun submitSearch() {
        val term = _query.value
        if (term.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        viewModelScope.launch { runSearch(term) }
    }

    /** Führt die eigentliche Suche aus und bildet das Ergebnis auf den UI-State ab. */
    private suspend fun runSearch(term: String) {
        if (term.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }
        _uiState.value = SearchUiState.Loading
        when (val outcome = foodRepository.searchByText(term)) {
            is SearchOutcome.Success -> {
                _uiState.value = SearchUiState.Results(outcome.items)
                searchHistoryRepository.add(term)
            }
            is SearchOutcome.Empty -> {
                _uiState.value = SearchUiState.Empty
                searchHistoryRepository.add(term)
            }
            is SearchOutcome.NetworkError -> {
                _uiState.value = SearchUiState.NetworkError
            }
        }
    }

    /** Löscht den Suchverlauf. */
    fun clearHistory() {
        viewModelScope.launch { searchHistoryRepository.clear() }
    }

    /** Übernimmt einen Verlaufseintrag ins Suchfeld und startet die Suche. */
    fun useRecent(term: String) {
        _query.value = term
        submitSearch()
    }

    /** Ändert die gemerkte Mahlzeit (Tag bleibt erhalten). */
    fun setMeal(mealType: MealType) = addEntryContext.setMeal(mealType)

    /** Speichert einen manuell erfassten Eintrag am gemerkten Tag/Mahlzeit. */
    fun addManual(result: QuickAddResult) {
        val current = addEntryContext.target.value
        viewModelScope.launch {
            diaryRepository.addEntry(
                epochDay = current.epochDay,
                mealType = current.mealType,
                foodName = result.name,
                amountGrams = result.amountGrams,
                caloriesPer100g = result.caloriesPer100g,
                proteinPer100g = result.proteinPer100g,
                carbsPer100g = result.carbsPer100g,
                fatPer100g = result.fatPer100g,
                unit = result.unit
            )
        }
    }

    /** Schaltet den Favoriten-Status eines Treffers/Favoriten um. */
    fun toggleFavorite(item: FoodItem) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(item) }
    }

    /** true, wenn der Treffer in der aktuellen Favoritenliste enthalten ist. */
    fun isFavorite(item: FoodItem): Boolean =
        favoritesRepository.isFavorite(item, favorites.value)

    /** Wendet eine Vorlage an: schreibt alle Elemente auf das gemerkte Ziel. */
    fun applyTemplate(template: MealTemplateWithItems) {
        val current = addEntryContext.target.value
        viewModelScope.launch {
            template.items.forEach { item ->
                diaryRepository.addEntry(
                    epochDay = current.epochDay,
                    mealType = current.mealType,
                    foodName = item.name,
                    amountGrams = item.amountGrams,
                    caloriesPer100g = item.caloriesPer100g,
                    proteinPer100g = item.proteinPer100g,
                    carbsPer100g = item.carbsPer100g,
                    fatPer100g = item.fatPer100g,
                    unit = item.unit
                )
            }
        }
    }

    /** Löscht eine Vorlage. */
    fun deleteTemplate(id: Long) {
        viewModelScope.launch { mealTemplateRepository.deleteTemplate(id) }
    }

    /** Speichert einen aus einem Suchtreffer bestätigten Eintrag. */
    fun addEntry(
        mealType: MealType,
        name: String,
        amountGrams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: MeasureUnit,
        microsPer100g: Micronutrients = Micronutrients()
    ) {
        viewModelScope.launch {
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
                microsPer100g = microsPer100g
            )
        }
    }
}
