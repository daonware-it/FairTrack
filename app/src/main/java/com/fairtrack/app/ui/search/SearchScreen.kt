package com.fairtrack.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.MealTemplateWithItems
import com.fairtrack.app.ui.components.ProductThumbnail
import com.fairtrack.app.ui.home.EditEntryDialog
import com.fairtrack.app.ui.home.QuickAddDialog
import com.fairtrack.app.ui.home.labelRes
import com.fairtrack.app.ui.scanner.ProductConfirmDialog
import com.fairtrack.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * Einstiegspunkt für die Lebensmittelsuche. Zeigt oben das gemerkte Ziel
 * (Tag + Mahlzeit aus der Tagesübersicht) an, das der Nutzer hier noch
 * anpassen kann. Über das Suchfeld läuft die Volltextsuche (Open Food Facts);
 * alternativ führen Scan, Obst & Gemüse oder die manuelle Eingabe zum Ziel –
 * alle speichern auf genau diesen Tag/diese Mahlzeit (auch in der Vergangenheit).
 */
@Composable
fun SearchScreen(
    onScanClick: () -> Unit,
    onProduceClick: () -> Unit,
    onMyFoodsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val target by viewModel.target.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recentEaten by viewModel.recentEaten.collectAsStateWithLifecycle()
    val templates by viewModel.templates.collectAsStateWithLifecycle()

    var showManualDialog by remember { mutableStateOf(false) }
    // Aktuell zur Bestätigung angetipptes Suchergebnis (null = kein Dialog).
    var confirmItem by remember { mutableStateOf<FoodItem?>(null) }
    // Angetippter "Kürzlich gegessen"-Eintrag zum erneuten Hinzufügen.
    var recentToAdd by remember { mutableStateOf<DiaryEntry?>(null) }
    // Vorlagen-Bottom-Sheet offen?
    var showTemplates by remember { mutableStateOf(false) }

    // Formatiert Nährwerte für die String-Felder des Dialogs ohne unnötiges ".0".
    fun fmt(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else d.toString()

    // Reaktiver Favoriten-Abgleich für die Sterne in der Trefferliste.
    fun isFav(item: FoodItem): Boolean = favorites.any { fav ->
        if (!item.barcode.isNullOrBlank()) fav.barcode == item.barcode
        else fav.name == item.name && fav.brand == item.brand
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Suchfeld ganz oben – löst per Tastatur-Aktion die Suche aus.
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.search_food_hint)) },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.search_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch() }),
            modifier = Modifier.fillMaxWidth()
        )

        AddTargetCard(
            epochDay = target.epochDay,
            selectedMeal = target.mealType,
            onMealSelected = viewModel::setMeal
        )

        when (val state = uiState) {
            is SearchUiState.Idle -> IdleBody(
                recentSearches = recentSearches,
                favorites = favorites,
                recentEaten = recentEaten,
                onRecentClick = viewModel::useRecent,
                onClearHistory = viewModel::clearHistory,
                onFavoriteClick = { confirmItem = it },
                onFavoriteToggle = viewModel::toggleFavorite,
                onRecentEatenClick = { recentToAdd = it },
                onTemplatesClick = { showTemplates = true },
                onScanClick = onScanClick,
                onProduceClick = onProduceClick,
                onMyFoodsClick = onMyFoodsClick,
                onManualClick = { showManualDialog = true }
            )

            is SearchUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            is SearchUiState.Results -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(state.items) { item ->
                    FoodResultRow(
                        item = item,
                        isFavorite = isFav(item),
                        onClick = { confirmItem = item },
                        onToggleFavorite = { viewModel.toggleFavorite(item) }
                    )
                }
            }

            is SearchUiState.Empty -> CenteredMessage(
                title = stringResource(R.string.search_empty_title),
                hint = stringResource(R.string.search_empty_hint)
            )

            is SearchUiState.NetworkError -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.search_network_title),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.search_network_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Button(onClick = { viewModel.submitSearch() }) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }

    if (showManualDialog) {
        QuickAddDialog(
            mealType = target.mealType,
            onDismiss = { showManualDialog = false },
            onConfirm = { result ->
                viewModel.addManual(result)
                showManualDialog = false
            }
        )
    }

    confirmItem?.let { item ->
        ProductConfirmDialog(
            initialName = item.name,
            initialBrand = item.brand,
            initialCaloriesPer100g = fmt(item.caloriesPer100g),
            initialProteinPer100g = fmt(item.proteinPer100g),
            initialCarbsPer100g = fmt(item.carbsPer100g),
            initialFatPer100g = fmt(item.fatPer100g),
            isBeverage = item.isBeverage,
            imageUrl = item.imageUrl,
            initialMeal = target.mealType,
            initialMicros = item.micros,
            onDismiss = { confirmItem = null },
            onConfirm = { mealType, name, amountGrams, kcal, protein, carbs, fat, unit, micros ->
                viewModel.addEntry(mealType, name, amountGrams, kcal, protein, carbs, fat, unit, micros)
                confirmItem = null
            }
        )
    }

    // "Kürzlich gegessen" antippen: EditEntryDialog vorbefüllt (Menge/Makros
    // aus dem Ursprungseintrag), dann als neuen Eintrag am Ziel speichern.
    recentToAdd?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { recentToAdd = null },
            onConfirm = { result ->
                viewModel.addEntry(
                    mealType = result.mealType,
                    name = result.name,
                    amountGrams = result.amountGrams,
                    kcal = result.caloriesPer100g,
                    protein = result.proteinPer100g,
                    carbs = result.carbsPer100g,
                    fat = result.fatPer100g,
                    unit = result.unit,
                    // Snapshot des Ursprungseintrags zurück auf pro-100 g rechnen,
                    // damit addEntry mit der neuen Menge korrekt skaliert.
                    microsPer100g = if (entry.amountGrams > 0.0) {
                        entry.micros.scale(100.0 / entry.amountGrams)
                    } else {
                        entry.micros
                    }
                )
                recentToAdd = null
            }
        )
    }

    if (showTemplates) {
        TemplatesSheet(
            templates = templates,
            onDismiss = { showTemplates = false },
            onApply = {
                viewModel.applyTemplate(it)
                showTemplates = false
            },
            onDelete = viewModel::deleteTemplate
        )
    }
}

