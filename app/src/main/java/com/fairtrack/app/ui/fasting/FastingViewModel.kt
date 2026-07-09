package com.fairtrack.app.ui.fasting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fairtrack.app.data.FastingPreferencesRepository
import com.fairtrack.app.data.FastingProtocol
import com.fairtrack.app.data.FastingRepository
import com.fairtrack.app.data.entity.FastingSession
import com.fairtrack.app.work.FastingOngoingNotifier
import com.fairtrack.app.work.FastingReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** UI-Zustand des Fasten-Screens (v0.13.0). */
data class FastingUiState(
    val protocol: FastingProtocol = FastingProtocol.DEFAULT,
    val startMillis: Long = 0L,
    val targetHours: Int = FastingProtocol.DEFAULT.fastingHours,
    val reminderEnabled: Boolean = false,
    val completedCount: Int = 0,
    val recent: List<FastingSession> = emptyList()
) {
    /** Ob gerade ein Fasten läuft. */
    val isRunning: Boolean get() = startMillis > 0L

    /** Zielzeitpunkt (Fastenende) in Millis. */
    val targetEndMillis: Long get() = startMillis + targetHours * MILLIS_PER_HOUR
}

/** Millisekunden pro Stunde – für Zielzeit/Restzeit-Berechnung. */
const val MILLIS_PER_HOUR = 3_600_000L

@HiltViewModel
class FastingViewModel @Inject constructor(
    private val preferences: FastingPreferencesRepository,
    private val repository: FastingRepository,
    private val reminderScheduler: FastingReminderScheduler,
    private val ongoingNotifier: FastingOngoingNotifier
) : ViewModel() {

    private data class Running(
        val protocol: FastingProtocol,
        val startMillis: Long,
        val targetHours: Int,
        val reminderEnabled: Boolean
    )

    val state: StateFlow<FastingUiState> = combine(
        combine(
            preferences.protocol,
            preferences.startMillis,
            preferences.targetHours,
            preferences.reminderEnabled
        ) { protocol, start, target, reminder -> Running(protocol, start, target, reminder) },
        repository.observeCount(),
        repository.observeRecent()
    ) { running, count, recent ->
        FastingUiState(
            protocol = running.protocol,
            startMillis = running.startMillis,
            targetHours = running.targetHours,
            reminderEnabled = running.reminderEnabled,
            completedCount = count,
            recent = recent
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = FastingUiState()
    )

    /** Wählt ein Preset (nur außerhalb eines laufenden Fastens sinnvoll). */
    fun selectProtocol(protocol: FastingProtocol) {
        viewModelScope.launch { preferences.setProtocol(protocol) }
    }

    /** Startet ein Fasten mit dem aktuell gewählten Preset. */
    fun startFast() {
        viewModelScope.launch {
            val protocol = preferences.protocol.first()
            // Tagebasierte Methoden (5:2) haben keinen Stunden-Countdown.
            if (protocol.isDayBased) return@launch
            val now = System.currentTimeMillis()
            preferences.startFast(now, protocol.fastingHours)
            ongoingNotifier.show(now, protocol.fastingHours, protocol)
            if (preferences.reminderEnabled.first()) {
                reminderScheduler.schedule(protocol.fastingHours * MILLIS_PER_HOUR)
            }
        }
    }

    /** Beendet das laufende Fasten und legt eine Verlaufs-Zeile an. */
    fun stopFast() {
        viewModelScope.launch {
            val start = preferences.startMillis.first()
            if (start <= 0L) return@launch
            val target = preferences.targetHours.first()
            val protocol = preferences.protocol.first()
            reminderScheduler.cancel()
            repository.record(
                startEpochMillis = start,
                endEpochMillis = System.currentTimeMillis(),
                targetHours = target,
                presetName = protocol.name
            )
            preferences.stopFast()
            ongoingNotifier.hide()
        }
    }

    /** Schaltet die Fastenende-Erinnerung ein/aus und plant den Worker ggf. neu. */
    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReminderEnabled(enabled)
            val start = preferences.startMillis.first()
            if (enabled && start > 0L) {
                val target = preferences.targetHours.first()
                val remaining = start + target * MILLIS_PER_HOUR - System.currentTimeMillis()
                reminderScheduler.schedule(remaining)
            } else {
                reminderScheduler.cancel()
            }
        }
    }
}
