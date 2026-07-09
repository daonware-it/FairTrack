package com.fairtrack.app.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.DiaryRepository
import com.fairtrack.app.data.SelectedDayCoordinator
import com.fairtrack.app.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class DiaryViewModel @Inject constructor(
    diaryRepository: DiaryRepository,
    userProfileRepository: UserProfileRepository,
    private val selectedDayCoordinator: SelectedDayCoordinator
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState: StateFlow<DiaryUiState> =
        combine(
            diaryRepository.observeDailyNutritionSince(today.minusDays(HISTORY_DAYS).toEpochDay()),
            // Bewusst das Basis-Ziel aus dem Profil, nicht activityAdjustedGoals:
            // der Bewegungs-Aufschlag ist tagesabhängig und müsste für jeden Tag
            // des Verlaufs einzeln abgefragt werden. Der Verlauf zeigt daher das
            // Profil-Ziel; die Tagesübersicht zeigt weiterhin das angepasste.
            userProfileRepository.goals
        ) { nutrition, goals ->
            DiaryUiState.from(nutrition, goals, today)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DiaryUiState()
        )

    /** Bittet die Tagesübersicht, diesen Tag zu zeigen (der Screen wechselt dorthin). */
    fun openDay(date: LocalDate) {
        selectedDayCoordinator.request(date)
    }

    private companion object {
        /** Verlaufstiefe. Genug für einen Quartalsrückblick, ohne die Liste zu sprengen. */
        const val HISTORY_DAYS = 90L
    }
}
