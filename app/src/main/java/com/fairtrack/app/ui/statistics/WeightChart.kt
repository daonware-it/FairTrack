package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fairtrack.app.data.entity.WeightEntry

/**
 * Selbstgezeichnetes Liniendiagramm für den Gewichtsverlauf. X-Achse = Reihenfolge
 * der Einträge (index-basiert, gleichmäßig verteilt), Y-Achse = Gewicht in kg.
 * Optional eine gestrichelte Ziel-Linie. Bei < 2 Einträgen wird nichts gezeichnet.
 */
@Composable
fun WeightChart(
    entries: List<WeightEntry>,
    targetWeightKg: Double?,
    modifier: Modifier = Modifier
) {
    // Farben im Composable-Kontext lesen; der Canvas-DrawScope hat keinen Zugriff darauf.
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary
    val targetColor = MaterialTheme.colorScheme.tertiary

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (entries.size < 2) return@Canvas

        val horizontalPadding = 12f
        val verticalPadding = 24f
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2

        val weights = entries.map { it.weightKg }
        var minWeight = weights.min()
        var maxWeight = weights.max()

        // Ziel-Linie ggf. in den Wertebereich einbeziehen, damit sie sichtbar ist.
        if (targetWeightKg != null) {
            minWeight = minOf(minWeight, targetWeightKg)
            maxWeight = maxOf(maxWeight, targetWeightKg)
        }

        // Etwas Padding ober-/unterhalb; bei identischen Werten künstlichen Bereich schaffen.
        val rawRange = maxWeight - minWeight
        val range = if (rawRange <= 0.0) 1.0 else rawRange
        val paddedMin = minWeight - range * 0.1
        val paddedMax = maxWeight + range * 0.1
        val paddedRange = paddedMax - paddedMin

        fun xFor(index: Int): Float =
            horizontalPadding + chartWidth * index / (entries.size - 1)

        fun yFor(weight: Double): Float {
            val fraction = ((weight - paddedMin) / paddedRange).toFloat()
            return verticalPadding + chartHeight * (1f - fraction)
        }

        // Ziel-Linie (gestrichelt), nur wenn im dargestellten Bereich.
        if (targetWeightKg != null && targetWeightKg in paddedMin..paddedMax) {
            val y = yFor(targetWeightKg)
            drawLine(
                color = targetColor,
                start = Offset(horizontalPadding, y),
                end = Offset(size.width - horizontalPadding, y),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        }

        // Linienzug durch alle Datenpunkte.
        val path = Path()
        entries.forEachIndexed { index, entry ->
            val x = xFor(index)
            val y = yFor(entry.weightKg)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Datenpunkte als kleine Kreise.
        entries.forEachIndexed { index, entry ->
            drawCircle(
                color = pointColor,
                radius = 5f,
                center = Offset(xFor(index), yFor(entry.weightKg))
            )
        }
    }
}
