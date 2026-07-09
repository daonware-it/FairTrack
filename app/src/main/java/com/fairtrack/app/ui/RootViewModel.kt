package com.fairtrack.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.AppPreferencesRepository
import com.fairtrack.app.data.ThemeMode
import com.fairtrack.app.data.UnitSystem
import com.fairtrack.app.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Wurzel-ViewModel: liefert den Onboarding-Status für das Start-Gating sowie die
 * app-weiten UI-Präferenzen (Theme, Material You, Maßsystem), die [MainActivity]
 * und [FairTrackApp] gemeinsam nutzen.
 * `onboardingComplete`: `null` = Status wird noch geladen, danach `true`/`false`.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    repository: UserProfileRepository,
    appPreferences: AppPreferencesRepository
) : ViewModel() {

    val onboardingComplete: StateFlow<Boolean?> = repository.isOnboardingComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val themeMode: StateFlow<ThemeMode> = appPreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.AUTO)

    val dynamicColor: StateFlow<Boolean> = appPreferences.dynamicColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val unitSystem: StateFlow<UnitSystem> = appPreferences.unitSystem
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UnitSystem.METRIC)
}
