package com.fairtrack.app.ui.theme

import androidx.compose.ui.unit.dp

/** Abstands-Tokens — statt Magic Numbers in den Screens. */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

/** Komponentenspezifische Maße. */
object Dimens {
    val calorieRingSize = 160.dp
    val calorieRingStroke = 16.dp
    val macroBarHeight = 8.dp
    val iconChip = 40.dp
    val waterRingSize = 132.dp
    val waterRingStroke = 14.dp
    val activityRingSize = 132.dp
    val activityRingStroke = 14.dp

    /**
     * Breite eines Bottom-Nav-Labels. Der Item-Slot ist Bildschirmbreite / 5 (~85 dp auf einem
     * 360-dp-Gerät); die Schranke lässt bewusst Rand, damit lange Labels wie "Einstellungen"
     * herunterskalieren statt bündig am Bildschirmrand zu kleben.
     */
    val navItemLabelMaxWidth = 64.dp
}
