package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors

/**
 * Selbstgezeichneter Donut/Ring der Makro-Verteilung (Protein/Kohlenhydrate/
 * Fett). Segmente werden nach ihrem Kalorien-Beitrag (Protein 4, Carbs 4,
 * Fett 9 kcal/g) gewichtet und in den [fairTrackColors]-Makrofarben gezeichnet.
 * Rechts eine Legende mit Gramm und Prozent.
 */
@Composable
fun MacroDonut(
    macros: MacroBreakdown,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.fairTrackColors
    val proteinColor = colors.protein
    val carbsColor = colors.carbs
    val fatColor = colors.fat
    val trackColor = colors.ringTrack

    val segments = listOf(
        Triple(proteinColor, macros.proteinKcal, macros.proteinPercent),
        Triple(carbsColor, macros.carbsKcal, macros.carbsPercent),
        Triple(fatColor, macros.fatKcal, macros.fatPercent)
    )
    val total = macros.totalKcal

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
    ) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(132.dp)) {
                val stroke = 22f
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)

                // Track als voller Ring.
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke)
                )

                if (total > 0) {
                    var startAngle = -90f
                    segments.forEach { (color, kcal, _) ->
                        val sweep = kcal.toFloat() / total * 360f
                        if (sweep > 0f) {
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = stroke)
                            )
                            startAngle += sweep
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.statistics_kcal, total),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.statistics_macro_total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MacroLegendRow(
                color = proteinColor,
                label = stringResource(R.string.statistics_macro_protein),
                grams = macros.proteinGrams,
                percent = macros.proteinPercent
            )
            MacroLegendRow(
                color = carbsColor,
                label = stringResource(R.string.statistics_macro_carbs),
                grams = macros.carbsGrams,
                percent = macros.carbsPercent
            )
            MacroLegendRow(
                color = fatColor,
                label = stringResource(R.string.statistics_macro_fat),
                grams = macros.fatGrams,
                percent = macros.fatPercent
            )
        }
    }
}

@Composable
private fun MacroLegendRow(
    color: Color,
    label: String,
    grams: Int,
    percent: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .padding(end = Spacing.sm)
                .size(12.dp)
                .background(color = color, shape = CircleShape)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.statistics_macro_value, grams, percent),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
