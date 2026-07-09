package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors
import kotlin.math.roundToInt

/**
 * Formatiert einen Mikronährstoffwert (mg/µg) kompakt: große Werte ohne, kleine
 * mit ein bis zwei Nachkommastellen. Gemeinsam von der Statistik und dem
 * Produkt-Bestätigungsdialog genutzt.
 */
fun formatMicroValue(value: Double): String = when {
    value >= 100.0 -> value.roundToInt().toString()
    value >= 10.0 -> ((value * 10).roundToInt() / 10.0).toString()
    else -> ((value * 100).roundToInt() / 100.0).toString()
}

/** Karte mit der Tages-Mikronährstoffdeckung (% NRV) als Fortschrittsbalken. */
@Composable
fun MicronutrientCard(coverage: List<MicronutrientCoverage>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.statistics_micro_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.statistics_micro_subtitle),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (coverage.isEmpty()) {
                Text(
                    text = stringResource(R.string.statistics_micro_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                coverage.forEach { MicronutrientRow(it) }
                Text(
                    text = stringResource(R.string.statistics_micro_source),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MicronutrientRow(item: MicronutrientCoverage) {
    // Theme-Farben VOR dem Canvas lesen.
    val fillColor = MaterialTheme.fairTrackColors.accentLunch
    val trackColor = MaterialTheme.fairTrackColors.ringTrack
    val fraction = (item.percent / 100f).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(item.type.labelRes),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${formatMicroValue(item.value)} ${stringResource(item.type.unit.labelRes)}" +
                    " · ${item.percent}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        MicronutrientBar(fraction = fraction, fillColor = fillColor, trackColor = trackColor)
    }
}

@Composable
private fun MicronutrientBar(fraction: Float, fillColor: Color, trackColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = radius
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset.Zero,
                size = Size(size.width * fraction, size.height),
                cornerRadius = radius
            )
        }
    }
}

/**
 * Tagesdeckung eines Mikronährstoffs: absoluter Tageswert (in seiner Einheit) und
 * die prozentuale Deckung des EU-NRV (kann >100 % sein).
 */
data class MicronutrientCoverage(
    val type: com.fairtrack.app.data.MicronutrientType,
    val value: Double,
    val percent: Int
)
