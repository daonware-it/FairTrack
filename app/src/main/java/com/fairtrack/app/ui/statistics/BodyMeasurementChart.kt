package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fairtrack.app.data.entity.BodyMeasurement

/**
 * Selbstgezeichnetes Liniendiagramm für ein einzelnes Körpermaß. X-Achse =
 * Reihenfolge der (für dieses Maß gefüllten) Einträge, Y-Achse = Messwert.
 * Nulls des jeweiligen Maßes werden übersprungen; bei < 2 gefüllten Punkten wird
 * nichts gezeichnet. Vorlage: [WeightChart].
 */
@Composable
fun BodyMeasurementChart(
    entries: List<BodyMeasurement>,
    type: BodyMeasurementType,
    modifier: Modifier = Modifier
) {
    // Farben im Composable-Kontext lesen; der Canvas-DrawScope hat keinen Zugriff.
    val lineColor = MaterialTheme.colorScheme.primary
    val pointColor = MaterialTheme.colorScheme.primary

    // Nur Einträge mit einem Wert für das gewählte Maß.
    val values = entries.mapNotNull { type.selector(it) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        if (values.size < 2) return@Canvas

        val horizontalPadding = 12f
        val verticalPadding = 24f
        val chartWidth = size.width - horizontalPadding * 2
        val chartHeight = size.height - verticalPadding * 2

        val minValue = values.min()
        val maxValue = values.max()

        // Etwas Padding ober-/unterhalb; bei identischen Werten künstlichen Bereich schaffen.
        val rawRange = maxValue - minValue
        val range = if (rawRange <= 0.0) 1.0 else rawRange
        val paddedMin = minValue - range * 0.1
        val paddedMax = maxValue + range * 0.1
        val paddedRange = paddedMax - paddedMin

        fun xFor(index: Int): Float =
            horizontalPadding + chartWidth * index / (values.size - 1)

        fun yFor(value: Double): Float {
            val fraction = ((value - paddedMin) / paddedRange).toFloat()
            return verticalPadding + chartHeight * (1f - fraction)
        }

        // Linienzug durch alle Datenpunkte.
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = xFor(index)
            val y = yFor(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Datenpunkte als kleine Kreise.
        values.forEachIndexed { index, value ->
            drawCircle(
                color = pointColor,
                radius = 5f,
                center = Offset(xFor(index), yFor(value))
            )
        }
    }
}
