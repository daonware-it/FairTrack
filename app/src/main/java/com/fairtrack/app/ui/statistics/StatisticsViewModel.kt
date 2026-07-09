package com.fairtrack.app.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.R
import com.fairtrack.app.data.ActivityPreferencesRepository
import com.fairtrack.app.data.ActivityRepository
import com.fairtrack.app.data.BodyMeasurementRepository
import com.fairtrack.app.data.DailyMicronutrients
import com.fairtrack.app.data.DailyNutrition
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.MicronutrientType
import com.fairtrack.app.data.NutritionGoals
import com.fairtrack.app.data.UserProfileRepository
import com.fairtrack.app.data.WeightRepository
import com.fairtrack.app.data.activity.activityAdjustedGoals
import com.fairtrack.app.data.entity.BodyMeasurement
import com.fairtrack.app.data.entity.WeightEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val weightRepository: WeightRepository,
    private val userProfileRepository: UserProfileRepository,
    private val bodyMeasurementRepository: BodyMeasurementRepository,
    private val activityRepository: ActivityRepository,
    private val activityPreferences: ActivityPreferencesRepository,
    diaryRepository: DiaryRepository
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    // Kalorienziel inkl. optionalem Bewegungs-Aufschlag für HEUTE – dasselbe
    // Budget wie Home-Screen und Widget (Konsistenz).
    private val adjustedGoals =
        activityAdjustedGoals(
            userProfileRepository.goals,
            activityRepository,
            activityPreferences,
            today.toEpochDay()
        )

    // Genug Historie für die längste Auswertung (Streak). Weekly/Monthly leiten
    // sich aus derselben Aggregat-Liste ab.
    private val nutritionSince =
        diaryRepository.observeDailyNutritionSince(today.minusDays(STREAK_WINDOW_DAYS - 1).toEpochDay())

    // Mikronährstoff-Deckung bezieht sich auf HEUTE -> nur der heutige Tag.
    private val microsToday =
        diaryRepository.observeDailyMicronutrientsSince(today.toEpochDay())

    private val baseState =
        combine(
            weightRepository.history,
            userProfileRepository.profile,
            adjustedGoals,
            nutritionSince,
            microsToday
        ) { history, profile, goals, daily, micros ->
            buildState(history, profile?.targetWeightKg, profile?.weightKg, goals, daily, micros)
        }

    val uiState: StateFlow<StatisticsUiState> =
        combine(baseState, bodyMeasurementRepository.history) { state, body ->
            state.copy(bodyHistory = body)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState()
        )

    private fun buildState(
        history: List<WeightEntry>,
        targetWeightKg: Double?,
        profileWeightKg: Double?,
        goals: NutritionGoals,
        daily: List<DailyNutrition>,
        micros: List<DailyMicronutrients>
    ): StatisticsUiState {
        val byDay: Map<Long, DailyNutrition> = daily.associateBy { it.epochDay }
        val todayEpoch = today.toEpochDay()

        // 1) Wochenverlauf: die letzten 7 Kalendertage inkl. heute, fehlende = 0.
        val week = (WEEK_DAYS - 1 downTo 0).map { back ->
            val date = today.minusDays(back.toLong())
            val epoch = date.toEpochDay()
            DayCalories(
                epochDay = epoch,
                dayOfWeekValue = date.dayOfWeek.value,
                calories = byDay[epoch]?.calories?.roundToInt() ?: 0
            )
        }

        // 2) Monatsschnitt: nur Kalendertage mit Einträgen in den letzten 30 Tagen
        //    (Tage ohne Logging sollen den Schnitt nicht künstlich drücken).
        val monthFrom = todayEpoch - (MONTH_DAYS - 1)
        val loggedDays = daily.filter { it.epochDay in monthFrom..todayEpoch }
        val monthlyAverage =
            if (loggedDays.isEmpty()) 0
            else (loggedDays.sumOf { it.calories } / loggedDays.size).roundToInt()

        // 3) Makro-Donut: summierte Makros der letzten 7 Tage.
        val weekFrom = todayEpoch - (WEEK_DAYS - 1)
        val weekDaily = daily.filter { it.epochDay in weekFrom..todayEpoch }
        val macros = MacroBreakdown(
            proteinGrams = weekDaily.sumOf { it.protein }.roundToInt(),
            carbsGrams = weekDaily.sumOf { it.carbs }.roundToInt(),
            fatGrams = weekDaily.sumOf { it.fat }.roundToInt()
        )

        // 4) Streak: aufeinanderfolgende Tage mit Eintrag, rückwärts ab heute.
        //    Regel: heute ohne Eintrag bricht die Serie nicht, sofern gestern
        //    einer da war (heute ist optional); der erste Lücken-Tag beendet sie.
        val activeDays = daily.map { it.epochDay }.toHashSet()
        val streak = computeStreak(todayEpoch, activeDays)

        // 5) Mikronährstoff-Deckung (heute) ggü. EU-NRV.
        val todayMicros = micros.firstOrNull { it.epochDay == todayEpoch }?.toMicronutrients()
        val microCoverage = if (todayMicros == null) {
            emptyList()
        } else {
            MicronutrientType.entries.mapNotNull { type ->
                val value = type.selector(todayMicros)
                if (value == null || value <= 0.0) {
                    null
                } else {
                    MicronutrientCoverage(
                        type = type,
                        value = value,
                        percent = (value / type.nrv * 100).roundToInt()
                    )
                }
            }
        }

        return StatisticsUiState(
            history = history,
            targetWeightKg = targetWeightKg,
            currentWeightKg = history.lastOrNull()?.weightKg ?: profileWeightKg,
            calorieGoal = goals.calories,
            weeklyCalories = week,
            monthlyAverage = monthlyAverage,
            monthlyLoggedDays = loggedDays.size,
            macros = macros,
            streakDays = streak,
            microCoverage = microCoverage
        )
    }

    private fun computeStreak(todayEpoch: Long, activeDays: Set<Long>): Int {
        // Startpunkt bestimmen: heute, wenn heute geloggt, sonst gestern.
        var day = when {
            activeDays.contains(todayEpoch) -> todayEpoch
            activeDays.contains(todayEpoch - 1) -> todayEpoch - 1
            else -> return 0
        }
        var count = 0
        while (activeDays.contains(day)) {
            count++
            day--
        }
        return count
    }

    fun logWeight(weightKg: Double) {
        viewModelScope.launch {
            val epochDay = LocalDate.now().toEpochDay()
            weightRepository.logWeight(epochDay, weightKg)
            userProfileRepository.updateCurrentWeight(weightKg)
        }
    }

    fun deleteEntry(entry: WeightEntry) {
        viewModelScope.launch { weightRepository.delete(entry) }
    }

    /**
     * Speichert die für heute eingegebenen Körpermaße (bereits metrisch, cm bzw. %).
     * Nur nicht-null Werte werden gesetzt; andere Maße des Tages bleiben erhalten.
     */
    fun saveMeasurements(
        waistCm: Double?,
        bodyFatPercent: Double?,
        chestCm: Double?,
        armCm: Double?
    ) {
        viewModelScope.launch {
            bodyMeasurementRepository.saveMeasurements(
                epochDay = LocalDate.now().toEpochDay(),
                waistCm = waistCm,
                bodyFatPercent = bodyFatPercent,
                chestCm = chestCm,
                armCm = armCm
            )
        }
    }

    fun deleteMeasurement(entry: BodyMeasurement) {
        viewModelScope.launch { bodyMeasurementRepository.delete(entry) }
    }

    private companion object {
        const val WEEK_DAYS = 7
        const val MONTH_DAYS = 30
        const val STREAK_WINDOW_DAYS = 60L
    }
}

