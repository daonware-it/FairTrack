package com.fairtrack.app.ui.fasting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.FastingProtocol
import com.fairtrack.app.data.entity.FastingSession
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Fasten-Timer (v0.13.0): selbstgezeichneter Fortschrittsring mit HH:MM:SS-
 * Countdown, Preset-Auswahl, Erklärtext, Fastenende-Erinnerung und Verlauf.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastingScreen(
    onClose: () -> Unit,
    viewModel: FastingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Ab Android 13 ist eine Laufzeit-Berechtigung nötig, bevor Notifications erscheinen.
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.setReminderEnabled(true) }

    fun toggleReminder(enabled: Boolean) {
        if (!enabled) {
            viewModel.setReminderEnabled(false)
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
            viewModel.setReminderEnabled(true)
        }
    }

    // Live-Tick: aktualisiert die "jetzt"-Zeit sekündlich, solange ein Fasten läuft.
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isRunning) {
        while (state.isRunning) {
            nowMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.fasting_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            item {
                TimerCard(
                    state = state,
                    nowMillis = nowMillis,
                    onStart = viewModel::startFast,
                    onStop = viewModel::stopFast
                )
            }
            item {
                PresetCard(
                    selected = state.protocol,
                    running = state.isRunning,
                    onSelect = viewModel::selectProtocol
                )
            }
            item {
                ReminderRow(
                    enabled = state.reminderEnabled,
                    onToggle = { toggleReminder(it) }
                )
            }
            if (state.recent.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.fasting_history_title, state.completedCount),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs)
                    )
                }
                items(state.recent, key = { it.id }) { session ->
                    HistoryRow(session)
                }
            }
        }
    }
}

@Composable
private fun TimerCard(
    state: FastingUiState,
    nowMillis: Long,
    onStart: () -> Unit,
    onStop: () -> Unit
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            if (state.protocol.isDayBased) {
                // 5:2 ist tagebasiert – kein Stunden-Countdown, nur ein Hinweis.
                DayBasedHint()
                return@Column
            }

            val targetMillis = state.targetHours * 3_600_000L
            val elapsed = if (state.isRunning) (nowMillis - state.startMillis).coerceAtLeast(0L) else 0L
            val fraction = if (targetMillis > 0) (elapsed.toFloat() / targetMillis).coerceIn(0f, 1f) else 0f
            val goalReached = state.isRunning && elapsed >= targetMillis

            FastingRing(
                fraction = fraction,
                elapsedLabel = formatDuration(elapsed),
                targetLabel = stringResource(R.string.fasting_target_hours, state.targetHours),
                goalReached = goalReached,
                running = state.isRunning,
                modifier = Modifier.size(220.dp)
            )

            if (state.isRunning) {
                val zone = ZoneId.systemDefault()
                Text(
                    text = stringResource(
                        R.string.fasting_window_range,
                        TIME_FORMAT.format(Instant.ofEpochMilli(state.startMillis).atZone(zone)),
                        TIME_FORMAT.format(Instant.ofEpochMilli(state.targetEndMillis).atZone(zone))
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = { if (state.isRunning) onStop() else onStart() },
                modifier = Modifier.fillMaxWidth(),
                colors = if (state.isRunning) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    stringResource(
                        if (state.isRunning) R.string.fasting_stop else R.string.fasting_start
                    )
                )
            }
        }
    }
}

@Composable
private fun DayBasedHint() {
    Icon(
        imageVector = Icons.Rounded.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(48.dp)
    )
    Text(
        text = stringResource(R.string.fasting_day_based_hint),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun FastingRing(
    fraction: Float,
    elapsedLabel: String,
    targetLabel: String,
    goalReached: Boolean,
    running: Boolean,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.fairTrackColors.ringTrack
    // Blau-Akzent während des Fastens, Grün-Akzent (Ziel erreicht) als Farbwechsel.
    val activeColor = MaterialTheme.fairTrackColors.fat
    val reachedColor = MaterialTheme.fairTrackColors.accentLunch
    val arcColor = if (goalReached) reachedColor else activeColor

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
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
                    color = arcColor,
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
                text = elapsedLabel,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = when {
                    goalReached -> stringResource(R.string.fasting_goal_reached)
                    running -> targetLabel
                    else -> stringResource(R.string.fasting_idle)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (goalReached) {
                    MaterialTheme.fairTrackColors.accentLunch
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PresetCard(
    selected: FastingProtocol,
    running: Boolean,
    onSelect: (FastingProtocol) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.fasting_preset_title),
                style = MaterialTheme.typography.titleMedium
            )
            // FilterChip-Reihe (umbrechend) über alle Presets.
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                FastingProtocol.entries.chunked(3).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        rowItems.forEach { protocol ->
                            FilterChip(
                                selected = protocol == selected,
                                // Preset-Wechsel nur außerhalb eines laufenden Fastens.
                                enabled = !running,
                                onClick = { onSelect(protocol) },
                                label = { Text(stringResource(protocol.labelRes)) }
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(selected.descriptionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (running) {
                Text(
                    text = stringResource(R.string.fasting_preset_locked),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReminderRow(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fasting_reminder_label),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = stringResource(R.string.fasting_reminder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun HistoryRow(session: FastingSession) {
    val zone = ZoneId.systemDefault()
    val duration = (session.endEpochMillis - session.startEpochMillis).coerceAtLeast(0L)
    val reached = duration >= session.targetHours * 3_600_000L
    val protocol = FastingProtocol.fromNameOrDefault(session.presetName)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.fasting_history_entry,
                        stringResource(protocol.labelRes),
                        formatDuration(duration)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = DATE_FORMAT.format(Instant.ofEpochMilli(session.endEpochMillis).atZone(zone)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (reached) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.fasting_goal_reached),
                    tint = MaterialTheme.fairTrackColors.accentLunch,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/** Formatiert eine Dauer in Millis als HH:MM:SS. */
private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