/** Startzustand: Suchverlauf, Favoriten, kürzlich gegessen + Aktions-Karte. */
@Composable
private fun IdleBody(
    recentSearches: List<String>,
    favorites: List<FoodItem>,
    recentEaten: List<DiaryEntry>,
    onRecentClick: (String) -> Unit,
    onClearHistory: () -> Unit,
    onFavoriteClick: (FoodItem) -> Unit,
    onFavoriteToggle: (FoodItem) -> Unit,
    onRecentEatenClick: (DiaryEntry) -> Unit,
    onTemplatesClick: () -> Unit,
    onScanClick: () -> Unit,
    onProduceClick: () -> Unit,
    onMyFoodsClick: () -> Unit,
    onManualClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Schnellzugriff: Mahlzeiten-Vorlagen öffnen.
        FilledTonalButton(
            onClick = onTemplatesClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.Bookmarks,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.templates_button),
                modifier = Modifier.padding(start = Spacing.sm)
            )
        }

        if (favorites.isNotEmpty()) {
            SectionHeader(text = stringResource(R.string.favorites_title))
            favorites.forEach { item ->
                CompactFoodRow(
                    title = item.name,
                    subtitle = item.brand?.takeIf { it.isNotBlank() },
                    detail = "${item.caloriesPer100g.roundToInt()} kcal / 100 g",
                    imageUrl = item.imageUrl,
                    isFavorite = true,
                    onClick = { onFavoriteClick(item) },
                    onToggleFavorite = { onFavoriteToggle(item) }
                )
            }
        }

        if (recentEaten.isNotEmpty()) {
            SectionHeader(text = stringResource(R.string.recent_eaten_title))
            recentEaten.forEach { entry ->
                CompactFoodRow(
                    title = entry.foodName,
                    subtitle = "${entry.amountGrams.roundToInt()} ${entry.unit.symbol}",
                    detail = "${entry.calories.roundToInt()} kcal",
                    imageUrl = null,
                    leadingIcon = Icons.Rounded.History,
                    onClick = { onRecentEatenClick(entry) }
                )
            }
        }

        if (recentSearches.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.search_recent_searches),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onClearHistory) {
                        Text(stringResource(R.string.search_clear_history))
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    recentSearches.forEach { term ->
                        FilterChip(
                            selected = false,
                            onClick = { onRecentClick(term) },
                            label = { Text(term) }
                        )
                    }
                }
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
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    text = stringResource(R.string.search_scan_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = onScanClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.search_scan_button),
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
                FilledTonalButton(
                    onClick = onProduceClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.search_produce_button),
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
                FilledTonalButton(
                    onClick = onMyFoodsClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bookmarks,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.search_myfoods_button),
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
                OutlinedButton(
                    onClick = onManualClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.search_manual_button),
                        modifier = Modifier.padding(start = Spacing.sm)
                    )
                }
            }
        }
    }
}

