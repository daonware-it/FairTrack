package com.fairtrack.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.ActivityRepository
import com.fairtrack.app.data.AddEntryContext
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.MealTemplateRepository
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.NutritionGoals
import com.fairtrack.app.data.SelectedDayCoordinator
import com.fairtrack.app.data.UserProfileRepository
import com.fairtrack.app.data.WaterPreferencesRepository
import com.fairtrack.app.data.WaterRepository
import com.fairtrack.app.data.activity.ActivitySourceRegistry
import com.fairtrack.app.data.activity.ActivitySourceType
import com.fairtrack.app.data.activity.activityAdjustedGoals
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.MealTemplateItem
import com.fairtrack.app.work.ActivitySyncScheduler
import com.fairtrack.app.work.WaterReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: DiaryRepository,
    private val addEntryContext: AddEntryContext,
    private val mealTemplateRepository: MealTemplateRepository,
    private val waterRepository: WaterRepository,
    private val waterPreferences: WaterPreferencesRepository,
    private val reminderScheduler: WaterReminderScheduler,
    private val activityRepository: ActivityRepository,
    private val activityPreferences: ActivityPreferencesRepository,
    private val activitySourceRegistry: ActivitySourceRegistry,
    private val activitySyncScheduler: ActivitySyncScheduler,
    private val selectedDayCoordinator: SelectedDayCoordinator,
    userProfileRepository: UserProfileRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val selectedDate = MutableStateFlow(today)

    val uiState: StateFlow<HomeUiState> =
        selectedDate.flatMapLatest { date ->
            // Kalorienziel inkl. optionalem Bewegungs-Aufschlag (Flag im Settings,
            // Default aus) – zentral über activityAdjustedGoals, damit Home,
            // Statistik und Widget dasselbe Budget zeigen.
            val goalsForDay = activityAdjustedGoals(
                userProfileRepository.goals,
                activityRepository,
                activityPreferences,
                date.toEpochDay()
            )
            combine(goalsForDay, repository.observeDay(date.toEpochDay())) { goals, entries ->
                HomeUiState.from(date, today, entries, goals)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.from(today, today, emptyList(), NutritionGoals.DEFAULT)
        )

    // Verfügbarkeit + Berechtigung der Bewegungsquelle (einmalige Suspend-Abfragen,
    // deshalb als eigener State, der bei Bedarf neu geladen wird).
    private val activityAccess = MutableStateFlow(ActivityAccess())

    val activityState: StateFlow<ActivityUiState> =
        combine(
            selectedDate,
            activityPreferences.selectedSource,
            activityPreferences.addActivityCaloriesToBudget,
            activityAccess
        ) { date, source, addToBudget, access ->
            ActivityQuery(date, source, addToBudget, access)
        }.flatMapLatest { q ->
            activityRepository.observeForDay(q.date.toEpochDay(), q.source).map { entry ->
                ActivityUiState(
                    available = q.access.available,
                    hasPermission = q.access.hasPermission,
                    steps = entry?.steps ?: 0,
                    activeKcal = entry?.activeKcal ?: 0,
                    addToBudget = q.addToBudget
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ActivityUiState()
        )

    init {
        refreshActivityAccess()
        observeRequestedDay()
    }

    /**
     * Springt zu dem Tag, den der Tagebuch-Tab angefragt hat, und quittiert die
     * Anfrage sofort — sonst würde jede erneute Sammlung wieder dorthin springen
     * und ein Weiterblättern von Hand unmöglich machen.
     */
    private fun observeRequestedDay() {
        viewModelScope.launch {
            selectedDayCoordinator.requestedDate.collect { date ->
                if (date != null) {
                    selectDate(date)
                    selectedDayCoordinator.consume()
                }
            }
        }
    }

    /**
     * Lädt Verfügbarkeit + Berechtigung der gewählten Quelle neu und stößt bei
     * erteilter Berechtigung den periodischen sowie einen sofortigen Sync an
     * (Sync beim Öffnen des Home-Screens). Wird auch nach dem Permission-Flow
     * der [ActivityCard] aufgerufen.
     */
    fun refreshActivityAccess() {
        viewModelScope.launch {
            val type = activityPreferences.selectedSource.first()
            val source = activitySourceRegistry.forType(type)
            val available = source != null && source.isAvailable()
            val hasPermission = available && source.hasPermissions()
            activityAccess.value = ActivityAccess(available, hasPermission)
            if (hasPermission) {
                activitySyncScheduler.schedulePeriodic()
                activitySyncScheduler.syncNow()
            }
        }
    }

    private data class ActivityAccess(
        val available: Boolean = false,
        val hasPermission: Boolean = false
    )

    private data class ActivityQuery(
        val date: LocalDate,
        val source: ActivitySourceType,
        val addToBudget: Boolean,
        val access: ActivityAccess
    )

    /** Wasser-Karte: getrunkene Menge des gewählten Tages + Tagesziel. */
    val waterState: StateFlow<WaterUiState> =
        combine(selectedDate, waterPreferences.goalMl) { date, goal -> date to goal }
            .flatMapLatest { (date, goal) ->
                waterRepository.observeForDay(date.toEpochDay()).map { consumed ->
                    WaterUiState(consumedMl = consumed, goalMl = goal)
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = WaterUiState()
            )

    /** Trink-Erinnerung: Ein/Aus + Intervall (für die Einstell-UI). */
    val reminderState: StateFlow<WaterReminderState> =
        combine(
            waterPreferences.reminderEnabled,
            waterPreferences.reminderIntervalHours
        ) { enabled, hours -> WaterReminderState(enabled, hours) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = WaterReminderState()
            )

    /** Fügt dem aktuell gewählten Tag Wasser hinzu (Schnell-Buttons). */
    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            waterRepository.addWater(selectedDate.value.toEpochDay(), deltaMl)
        }
    }

    /** Setzt den Wasserstand des gewählten Tages zurück. */
    fun resetWater() {
        viewModelScope.launch {
            waterRepository.setAmount(selectedDate.value.toEpochDay(), 0)
        }
    }

    /** Speichert ein neues Wasser-Tagesziel in ml. */
    fun setWaterGoal(goalMl: Int) {
        viewModelScope.launch { waterPreferences.setGoalMl(goalMl) }
    }

    /** Schaltet die Trink-Erinnerung ein/aus und plant den Worker entsprechend. */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            waterPreferences.setReminderEnabled(enabled)
            reminderScheduler.update(enabled, reminderState.value.intervalHours)
        }
    }

    /** Ändert das Erinnerungs-Intervall (Stunden) und plant ggf. neu. */
    fun setReminderInterval(hours: Int) {
        viewModelScope.launch {
            waterPreferences.setReminderIntervalHours(hours)
            if (reminderState.value.enabled) reminderScheduler.update(true, hours)
        }
    }

    fun showPreviousDay() = selectedDate.update { it.minusDays(1) }

    /** Vorwärts nur bis heute – Einträge in der Zukunft sind nicht erlaubt. */
    fun showNextDay() = selectedDate.update { if (it < today) it.plusDays(1) else it }

    fun goToToday() {
        selectedDate.value = today
    }

    /** Springt zu einem gewählten Tag; Zukunft wird auf heute begrenzt. */
    fun selectDate(date: LocalDate) {
        selectedDate.value = if (date.isAfter(today)) today else date
    }

    /**
     * Merkt Tag (der aktuell angezeigte) und Mahlzeit für den folgenden
     * Hinzufügen-Flow in Suche/Scanner. Wird beim "+"-Tippen aufgerufen,
     * bevor zum Such-Tab navigiert wird.
     */
    fun prepareAdd(mealType: MealType) {
        addEntryContext.set(selectedDate.value.toEpochDay(), mealType)
    }

    fun updateEntry(
        original: DiaryEntry,
        mealType: MealType,
        foodName: String,
        amountGrams: Double,
        caloriesPer100g: Double,
        proteinPer100g: Double,
        carbsPer100g: Double,
        fatPer100g: Double,
        unit: MeasureUnit
    ) {
        viewModelScope.launch {
            repository.updateEntry(
                original = original,
                mealType = mealType,
                foodName = foodName,
                amountGrams = amountGrams,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                unit = unit
            )
        }
    }

    fun deleteEntry(entry: DiaryEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    /**
     * Speichert alle Einträge einer Mahlzeit als wiederverwendbare Vorlage.
     * Die Nährwerte werden aus den gespeicherten Gesamtwerten auf Werte pro
     * 100 g zurückgerechnet (Snapshot), damit die Vorlage unabhängig bleibt.
     */
    fun saveMealAsTemplate(name: String, entries: List<DiaryEntry>) {
        if (name.isBlank() || entries.isEmpty()) return
        viewModelScope.launch {
            val items = entries.map { e ->
                val factor = if (e.amountGrams > 0) e.amountGrams / 100.0 else 1.0
                MealTemplateItem(
                    templateId = 0,
                    name = e.foodName,
                    amountGrams = e.amountGrams,
                    unit = e.unit,
                    caloriesPer100g = e.calories / factor,
                    proteinPer100g = e.protein / factor,
                    carbsPer100g = e.carbs / factor,
                    fatPer100g = e.fat / factor,
                    isBeverage = e.unit == MeasureUnit.MILLILITERS
                )
            }
            mealTemplateRepository.saveTemplate(name.trim(), items)
        }
    }
}
