package com.fairtrack.app.ui.produce

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.bls.BlsMicronutrientCatalog
import com.fairtrack.app.data.produce.ProduceCatalog
import com.fairtrack.app.data.produce.ProduceCategory
import com.fairtrack.app.data.produce.ProduceGroup
import com.fairtrack.app.data.produce.ProduceItem
import com.fairtrack.app.ui.scanner.ProductConfirmDialog
import com.fairtrack.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** Interne Schrittfolge des Produce-Screens: Gruppe → Kategorie → Sorten. */
private sealed interface Step {
    data object Group : Step
    data class Category(val group: ProduceGroup) : Step
    data class Items(val category: ProduceCategory) : Step
}

/** Zahl ohne überflüssiges ".0" für ganze Werte (Vorbefüllung des Dialogs). */
private fun fmt(d: Double): String =
    if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

/** Icon je Warengruppe. */
private val ProduceGroup.icon: ImageVector
    get() = when (this) {
        ProduceGroup.FRUIT -> Icons.Rounded.Restaurant
        ProduceGroup.VEGETABLE -> Icons.Rounded.Eco
    }

/**
 * Obst-/Gemüse-Screen (Produce). Führt in drei Schritten von der Warengruppe
 * über die Kategorie zur konkreten Sorte aus dem Offline-Katalog. Die Auswahl
 * öffnet den [ProductConfirmDialog] (vorbefüllt) und speichert über das
 * [ProduceViewModel]; der Screen schließt sich nach dem Speichern selbst.
 */
@Composable
fun ProduceScreen(
    onClose: () -> Unit,
    viewModel: ProduceViewModel = hiltViewModel()
) {
    val target by viewModel.target.collectAsStateWithLifecycle()

    var step by remember { mutableStateOf<Step>(Step.Group) }
    // Aktuell zur Bestätigung ausgewählte Sorte (null = kein Dialog offen).
    var selected by remember { mutableStateOf<ProduceItem?>(null) }

    // Schließt den Screen, sobald ein Eintrag gespeichert wurde.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.finished.collect { onClose() }
    }

    // Einheitliche Zurück-Logik für Back-Pfeil und System-Back.
    fun goBack() {
        when (val s = step) {
            is Step.Items -> step = Step.Category(s.category.group)
            is Step.Category -> step = Step.Group
            Step.Group -> onClose()
        }
    }

    BackHandler(enabled = true) { goBack() }

    // Titel des aktuellen Schritts (Gruppe/Kategorie-Label oder Screen-Titel).
    val title = when (val s = step) {
        Step.Group -> stringResource(R.string.produce_title)
        is Step.Category -> stringResource(s.group.labelRes)
        is Step.Items -> stringResource(s.category.labelRes)
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Kopfzeile mit Zurück-Pfeil und Titel.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                IconButton(onClick = { goBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_cancel)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            when (val s = step) {
                Step.Group -> GroupStep(
                    onSelect = { group -> step = Step.Category(group) }
                )
                is Step.Category -> CategoryStep(
                    group = s.group,
                    onSelect = { category -> step = Step.Items(category) }
                )
                is Step.Items -> ItemsStep(
                    category = s.category,
                    onSelect = { item -> selected = item }
                )
            }
        }
    }

    // Bestätigungs-Dialog: vorbefüllt aus der gewählten Sorte.
    selected?.let { item ->
        ProductConfirmDialog(
            initialName = item.name,
            initialAmountGrams = "100",
            initialCaloriesPer100g = fmt(item.kcal),
            initialProteinPer100g = fmt(item.protein),
            initialCarbsPer100g = fmt(item.carbs),
            initialFatPer100g = fmt(item.fat),
            isBeverage = false,
            initialMeal = target.mealType,
            initialMicros = BlsMicronutrientCatalog.forFood(item.name) ?: Micronutrients(),
            onDismiss = { selected = null },
            // Mikronährstoffe holt addProduce selbst aus dem BLS-Katalog (per Name).
            onConfirm = { meal, name, amt, kcal, p, c, f, unit, _ ->
                viewModel.addProduce(meal, name, amt, kcal, p, c, f, unit)
            }
        )
    }
}

/** Schritt 1: Auswahl der Warengruppe (Obst/Gemüse) über zwei große Karten. */
@Composable
private fun GroupStep(
    onSelect: (ProduceGroup) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Text(
            text = stringResource(R.string.produce_choose_group),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ProduceGroup.entries.forEach { group ->
            Card(
                onClick = { onSelect(group) },
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = group.icon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(group.labelRes),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = Spacing.sm)
                    )
                }
            }
        }
    }
}

/** Schritt 2: Auswahl der Kategorie innerhalb der Gruppe (2-spaltiges Raster). */
@Composable
private fun CategoryStep(
    group: ProduceGroup,
    onSelect: (ProduceCategory) -> Unit
) {
    val categories = remember(group) { ProduceCatalog.categories(group) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Text(
            text = stringResource(R.string.produce_choose_category),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            items(categories, key = { it.name }) { category ->
                Card(
                    onClick = { onSelect(category) },
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(category.labelRes),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/** Schritt 3: Liste der Sorten (alphabetisch) mit Nährwert-Vorschau je 100 g. */
@Composable
private fun ItemsStep(
    category: ProduceCategory,
    onSelect: (ProduceItem) -> Unit
) {
    val entries = remember(category) { ProduceCatalog.items(category) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.lg,
            end = Spacing.lg,
            bottom = Spacing.lg
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        items(entries, key = { it.name }) { item ->
            Card(
                onClick = { onSelect(item) },
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(Spacing.lg)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.produce_kcal_per_100g,
                            item.kcal.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
