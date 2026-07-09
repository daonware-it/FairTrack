package com.fairtrack.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material.icons.rounded.Today
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.UnitFormatter
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.ui.LocalUnitSystem
import com.fairtrack.app.ui.theme.Dimens
import com.fairtrack.app.ui.theme.Spacing
import com.fairtrack.app.ui.theme.fairTrackColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun HomeScreen(
    onAddFood: () -> Unit,
    onOpenFasting: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val waterState by viewModel.waterState.collectAsStateWithLifecycle()
    val reminderState by viewModel.reminderState.collectAsStateWithLifecycle()
    val activityState by viewModel.activityState.collectAsStateWithLifecycle()

    // Welcher Eintrag soll gelöscht werden (null = keine Rückfrage offen).
    var entryToDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    // Welcher Eintrag wird gerade bearbeitet (null = kein Dialog offen).
    var entryToEdit by remember { mutableStateOf<DiaryEntry?>(null) }
    // Welche Mahlzeit soll als Vorlage gespeichert werden (null = kein Dialog).
    var mealToSaveAsTemplate by remember { mutableStateOf<MealSectionState?>(null) }
    // Kalender zur Datumsauswahl offen?
    var showDatePicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        item {
            DateNavigationBar(
                date = uiState.date,
                isToday = uiState.isToday,
                onPrevious = viewModel::showPreviousDay,
                onNext = viewModel::showNextDay,
                onToday = viewModel::goToToday,
                onDateClick = { showDatePicker = true }
            )
        }
        item { CalorieSummaryCard(uiState) }
        item { MacroOverviewCard(uiState) }
        item {
            WaterCard(
                water = waterState,
                reminder = reminderState,
                onAdd = viewModel::addWater,
                onReset = viewModel::resetWater,
                onSetGoal = viewModel::setWaterGoal,
                onReminderEnabledChange = viewModel::setReminderEnabled,
                onReminderIntervalChange = viewModel::setReminderInterval
            )
        }
        item {
            ActivityCard(
                state = activityState,
                onPermissionResult = viewModel::refreshActivityAccess
            )
        }
        item { FastingEntryCard(onClick = onOpenFasting) }

        items(uiState.meals, key = { it.mealType }) { meal ->
            MealSectionCard(
                meal = meal,
                onAddClick = {
                    // Tag + Mahlzeit merken, dann zum Such-Tab wechseln.
                    viewModel.prepareAdd(meal.mealType)
                    onAddFood()
                },
                onEditEntry = { entryToEdit = it },
                onDeleteEntry = { entryToDelete = it },
                onSaveAsTemplate = { mealToSaveAsTemplate = meal }
            )
        }
    }

    // Sicherheitsabfrage vor dem Löschen eines Eintrags.
    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            icon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.delete_entry_title)) },
            text = { Text(stringResource(R.string.delete_entry_message, entry.foodName)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry)
                        entryToDelete = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Bearbeiten eines bestehenden Eintrags.
    entryToEdit?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { entryToEdit = null },
            onConfirm = { result ->
                viewModel.updateEntry(
                    original = entry,
                    mealType = result.mealType,
                    foodName = result.name,
                    amountGrams = result.amountGrams,
                    caloriesPer100g = result.caloriesPer100g,
                    proteinPer100g = result.proteinPer100g,
                    carbsPer100g = result.carbsPer100g,
                    fatPer100g = result.fatPer100g,
                    unit = result.unit
                )
                entryToEdit = null
            }
        )
    }

    // Mahlzeit als Vorlage speichern: Namen abfragen.
    mealToSaveAsTemplate?.let { meal ->
        SaveTemplateDialog(
            initialName = stringResource(meal.mealType.labelRes),
            onDismiss = { mealToSaveAsTemplate = null },
            onConfirm = { name ->
                viewModel.saveMealAsTemplate(name, meal.entries)
                mealToSaveAsTemplate = null
            }
        )
    }

    // Kalender zur Datumsauswahl (Zukunft ausgegraut).
    if (showDatePicker) {
        DiaryDatePickerDialog(
            initialDate = uiState.date,
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                viewModel.selectDate(date)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun DateNavigationBar(
    date: LocalDate,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDateClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.xs, vertical = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.previous_day)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onDateClick)
                        .padding(vertical = Spacing.xs)
                ) {
                    Text(
                        text = relativeDayLabel(date),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = date.format(
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Kein Vorwärts über heute hinaus – keine Einträge in der Zukunft.
                IconButton(onClick = onNext, enabled = !isToday) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.next_day)
                    )
                }
            }
        }
        if (!isToday) {
            FilledTonalButton(
                onClick = onToday,
                modifier = Modifier.padding(top = Spacing.sm)
            ) {
                Icon(
                    Icons.Rounded.Today,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.back_to_today))
            }
        }
    }
}

