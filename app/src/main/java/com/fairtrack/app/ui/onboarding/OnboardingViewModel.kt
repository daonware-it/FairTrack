package com.fairtrack.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.UserProfile
import com.fairtrack.app.data.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel für das Onboarding (WP-2). Erfasst das Nutzerprofil für die
 * persönliche TDEE-/Makro-Berechnung und persistiert es via
 * [UserProfileRepository]. Signalisiert den Abschluss Channel-basiert, damit
 * die Navigation den Screen genau einmal verlassen kann.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: UserProfileRepository
) : ViewModel() {

    /** Vorhandenes Profil für den Prefill im Edit-Modus (oder `null` beim Erststart). */
    val existingProfile: StateFlow<UserProfile?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _finished = Channel<Unit>(Channel.BUFFERED)
    val finished = _finished.receiveAsFlow()

    /** Persistiert das Profil und signalisiert Abschluss. */
    fun save(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            _finished.send(Unit)
        }
    }
}
