package com.fairtrack.app.ui

import androidx.compose.runtime.compositionLocalOf
import com.fairtrack.app.data.UnitSystem

/**
 * Stellt das aktive [UnitSystem] für die gesamte UI bereit (v0.11.0). Wird in
 * [FairTrackApp] aus [com.fairtrack.app.data.AppPreferencesRepository] gesammelt
 * und geprovidet; Anzeigestellen lesen es über `LocalUnitSystem.current`.
 */
val LocalUnitSystem = compositionLocalOf { UnitSystem.METRIC }