/**
 * Kalender zur Datumsauswahl. Zukünftige Tage sind ausgegraut und nicht
 * wählbar; Vergangenheit und heute lassen sich antippen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiaryDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val todayEpochDay = LocalDate.now().toEpochDay()
    val currentYear = LocalDate.now().year
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toEpochDay() * MILLIS_PER_DAY,
        selectableDates = object : SelectableDates {
            // UTC-Millis der Auswahl -> epochDay; alles nach heute sperren.
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis / MILLIS_PER_DAY <= todayEpochDay

            override fun isSelectableYear(year: Int): Boolean = year <= currentYear
        }
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    }
                }
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    ) {
        DatePicker(state = datePickerState, title = null)
    }
}

/** Millisekunden pro Tag – für die Umrechnung epochDay <-> UTC-Millis des DatePickers. */
private const val MILLIS_PER_DAY = 86_400_000L

/** Einstiegs-Karte auf dem Home-Screen zum Fasten-Timer (v0.13.0). */
@Composable
private fun FastingEntryCard(onClick: () -> Unit) {
    val accent = MaterialTheme.fairTrackColors.accentDinner
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(Dimens.iconChip)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timelapse,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.fasting_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.fasting_entry_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalorieSummaryCard(state: HomeUiState) {
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
                .padding(Spacing.xl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.xl)
        ) {
            CalorieRing(
                consumed = state.consumedCalories,
                goal = state.goalCalories,
                remaining = state.remainingCalories,
                modifier = Modifier.size(Dimens.calorieRingSize)
            )
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                CalorieStat(
                    label = stringResource(R.string.calories_consumed),
                    value = "${state.consumedCalories} ${stringResource(R.string.kcal)}"
                )
                CalorieStat(
                    label = stringResource(R.string.calories_goal),
                    value = "${state.goalCalories} ${stringResource(R.string.kcal)}"
                )
                val percent = if (state.goalCalories > 0) {
                    (state.consumedCalories * 100f / state.goalCalories).roundToInt()
                } else 0
                CalorieStat(
                    label = stringResource(R.string.calories_percent),
                    value = stringResource(R.string.percent_short, percent)
                )
            }
        }
    }
}

