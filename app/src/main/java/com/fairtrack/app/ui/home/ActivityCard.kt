package com.fairtrack.app.ui.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fairtrack.app.R
import com.fairtrack.app.data.activity.HealthConnectPermissions
import com.fairtrack.app.ui.theme.Dimens
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors

/** Schritt-Tagesziel für den Fortschrittsring (fester Richtwert). */
private const val STEP_GOAL = 10_000

/** Health-Connect-Paket für den Play-Store-Verweis, falls nicht installiert. */
private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

/**
 * Home-Card für Bewegungsdaten (v0.14.0): Schritte als selbst gezeichneter
 * Fortschrittsring plus Aktivkalorien. Blendet sich je nach Zustand um –
 * Health Connect nicht verfügbar, noch nicht verbunden, oder mit Daten.
 */
@Composable
fun ActivityCard(
    state: ActivityUiState,
    onPermissionResult: () -> Unit
) {
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
                .padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsWalk,
                    contentDescription = null,
                    tint = MaterialTheme.fairTrackColors.accentLunch
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    text = stringResource(R.string.activity_title),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            when {
                !state.available -> UnavailableContent()
                !state.hasPermission -> ConnectContent(onPermissionResult = onPermissionResult)
                else -> ActivityDataContent(state)
            }
        }
    }
}

@Composable
private fun UnavailableContent() {
    val context = LocalContext.current
    Text(
        text = stringResource(R.string.activity_unavailable_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(
        onClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
            )
            try {
                context.startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$HEALTH_CONNECT_PACKAGE")
                    )
                )
            }
        }
    ) {
        Text(stringResource(R.string.activity_install_health_connect))
    }
}

@Composable
private fun ConnectContent(onPermissionResult: () -> Unit) {
    // Dieser Zweig wird nur auf Geräten mit installiertem Health Connect (API >= 26)
    // komponiert – daher ist der Health-Connect-Contract hier sicher.
    val launcher = rememberLauncherForActivityResult(
        HealthConnectPermissions.permissionContract()
    ) { _ -> onPermissionResult() }

    Text(
        text = stringResource(R.string.activity_connect_body),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Button(onClick = { launcher.launch(HealthConnectPermissions.ALL) }) {
        Text(stringResource(R.string.activity_connect))
    }
}

@Composable
private fun ActivityDataContent(state: ActivityUiState) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        StepRing(
            steps = state.steps,
            goal = STEP_GOAL,
            modifier = Modifier.size(Dimens.activityRingSize)
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.fairTrackColors.protein,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(
            text = stringResource(R.string.activity_active_kcal, state.activeKcal),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    if (state.addToBudget && state.activeKcal > 0) {
        Text(
            text = stringResource(R.string.activity_budget_added, state.activeKcal),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepRing(
    steps: Int,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val fraction = if (goal > 0) (steps.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val trackColor = MaterialTheme.fairTrackColors.ringTrack
    val stepColor = MaterialTheme.fairTrackColors.accentLunch

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Dimens.activityRingStroke.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            if (fraction > 0f) {
                drawArc(
                    color = stepColor,
                    startAngle = -90f,
                    sweepAngle = 360f * fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$steps",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.activity_steps),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** UI-Zustand der Bewegungs-Card. */
data class ActivityUiState(
    val available: Boolean = false,
    val hasPermission: Boolean = false,
    val steps: Int = 0,
    val activeKcal: Int = 0,
    val addToBudget: Boolean = false
)
