package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fairtrack.app.ui.theme.Spacing
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * Selbstgezeichnetes Balkendiagramm der Tages-Kalorien der letzten 7 Tage.
 * Optional eine gestrichelte Kalorien-Ziellinie (analog zur Ziel-Linie im
 * [WeightChart]). Wochentags-Kürzel stehen als ausgerichtete Labels darunter.
 */
@Composable
fun CalorieWeekChart(
    days: List<DayCalories>,
    calorieGoal: Int,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val goalColor = MaterialTheme.colorScheme.tertiary

    // Skala: höchster Wert aus Tageswerten und Ziel, mit etwas Kopfraum.
    val maxValue = maxOf(
        days.maxOfOrNull { it.calories } ?: 0,
        calorieGoal,
        1
    ) * 1.1f

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            if (days.isEmpty()) return@Canvas

            val verticalPadding = 8f
            val chartHeight = size.height - verticalPadding * 2
            val slotWidth = size.width / days.size
            val barWidth = slotWidth * 0.5f

            fun yFor(value: Float): Float =
                verticalPadding + chartHeight * (1f - (value / maxValue).coerceIn(0f, 1f))

            days.forEachIndexed { index, day ->
                val centerX = slotWidth * index + slotWidth / 2f
                val top = yFor(day.calories.toFloat())
                val bottom = verticalPadding + chartHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2f, top),
                    size = Size(barWidth, (bottom - top).coerceAtLeast(0f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }

            // Ziel-Linie (gestrichelt), nur wenn im dargestellten Bereich.
            if (calorieGoal > 0 && calorieGoal <= maxValue) {
                val y = yFor(calorieGoal.toFloat())
                drawLine(
                    color = goalColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            days.forEach { day ->
                Text(
                    text = DayOfWeek.of(day.dayOfWeekValue)
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
