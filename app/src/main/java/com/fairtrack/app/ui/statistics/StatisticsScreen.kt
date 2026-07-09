package com.fairtrack.app.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.UnitFormatter
import com.fairtrack.app.data.entity.BodyMeasurement
import com.fairtrack.app.data.entity.WeightEntry
import com.fairtrack.app.ui.LocalUnitSystem
import com.fairtrack.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // --- Kalorien / Makros / Streak (v0.8.0) ---
        Text(
            text = stringResource(R.string.statistics_calories_title),
            style = MaterialTheme.typography.headlineSmall
        )

        CalorieWeekCard(
            days = state.weeklyCalories,
            calorieGoal = state.calorieGoal
        )

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(
                title = stringResource(R.string.statistics_month_avg_title),
                value = state.monthlyAverage.toString(),
                subtitle = if (state.monthlyLoggedDays > 0) {
                    stringResource(
                        R.string.statistics_month_avg_subtitle,
                        state.monthlyLoggedDays
                    )
                } else {
                    stringResource(R.string.statistics_month_avg_none)
                },
                modifier = Modifier.weight(1f)
            )
            StreakCard(
                streakDays = state.streakDays,
                modifier = Modifier.weight(1f)
            )
        }

        MacroDonutCard(macros = state.macros)

        // --- Mikronährstoffe (v0.10.0) ---
        Text(
            text = stringResource(R.string.statistics_micro_section_title),
            style = MaterialTheme.typography.headlineSmall
        )
        MicronutrientCard(coverage = state.microCoverage)

        // --- Gewichtsverlauf (v0.9.0) ---
        Text(
            text = stringResource(R.string.statistics_weight_title),
            style = MaterialTheme.typography.headlineSmall
        )

        CurrentTargetCard(
            currentWeightKg = state.currentWeightKg,
            targetWeightKg = state.targetWeightKg
        )

        WeightInputCard(onSave = viewModel::logWeight)

        if (state.history.size >= 2) {
            WeightChart(
                entries = state.history,
                targetWeightKg = state.targetWeightKg,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    text = stringResource(R.string.statistics_no_weight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Spacing.lg)
                )
            }
        }

        if (state.history.isNotEmpty()) {
            Text(
                text = stringResource(R.string.statistics_entries_title),
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                    state.history.reversed().forEach { entry ->
                        WeightEntryRow(
                            entry = entry,
                            onDelete = { viewModel.deleteEntry(entry) }
                        )
                    }
                }
            }
        }

        // --- Körpermaße (v0.12.0) ---
        Text(
            text = stringResource(R.string.body_section_title),
            style = MaterialTheme.typography.headlineSmall
        )

        BodyMeasurementInputCard(onSave = viewModel::saveMeasurements)

        BodyMeasurementChartCard(history = state.bodyHistory)

        if (state.bodyHistory.isNotEmpty()) {
            Text(
                text = stringResource(R.string.body_entries_title),
                style = MaterialTheme.typography.titleMedium
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
                    state.bodyHistory.reversed().forEach { entry ->
                        BodyMeasurementRow(
                            entry = entry,
                            onDelete = { viewModel.deleteMeasurement(entry) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Eingabe-Card für die vier Körpermaße (Taille/Brust/Arm in cm bzw. inch,
 * Körperfett in %). Speichert nur die tatsächlich ausgefüllten Felder für heute;
 * Längenmaße werden vor dem Speichern nach metrisch (cm) zurückgerechnet.
 */
@Composable
private fun BodyMeasurementInputCard(
    onSave: (waistCm: Double?, bodyFatPercent: Double?, chestCm: Double?, armCm: Double?) -> Unit
) {
    val unitSystem = LocalUnitSystem.current
    val lengthSymbol = UnitFormatter.lengthSymbol(unitSystem)

    var waist by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var arm by remember { mutableStateOf("") }

    fun parse(value: String): Double? =
        value.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

    val waistVal = parse(waist)
    val bodyFatVal = parse(bodyFat)
    val chestVal = parse(chest)
    val armVal = parse(arm)
    val anyValid = waistVal != null || bodyFatVal != null || chestVal != null || armVal != null

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
                text = stringResource(R.string.body_input_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                MeasurementField(
                    value = waist,
                    onValueChange = { waist = it },
                    label = stringResource(
                        R.string.body_field_label,
                        stringResource(R.string.body_measure_waist),
                        lengthSymbol
                    ),
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = chest,
                    onValueChange = { chest = it },
                    label = stringResource(
                        R.string.body_field_label,
                        stringResource(R.string.body_measure_chest),
                        lengthSymbol
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                MeasurementField(
                    value = arm,
                    onValueChange = { arm = it },
                    label = stringResource(
                        R.string.body_field_label,
                        stringResource(R.string.body_measure_arm),
                        lengthSymbol
                    ),
                    modifier = Modifier.weight(1f)
                )
                MeasurementField(
                    value = bodyFat,
                    onValueChange = { bodyFat = it },
                    label = stringResource(
                        R.string.body_field_label,
                        stringResource(R.string.body_measure_bodyfat),
                        "%"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Button(
                onClick = {
                    onSave(
                        waistVal?.let { UnitFormatter.toMetricLength(it, unitSystem) },
                        bodyFatVal,
                        chestVal?.let { UnitFormatter.toMetricLength(it, unitSystem) },
                        armVal?.let { UnitFormatter.toMetricLength(it, unitSystem) }
                    )
                    waist = ""
                    bodyFat = ""
                    chest = ""
                    arm = ""
                },
                enabled = anyValid
            ) {
                Text(stringResource(R.string.body_save))
            }
        }
    }
}

@Composable
private fun MeasurementField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier
    )
}

/**
 * Card mit Maß-Auswahl (FilterChips) und einem einzigen Verlaufschart, das das
 * gewählte Maß rendert. Zeigt einen Hinweis, wenn < 2 Datenpunkte vorliegen.
 */
@Composable
private fun BodyMeasurementChartCard(history: List<BodyMeasurement>) {
    var selected by remember { mutableStateOf(BodyMeasurementType.WAIST) }

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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                BodyMeasurementType.entries.forEach { type ->
                    FilterChip(
                        selected = selected == type,
                        onClick = { selected = type },
                        label = { Text(stringResource(type.labelRes)) }
                    )
                }
            }

            val pointCount = history.count { selected.selector(it) != null }
            if (pointCount >= 2) {
                BodyMeasurementChart(
                    entries = history,
                    type = selected,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.body_no_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Verlaufs-/Löschzeile eines Tages-Maßsatzes (Datum + gefüllte Maße + Löschen). */
@Composable
private fun BodyMeasurementRow(entry: BodyMeasurement, onDelete: () -> Unit) {
    val unitSystem = LocalUnitSystem.current
    val parts = buildList {
        entry.waistCm?.let {
            add(stringResource(R.string.body_measure_waist) + " " + UnitFormatter.lengthLabel(it, unitSystem))
        }
        entry.chestCm?.let {
            add(stringResource(R.string.body_measure_chest) + " " + UnitFormatter.lengthLabel(it, unitSystem))
        }
        entry.armCm?.let {
            add(stringResource(R.string.body_measure_arm) + " " + UnitFormatter.lengthLabel(it, unitSystem))
        }
        entry.bodyFatPercent?.let {
            add(stringResource(R.string.body_measure_bodyfat) + " " + formatPercent(it))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = LocalDate.ofEpochDay(entry.epochDay)
                    .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = parts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.body_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPercent(value: Double): String {
    val rounded = if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.1f", value)
    return "$rounded %"
}

@Composable
private fun CurrentTargetCard(
    currentWeightKg: Double?,
    targetWeightKg: Double?
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
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            val unitSystem = LocalUnitSystem.current
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                WeightStat(
                    label = stringResource(R.string.statistics_current_weight),
                    value = currentWeightKg?.let {
                        UnitFormatter.weightLabel(it, unitSystem)
                    } ?: "–"
                )
                WeightStat(
                    label = stringResource(R.string.statistics_target_weight),
                    value = targetWeightKg?.let {
                        UnitFormatter.weightLabel(it, unitSystem)
                    } ?: stringResource(R.string.statistics_target_none)
                )
            }
            if (currentWeightKg != null && targetWeightKg != null) {
                val delta = currentWeightKg - targetWeightKg
                Text(
                    text = if (abs(delta) <= 0.1) {
                        stringResource(R.string.statistics_weight_at_goal)
                    } else {
                        stringResource(
                            R.string.statistics_weight_delta_to_goal_generic,
                            UnitFormatter.weightLabel(abs(delta), unitSystem)
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WeightStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeightInputCard(onSave: (Double) -> Unit) {
    var input by remember { mutableStateOf("") }
    val parsed = input.replace(',', '.').toDoubleOrNull()
    val isValid = parsed != null && parsed > 0.0

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
                .padding(Spacing.lg),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.statistics_weight_input)) },
                supportingText = { Text(stringResource(R.string.statistics_weight_today)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    parsed?.let {
                        if (it > 0.0) {
                            onSave(it)
                            input = ""
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier.padding(top = Spacing.sm)
            ) {
                Text(stringResource(R.string.statistics_weight_save))
            }
        }
    }
}

@Composable
private fun WeightEntryRow(entry: WeightEntry, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = LocalDate.ofEpochDay(entry.epochDay)
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = UnitFormatter.weightLabel(entry.weightKg, LocalUnitSystem.current),
            style = MaterialTheme.typography.titleMedium
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.statistics_delete_weight),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Karte mit dem Wochen-Kalorien-Chart (Balken) und der Ziellinie. */
@Composable
private fun CalorieWeekCard(days: List<DayCalories>, calorieGoal: Int) {
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
                text = stringResource(R.string.statistics_week_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (days.any { it.calories > 0 }) {
                CalorieWeekChart(
                    days = days,
                    calorieGoal = calorieGoal,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(
                    text = stringResource(R.string.statistics_no_calorie_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Kennzahl-Karte (Titel, großer Wert, Untertitel). */
@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Kennzahl-Karte für die aktuelle Eintrags-Serie (Streak). */
@Composable
private fun StreakCard(streakDays: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Text(
                text = stringResource(R.string.statistics_streak_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (streakDays > 0) {
                Text(
                    text = streakDays.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = pluralStringResource(
                        R.plurals.statistics_streak_days,
                        streakDays,
                        streakDays
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "–",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.statistics_streak_none),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Karte mit dem Makro-Donut und Legende. */
@Composable
private fun MacroDonutCard(macros: MacroBreakdown) {
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
                text = stringResource(R.string.statistics_macro_title),
                style = MaterialTheme.typography.titleMedium
            )
            if (macros.isEmpty) {
                Text(
                    text = stringResource(R.string.statistics_macro_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                MacroDonut(macros = macros)
            }
        }
    }
}
