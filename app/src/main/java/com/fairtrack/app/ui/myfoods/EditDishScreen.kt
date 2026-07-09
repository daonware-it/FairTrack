package com.fairtrack.app.ui.myfoods

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.perServing
import com.fairtrack.app.ui.components.ProductThumbnail
import com.fairtrack.app.ui.myfoods.EditDishViewModel.Companion.parseDecimal
import com.fairtrack.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** Formatiert eine Zahl ohne unnötiges ".0". */
private fun fmt(value: Double): String {
    val rounded = value.roundToInt()
    return if (kotlin.math.abs(value - rounded) < 0.05) rounded.toString()
    else String.format("%.1f", value)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDishScreen(
    onClose: () -> Unit,
    onScanIngredient: () -> Unit,
    viewModel: EditDishViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val servings by viewModel.servings.collectAsStateWithLifecycle()
    val ingredients by viewModel.ingredients.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val ownFoods by viewModel.ownFoods.collectAsStateWithLifecycle()
    val isValid by viewModel.isValid.collectAsStateWithLifecycle()

    // Dialog-Zustände.
    var showManualDialog by remember { mutableStateOf(false) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var showOnlineSearch by remember { mutableStateOf(false) }
    // Index der zu bearbeitenden Zutat (-1 = neue manuelle Zutat).
    var editIndex by remember { mutableStateOf(-1) }
    // Gewähltes/gescanntes Produkt, für das noch die Menge abgefragt wird.
    var pendingFood by remember { mutableStateOf<FoodItem?>(null) }

    // Vom Barcode-Scanner (im "für Ergebnis"-Modus) zurückkommende Zutat.
    LaunchedEffect(Unit) {
        viewModel.scannedIngredient.collect { food -> pendingFood = food }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isNew) R.string.myfoods_new_dish
                            else R.string.editdish_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.editdish_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = servings,
                onValueChange = viewModel::onServingsChange,
                label = { Text(stringResource(R.string.editdish_servings)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            // Live-Zusammenfassung.
            val servingsValue = parseDecimal(servings)?.takeIf { it > 0 } ?: 1.0
            val perServing = totals.perServing(servingsValue)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = stringResource(R.string.editdish_total_kcal, fmt(totals.kcal)),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.editdish_macros_line,
                            fmt(totals.protein), fmt(totals.carbs), fmt(totals.fat)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Divider(
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                    )
                    Text(
                        text = stringResource(
                            R.string.editdish_per_serving,
                            fmt(servingsValue), fmt(perServing.kcal)
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.editdish_macros_line,
                            fmt(perServing.protein), fmt(perServing.carbs), fmt(perServing.fat)
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Text(
                text = stringResource(R.string.editdish_ingredients),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (ingredients.isEmpty()) {
                Text(
                    text = stringResource(R.string.editdish_no_ingredients),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ingredients.forEachIndexed { index, ingredient ->
                    IngredientRow(
                        ingredient = ingredient,
                        onClick = { editIndex = index; showManualDialog = true },
                        onRemove = { viewModel.removeIngredient(index) }
                    )
                }
            }

            // Schnelle Zutaten-Quellen: Online-Datenbank + Barcode zuerst.
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = { showOnlineSearch = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null)
                    Text(
                        stringResource(R.string.editdish_online_search),
                        modifier = Modifier.padding(start = Spacing.xs)
                    )
                }
                OutlinedButton(
                    onClick = onScanIngredient,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                    Text(
                        stringResource(R.string.editdish_barcode),
                        modifier = Modifier.padding(start = Spacing.xs)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(
                    onClick = { editIndex = -1; showManualDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(
                        stringResource(R.string.editdish_manual),
                        modifier = Modifier.padding(start = Spacing.xs)
                    )
                }
                OutlinedButton(
                    onClick = { showFoodPicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Restaurant, contentDescription = null)
                    Text(
                        stringResource(R.string.editdish_my_foods),
                        modifier = Modifier.padding(start = Spacing.xs)
                    )
                }
            }

            Button(
                onClick = { viewModel.save(onClose) },
                enabled = isValid,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }

    if (showManualDialog) {
        val existing = ingredients.getOrNull(editIndex)
        IngredientDialog(
            initial = existing,
            onDismiss = { showManualDialog = false },
            onConfirm = { result ->
                if (editIndex >= 0) {
                    viewModel.updateIngredient(editIndex, result.copy(dishId = existing?.dishId ?: 0, id = existing?.id ?: 0))
                } else {
                    viewModel.addIngredient(
                        name = result.name,
                        amountGrams = result.amountGrams,
                        kcalPer100g = result.caloriesPer100g,
                        proteinPer100g = result.proteinPer100g,
                        carbsPer100g = result.carbsPer100g,
                        fatPer100g = result.fatPer100g
                    )
                }
                showManualDialog = false
            }
        )
    }

    if (showFoodPicker) {
        FoodPickerDialog(
            foods = ownFoods,
            onDismiss = { showFoodPicker = false },
            onPick = { food, amount ->
                viewModel.addIngredientFromFood(food, amount)
                showFoodPicker = false
            }
        )
    }

    if (showOnlineSearch) {
        val query by viewModel.onlineQuery.collectAsStateWithLifecycle()
        val results by viewModel.onlineResults.collectAsStateWithLifecycle()
        OnlineSearchDialog(
            query = query,
            results = results,
            onQueryChange = viewModel::onOnlineQueryChange,
            onPick = { food ->
                showOnlineSearch = false
                viewModel.clearOnlineSearch()
                pendingFood = food
            },
            onDismiss = {
                showOnlineSearch = false
                viewModel.clearOnlineSearch()
            }
        )
    }

    // Menge abfragen für ein online gesuchtes oder gescanntes Produkt.
    pendingFood?.let { food ->
        AmountForFoodDialog(
            food = food,
            onDismiss = { pendingFood = null },
            onConfirm = { amount ->
                viewModel.addIngredientFromFood(food, amount)
                pendingFood = null
            }
        )
    }
}

/** Eine Zutatzeile mit Beitrag zur Gesamtkalorienzahl und Aktionen. */
@Composable
private fun IngredientRow(
    ingredient: DishIngredient,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val kcal = ingredient.caloriesPer100g * ingredient.amountGrams / 100.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${fmt(ingredient.amountGrams)} g · ${fmt(kcal)} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_remove))
            }
        }
    }
}

/** Dialog zum manuellen Erfassen bzw. Bearbeiten einer Zutat. */
@Composable
private fun IngredientDialog(
    initial: DishIngredient?,
    onDismiss: () -> Unit,
    onConfirm: (DishIngredient) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amount by remember { mutableStateOf(initial?.amountGrams?.let { fmt(it) } ?: "100") }
    var kcal by remember { mutableStateOf(initial?.caloriesPer100g?.let { fmt(it) } ?: "") }
    var protein by remember { mutableStateOf(initial?.proteinPer100g?.let { fmt(it) } ?: "") }
    var carbs by remember { mutableStateOf(initial?.carbsPer100g?.let { fmt(it) } ?: "") }
    var fat by remember { mutableStateOf(initial?.fatPer100g?.let { fmt(it) } ?: "") }

    val amountValue = parseDecimal(amount)
    val kcalValue = parseDecimal(kcal)
    val canConfirm = name.isNotBlank() &&
        amountValue != null && amountValue > 0 &&
        kcalValue != null && kcalValue >= 0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null) },
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.editdish_add_ingredient
                    else R.string.editdish_edit_ingredient
                )
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.edit_food_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.quick_add_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text(stringResource(R.string.editdish_kcal_per_100g_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.editdish_macros_optional),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.editdish_protein)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.editdish_carbs)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.editdish_fat)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        DishIngredient(
                            dishId = 0,
                            name = name.trim(),
                            amountGrams = amountValue ?: 0.0,
                            caloriesPer100g = kcalValue ?: 0.0,
                            proteinPer100g = parseDecimal(protein) ?: 0.0,
                            carbsPer100g = parseDecimal(carbs) ?: 0.0,
                            fatPer100g = parseDecimal(fat) ?: 0.0
                        )
                    )
                }
            ) { Text(stringResource(R.string.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Dialog: Online-Datenbank (Open Food Facts) durchsuchen und Treffer wählen. */
@Composable
private fun OnlineSearchDialog(
    query: String,
    results: OnlineSearchUi,
    onQueryChange: (String) -> Unit,
    onPick: (FoodItem) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        title = { Text(stringResource(R.string.editdish_online_search)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(R.string.search_food_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                when (val r = results) {
                    OnlineSearchUi.Idle -> Text(
                        text = stringResource(R.string.editdish_min_chars),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OnlineSearchUi.Loading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(Spacing.md),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    OnlineSearchUi.Empty -> Text(
                        text = stringResource(R.string.editdish_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OnlineSearchUi.NetworkError -> Text(
                        text = stringResource(R.string.editdish_network_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    is OnlineSearchUi.Results -> Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        r.items.forEach { food ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { onPick(food) }
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductThumbnail(imageUrl = food.imageUrl, size = 44.dp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    val brand = food.brand?.takeIf { it.isNotBlank() }
                                    Text(
                                        text = buildString {
                                            if (brand != null) append("$brand · ")
                                            append(
                                                stringResource(
                                                    R.string.editdish_kcal_per_100g_value,
                                                    fmt(food.caloriesPer100g)
                                                )
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        }
    )
}

/** Dialog: Menge in Gramm für ein gewähltes/gescanntes Produkt abfragen. */
@Composable
private fun AmountForFoodDialog(
    food: FoodItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("100") }
    val amountValue = parseDecimal(amount)
    val canConfirm = amountValue != null && amountValue > 0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null) },
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                if (!food.imageUrl.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ProductThumbnail(imageUrl = food.imageUrl, size = 88.dp)
                    }
                }
                Text(
                    text = stringResource(
                        R.string.editdish_kcal_per_100g_value,
                        fmt(food.caloriesPer100g)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(stringResource(R.string.quick_add_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                if (amountValue != null && amountValue > 0) {
                    val f = amountValue / 100.0
                    Text(
                        text = stringResource(
                            R.string.editdish_food_macros,
                            fmt(food.caloriesPer100g * f),
                            fmt(food.proteinPer100g * f),
                            fmt(food.carbsPer100g * f),
                            fmt(food.fatPer100g * f)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { amountValue?.let(onConfirm) }
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Dialog: eigenes Lebensmittel auswählen und Menge in Gramm angeben. */
@Composable
private fun FoodPickerDialog(
    foods: List<FoodItem>,
    onDismiss: () -> Unit,
    onPick: (FoodItem, Double) -> Unit
) {
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var amount by remember { mutableStateOf("100") }

    val amountValue = parseDecimal(amount)
    val canConfirm = selected != null && amountValue != null && amountValue > 0

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null) },
        title = { Text(stringResource(R.string.editdish_from_my_foods)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (foods.isEmpty()) {
                    Text(
                        text = stringResource(R.string.editdish_no_own_foods),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(
                        modifier = Modifier.heightIn(max = 260.dp),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        foods.forEach { food ->
                            val isSelected = selected?.id == food.id
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = MaterialTheme.shapes.small
                                    )
                                    .clickable { selected = food }
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                            ) {
                                Column {
                                    Text(
                                        text = food.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.editdish_kcal_per_100g_value,
                                            fmt(food.caloriesPer100g)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text(stringResource(R.string.quick_add_amount)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    val food = selected
                    if (food != null && amountValue != null) {
                        onPick(food, amountValue)
                    }
                }
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
