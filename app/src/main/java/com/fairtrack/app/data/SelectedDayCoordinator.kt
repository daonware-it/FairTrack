package com.fairtrack.app.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Übergibt einen gewünschten Tag über Screen-Grenzen hinweg an die Tagesübersicht.
 * Wird vom Tagebuch-Tab gesetzt, wenn der Nutzer einen Tag im Verlauf antippt, und
 * von der Tagesübersicht gelesen, die daraufhin zu diesem Tag springt.
 *
 * Muster analog [AddEntryContext]: @Singleton mit StateFlow, da beide Screens
 * eigene ViewModels mit eigenem Lebenszyklus haben.
 */
@Singleton
class SelectedDayCoordinator @Inject constructor() {

    private val _requestedDate = MutableStateFlow<LocalDate?>(null)

    /** Angefragter Tag, oder null wenn keine Anfrage offen ist. */
    val requestedDate: StateFlow<LocalDate?> = _requestedDate.asStateFlow()

    /** Bittet die Tagesübersicht, zu diesem Tag zu springen. */
    fun request(date: LocalDate) {
        _requestedDate.value = date
    }

    /**
     * Quittiert die Anfrage. Ohne dieses Zurücksetzen würde die Tagesübersicht
     * bei jeder Neu-Sammlung erneut zum selben Tag springen und den Nutzer daran
     * hindern, von dort weiterzublättern.
     */
    fun consume() {
        _requestedDate.value = null
    }
}