/** Eine Zeile in der Trefferliste – tippt sich in den Bestätigungs-Dialog. */
@Composable
private fun FoodResultRow(
    item: FoodItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
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
                .padding(Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductThumbnail(imageUrl = item.imageUrl, size = 56.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium
                )
                item.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${item.caloriesPer100g.roundToInt()} kcal / 100 g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FavoriteToggle(isFavorite = isFavorite, onToggle = onToggleFavorite)
        }
    }
}

/** Stern-Icon zum Umschalten des Favoriten-Status. */
@Composable
private fun FavoriteToggle(isFavorite: Boolean, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (isFavorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            contentDescription = stringResource(
                if (isFavorite) R.string.favorite_remove else R.string.favorite_add
            ),
            tint = if (isFavorite) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Abschnitts-Überschrift im Idle-Zustand. */
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Kompakte Produkt-/Eintragszeile für Favoriten und "Kürzlich gegessen".
 * Optional mit Stern (Favoriten) oder führendem Icon (kürzlich gegessen).
 */
@Composable
private fun CompactFoodRow(
    title: String,
    subtitle: String?,
    detail: String,
    imageUrl: String?,
    isFavorite: Boolean = false,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    onToggleFavorite: (() -> Unit)? = null
) {
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
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                ProductThumbnail(imageUrl = imageUrl, size = 40.dp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val secondary = listOfNotNull(subtitle, detail).joinToString(" · ")
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onToggleFavorite != null) {
                FavoriteToggle(isFavorite = isFavorite, onToggle = onToggleFavorite)
            }
        }
    }
}

/** Bottom-Sheet mit allen Mahlzeiten-Vorlagen; Tap wendet an, ⋮/Icon löscht. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesSheet(
    templates: List<MealTemplateWithItems>,
    onDismiss: () -> Unit,
    onApply: (MealTemplateWithItems) -> Unit,
    onDelete: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.templates_title),
                style = MaterialTheme.typography.titleLarge
            )
            if (templates.isEmpty()) {
                Text(
                    text = stringResource(R.string.templates_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(R.string.templates_apply_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                templates.forEach { template ->
                    val kcal = template.items.sumOf { item ->
                        item.caloriesPer100g * item.amountGrams / 100.0
                    }.roundToInt()
                    ListItem(
                        headlineContent = { Text(template.template.name) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.templates_item_count,
                                    template.items.size,
                                    kcal
                                )
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { onDelete(template.template.id) }) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.template_delete)
                                )
                            }
                        },
                        modifier = Modifier.clickable { onApply(template) }
                    )
                }
            }
        }
    }
}

/** Zentrierte Info-Meldung (z. B. für leere Trefferliste). */
@Composable
private fun CenteredMessage(
    title: String,
    hint: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Kopfkarte: zeigt Zieltag und lässt die Mahlzeit umstellen. */
@Composable
private fun AddTargetCard(
    epochDay: Long,
    selectedMeal: MealType,
    onMealSelected: (MealType) -> Unit
) {
    val date = LocalDate.ofEpochDay(epochDay)
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
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = stringResource(R.string.search_target_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                MealType.entries.forEach { meal ->
                    FilterChip(
                        selected = meal == selectedMeal,
                        onClick = { onMealSelected(meal) },
                        label = { Text(stringResource(meal.labelRes)) }
                    )
                }
            }
        }
    }
}