/** Kalorien eines Kalendertages für den Wochen-Chart. */
data class DayCalories(
    val epochDay: Long,
    /** 1 = Montag … 7 = Sonntag (für das Wochentags-Kürzel). */
    val dayOfWeekValue: Int,
    val calories: Int
)

/**
 * Summierte Makros über einen Zeitraum. Prozente werden aus den Kalorien-
 * beiträgen berechnet (Protein 4 kcal/g, Kohlenhydrate 4, Fett 9).
 */
data class MacroBreakdown(
    val proteinGrams: Int = 0,
    val carbsGrams: Int = 0,
    val fatGrams: Int = 0
) {
    val proteinKcal: Int get() = proteinGrams * 4
    val carbsKcal: Int get() = carbsGrams * 4
    val fatKcal: Int get() = fatGrams * 9
    val totalKcal: Int get() = proteinKcal + carbsKcal + fatKcal
    val isEmpty: Boolean get() = totalKcal <= 0

    private fun percentOf(kcal: Int): Int =
        if (totalKcal > 0) (kcal.toFloat() / totalKcal * 100).roundToInt() else 0

    val proteinPercent: Int get() = percentOf(proteinKcal)
    val carbsPercent: Int get() = percentOf(carbsKcal)
    val fatPercent: Int get() = percentOf(fatKcal)
}

data class StatisticsUiState(
    val history: List<WeightEntry> = emptyList(),
    val targetWeightKg: Double? = null,
    val currentWeightKg: Double? = null,
    val calorieGoal: Int = 0,
    val weeklyCalories: List<DayCalories> = emptyList(),
    val monthlyAverage: Int = 0,
    val monthlyLoggedDays: Int = 0,
    val macros: MacroBreakdown = MacroBreakdown(),
    val streakDays: Int = 0,
    val microCoverage: List<MicronutrientCoverage> = emptyList(),
    val bodyHistory: List<BodyMeasurement> = emptyList()
)

/**
 * Auswählbare Körpermaße für Eingabe und Verlaufschart. [isLength] unterscheidet
 * Längenmaße (cm/inch-Umrechnung) von einheitenlosen Prozentwerten (Körperfett).
 * [selector] liest den metrisch gespeicherten Wert (cm bzw. %) aus einem Eintrag.
 */
enum class BodyMeasurementType(
    val labelRes: Int,
    val isLength: Boolean,
    val selector: (BodyMeasurement) -> Double?
) {
    WAIST(R.string.body_measure_waist, true, { it.waistCm }),
    BODY_FAT(R.string.body_measure_bodyfat, false, { it.bodyFatPercent }),
    CHEST(R.string.body_measure_chest, true, { it.chestCm }),
    ARM(R.string.body_measure_arm, true, { it.armCm })
}
