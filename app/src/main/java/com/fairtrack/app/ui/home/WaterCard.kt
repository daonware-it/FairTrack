package com.fairtrack.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.Dimens
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors

/** Schnell-Buttons – Menge in ml, die pro Tap hinzugefügt wird. */
private val QUICK_AMOUNTS = listOf(150, 250, 500)

/** Auswählbare Erinnerungs-Intervalle in Stunden. */
private val REMINDER_INTERVALS = listOf(1, 2, 3, 4)

/**
 * Wasser-Karte auf dem Home-Screen (v0.7.0): Fortschrittsring, Schnell-Buttons,
 * Tagesziel-Bearbeitung und optionale Trink-Erinnerung.
 */
@Composable
fun WaterCard(
    water: WaterUiState,
    reminder: WaterReminderState,
    onAdd: (Int) -> Unit,
    onReset: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderIntervalChange: (Int) -> Unit
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Ab Android 13 ist eine Laufzeit-Berechtigung nötig, bevor Notifications erscheinen.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) onReminderEnabledChange(true) }

    fun toggleReminder(enabled: Boolean) {
        if (!enabled) {
            onReminderEnabledChange(false)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onReminderEnabledChange(true)
        }
    }

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
                Text(
                    text = stringResource(R.string.water_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onReset) {
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.water_reset)
                    )
                }
                IconButton(onClick = { showGoalDialog = true }) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.water_edit_goal)
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                WaterRing(
                    consumedMl = water.consumedMl,
                    goalMl = water.goalMl,
                    fraction = water.fraction,
                    modifier = Modifier.size(Dimens.waterRingSize)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                QUICK_AMOUNTS.forEach { amount ->
                    OutlinedButton(
                        onClick = { onAdd(amount) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.water_add_amount, amount))
                    }
                }
            }

            // Optionale Trink-Erinnerung.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.water_reminder_label),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { toggleReminder(it) }
                )
            }
            if (reminder.enabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    REMINDER_INTERVALS.forEach { hours ->
                        FilterChip(
                            selected = reminder.intervalHours == hours,
                            onClick = { onReminderIntervalChange(hours) },
                            label = { Text(stringResource(R.string.water_reminder_hours, hours)) }
                        )
                    }
                }
            }
        }
    }

    if (showGoalDialog) {
        WaterGoalDialog(
            currentGoalMl = water.goalMl,
            onDismiss = { showGoalDialog = false },
            onConfirm = { goal ->
                onSetGoal(goal)
                showGoalDialog = false
            }
        )
    }
}

@Composable
private fun WaterRing(
    consumedMl: Int,
    goalMl: Int,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.fairTrackColors.ringTrack
    // Blau-Akzent (Fett-Makro) als Wasser-Farbe – keine neue Farbe erfunden.
    val waterColor = MaterialTheme.fairTrackColors.fat

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Dimens.waterRingStroke.toPx()
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
                    color = waterColor,
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
                text = stringResource(R.string.water_amount_of_goal, consumedMl, goalMl),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.water_unit_ml),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Dialog zum Einstellen des Wasser-Tagesziels in ml. */
@Composable
private fun WaterGoalDialog(
    currentGoalMl: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf(currentGoalMl.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.water_goal_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { new -> text = new.filter { it.isDigit() }.take(5) },
                label = { Text(stringResource(R.string.water_goal_dialog_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { parsed?.let(onConfirm) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