@Composable
private fun CalorieStat(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CalorieRing(
    consumed: Int,
    goal: Int,
    remaining: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val over = remaining < 0
    val trackColor = MaterialTheme.fairTrackColors.ringTrack
    val gradientStart = MaterialTheme.colorScheme.primary
    val gradientEnd = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Dimens.calorieRingStroke.toPx()
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
            if (progress > 0f) {
                // Sweep-Gradient beginnt bei 0° (3-Uhr-Position); Canvas so drehen,
                // dass der Bogen oben startet und die Gradient-Naht am Bogenende liegt.
                val brush = when {
                    over -> SolidColor(errorColor)
                    progress >= 1f -> Brush.sweepGradient(
                        0f to gradientStart,
                        1f to gradientEnd,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                    else -> Brush.sweepGradient(
                        0f to gradientStart,
                        progress to gradientEnd,
                        1f to gradientStart,
                        center = Offset(size.width / 2, size.height / 2)
                    )
                }
                rotate(degrees = -90f) {
                    drawArc(
                        brush = brush,
                        startAngle = 0f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${abs(remaining)}",
                style = MaterialTheme.typography.displaySmall,
                color = if (over) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(
                    if (over) R.string.calories_over_short else R.string.calories_remaining_short
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MacroOverviewCard(state: HomeUiState) {
    val colors = MaterialTheme.fairTrackColors
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
            MacroRow(
                label = stringResource(R.string.macro_protein),
                macro = state.protein,
                barColor = colors.protein,
                trackColor = colors.proteinContainer
            )
            MacroRow(
                label = stringResource(R.string.macro_carbs),
                macro = state.carbs,
                barColor = colors.carbs,
                trackColor = colors.carbsContainer
            )
            MacroRow(
                label = stringResource(R.string.macro_fat),
                macro = state.fat,
                barColor = colors.fat,
                trackColor = colors.fatContainer
            )
        }
    }
}

@Composable
private fun MacroRow(
    label: String,
    macro: MacroValue,
    barColor: Color,
    trackColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.weight(1f)
            )
            // Fortschritt zum Tagesziel (kann über 100 % gehen).
            Text(
                text = stringResource(R.string.percent_short, macro.percent),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.grams_of_goal, macro.grams, macro.goalGrams),
                style = MaterialTheme.typography.labelLarge
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.macroBarHeight)
                .clip(CircleShape)
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(macro.goalFraction)
                    .height(Dimens.macroBarHeight)
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun MealSectionCard(
    meal: MealSectionState,
    onAddClick: () -> Unit,
    onEditEntry: (DiaryEntry) -> Unit,
    onDeleteEntry: (DiaryEntry) -> Unit,
    onSaveAsTemplate: () -> Unit
) {
    val accent = meal.mealType.accentColor
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.sm, top = Spacing.xs, bottom = Spacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(Dimens.iconChip)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = meal.mealType.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = stringResource(meal.mealType.labelRes),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = accent.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${meal.calories} ${stringResource(R.string.kcal)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                FilledTonalIconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.add_entry)
                    )
                }
                // Überlaufmenü: nur sinnvoll, wenn die Mahlzeit Einträge hat.
                if (meal.entries.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = stringResource(R.string.meal_more_actions)
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.save_as_template)) },
                                onClick = {
                                    menuExpanded = false
                                    onSaveAsTemplate()
                                }
                            )
                        }
                    }
                }
            }

            if (meal.entries.isEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)
                ) {
                    Icon(
                        Icons.Rounded.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text(
                        text = stringResource(R.string.meal_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                meal.entries.forEach { entry ->
                    EntryRow(
                        entry = entry,
                        onEdit = { onEditEntry(entry) },
                        onDelete = { onDeleteEntry(entry) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: DiaryEntry, onEdit: () -> Unit, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Wischen nach links (rechts -> links): löschen.
                SwipeToDismissBoxValue.EndToStart -> onDelete()
                // Wischen nach rechts (links -> rechts): bearbeiten.
                SwipeToDismissBoxValue.StartToEnd -> onEdit()
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Zeile nie automatisch entfernen -> federt zurück.
            false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            // Rechts-Wisch = bearbeiten (neutral), Links-Wisch = löschen (rot).
            val color = when (direction) {
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondaryContainer
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = if (direction == SwipeToDismissBoxValue.EndToStart) {
                Alignment.CenterEnd
            } else {
                Alignment.CenterStart
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = Spacing.lg),
                contentAlignment = alignment
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.EndToStart -> Icon(
                        Icons.Rounded.DeleteOutline,
                        contentDescription = stringResource(R.string.delete_entry),
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    SwipeToDismissBoxValue.StartToEnd -> Icon(
                        Icons.Rounded.EditNote,
                        contentDescription = stringResource(R.string.edit_entry),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val unitSystem = LocalUnitSystem.current
                Text(
                    text = "${UnitFormatter.formatAmount(entry.amountGrams, entry.unit, unitSystem)} · " +
                        "${entry.calories.roundToInt()} ${stringResource(R.string.kcal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Spacing.xxs))
                val colors = MaterialTheme.fairTrackColors
                val separator = " · "
                val macroText = buildAnnotatedString {
                    withStyle(SpanStyle(color = colors.protein)) {
                        append(
                            stringResource(
                                R.string.entry_macro_named,
                                stringResource(R.string.macro_protein),
                                entry.protein.roundToInt()
                            )
                        )
                    }
                    append(separator)
                    withStyle(SpanStyle(color = colors.carbs)) {
                        append(
                            stringResource(
                                R.string.entry_macro_named,
                                stringResource(R.string.macro_carbs),
                                entry.carbs.roundToInt()
                            )
                        )
                    }
                    append(separator)
                    withStyle(SpanStyle(color = colors.fat)) {
                        append(
                            stringResource(
                                R.string.entry_macro_named,
                                stringResource(R.string.macro_fat),
                                entry.fat.roundToInt()
                            )
                        )
                    }
                }
                Text(
                    text = macroText,
                    // Standardfarbe gilt für die Trenner "·"; Makros sind farbig gespannt.
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Dialog: Namen für eine neue Mahlzeiten-Vorlage abfragen. */
@Composable
private fun SaveTemplateDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_template_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.save_template_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onConfirm(name.trim()) }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** "Heute" / "Gestern" / "Morgen" oder Wochentag als Kurzlabel. */
@Composable
private fun relativeDayLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> stringResource(R.string.today)
        today.minusDays(1) -> stringResource(R.string.yesterday)
        today.plusDays(1) -> stringResource(R.string.tomorrow)
        else -> date.format(DateTimeFormatter.ofPattern("EEEE"))
    }
}
