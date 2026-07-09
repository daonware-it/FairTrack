package com.fairtrack.app.ui.diary

import com.fairtrack.app.data.DailyNutrition
import com.fairtrack.app.data.NutritionGoals
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

/** Ein Tag im Verlauf: Summen des Tages gegen das Tagesziel. */
data class DiaryDayState(
    val date: LocalDate,
    val isToday: Boolean,
    val calories: Int,
    val goalCalories: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int
) {
    /** Anteil am Kalorienziel, für die Balkenbreite auf 0..1 begrenzt. */
    val goalFraction: Float =
        if (goalCalories > 0) (calories.toFloat() / goalCalories).coerceIn(0f, 1f) else 0f

    /** Über dem Ziel — der Balken wird dann in der Warnfarbe gezeichnet. */
    val isOverGoal: Boolean = goalCalories > 0 && calories > goalCalories
}

/** Tage eines Monats, absteigend (neuester Tag zuerst). */
data class DiaryMonthState(
    val yearMonth: YearMonth,
    val days: List<DiaryDayState>
)

data class DiaryUiState(
    val months: List<DiaryMonthState> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean get() = !isLoading && months.isEmpty()

    companion object {
        /**
         * Baut den Verlauf aus den aggregierten Tageswerten.
         *
         * Tage ohne Einträge fehlen in [nutrition] und werden bewusst NICHT
         * aufgefüllt — ein Verlauf aus lauter Null-Tagen wäre unlesbar. Der
         * heutige Tag erscheint dagegen immer, damit der Screen nie leer wirkt,
         * solange die App in Benutzung ist.
         */
        fun from(
            nutrition: List<DailyNutrition>,
            goals: NutritionGoals,
            today: LocalDate
        ): DiaryUiState {
            val byDay = nutrition.associateBy { it.epochDay }
            val days = buildList {
                if (!byDay.containsKey(today.toEpochDay())) {
                    add(emptyDay(today, goals, isToday = true))
                }
                nutrition.forEach { daily ->
                    val date = LocalDate.ofEpochDay(daily.epochDay)
                    add(
                        DiaryDayState(
                            date = date,
                            isToday = date == today,
                            calories = daily.calories.roundToInt(),
                            goalCalories = goals.calories,
                            proteinGrams = daily.protein.roundToInt(),
                            carbsGrams = daily.carbs.roundToInt(),
                            fatGrams = daily.fat.roundToInt()
                        )
                    )
                }
            }.sortedByDescending { it.date }

            val months = days
                .groupBy { YearMonth.from(it.date) }
                .map { (yearMonth, daysOfMonth) -> DiaryMonthState(yearMonth, daysOfMonth) }
                .sortedByDescending { it.yearMonth }

            return DiaryUiState(months = months, isLoading = false)
        }

        private fun emptyDay(date: LocalDate, goals: NutritionGoals, isToday: Boolean) =
            DiaryDayState(
                date = date,
                isToday = isToday,
                calories = 0,
                goalCalories = goals.calories,
                proteinGrams = 0,
                carbsGrams = 0,
                fatGrams = 0
            )
    }
}
