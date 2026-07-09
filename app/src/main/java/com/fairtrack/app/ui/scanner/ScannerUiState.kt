package com.fairtrack.app.ui.scanner

import androidx.annotation.StringRes
import com.fairtrack.app.data.entity.FoodItem

/**
 * Zustände des Barcode-Scanners (v0.3.0). Steuert, ob die Kamera aktiv ist,
 * ein Produkt nachgeschlagen wird, der Bestätigungs-Dialog erscheint, eine
 * manuelle Eingabe nötig ist oder ein Fehler angezeigt wird.
 */
sealed interface ScannerUiState {
    /** Kamera aktiv, wartet auf einen erkannten Barcode. */
    data object Scanning : ScannerUiState

    /** Barcode erkannt, Produkt wird nachgeschlagen. */
    data object LookingUp : ScannerUiState

    /** Produkt gefunden – Nutzer bestätigt/ergänzt die Werte. */
    data class Confirm(val item: FoodItem) : ScannerUiState

    /** Produkt nicht gefunden – manuelle Eingabe mit bekanntem Barcode. */
    data class ManualEntry(val barcode: String) : ScannerUiState

    /** Fehler (z. B. keine Verbindung). */
    data class Error(@StringRes val messageRes: Int) : ScannerUiState
}
