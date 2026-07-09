package com.fairtrack.app.ui.myfoods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.entity.DishWithIngredients
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import com.fairtrack.app.data.perServing
import com.fairtrack.app.data.totals
import com.fairtrack.app.ui.home.labelRes
import com.fairtrack.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

/**
 * "Meine Lebensmittel": zeigt eigene Lebensmittel und Gerichte in zwei Tabs.
 * Antippen legt einen Tagebuch-Eintrag am gemerkten Ziel (Tag + Mahlzeit) an;
 * über das Überlauf-Menü lassen sich Einträge bearbeiten oder löschen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFoodsScreen(
    onClose: () -> Unit,
    onCreateFood: () -> Unit,
    onEditFood: (Long) -> Unit,
    onCreateDish: () -> Unit,
    onEditDish: (Long) -> Unit,
    viewModel: MyFoodsViewModel = hiltViewModel()
) {
    val target by viewModel.target.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val dishes by viewModel.dishes.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(0) }

    // Aktuell angetipptes Lebensmittel + dessen (asynchron geladene) Portionen.
    var dialogFood by remember { mutableStateOf<FoodItem?>(null) }
    var dialogPortions by remember { mutableStateOf<List<FoodPortion>>(emptyList()) }
    // Aktuell angetipptes Gericht für den Portionen-Dialog.
    var dialogDish by remember { mutableStateOf<DishWithIngredients?>(null) }
    // Zu löschendes Element (Food oder Dish) für die Bestätigung.
    var foodToDelete by remember { mutableStateOf<FoodItem?>(null) }
    var dishToDelete by remember { mutableStateOf<DishWithIngredients?>(null) }

    LaunchedEffect(dialogFood) {
        val food = dialogFood
        dialogPortions = if (food != null) viewModel.portionsFor(food.id) else emptyList()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.myfoods_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (selectedTab == 0) onCreateFood() else onCreateDish() },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = {
                    Text(
                        stringResource(
                            if (selectedTab == 0) R.string.myfoods_new_food
                            else R.string.myfoods_new_dish
                        )
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            TargetHeader(
                epochDay = target.epochDay,
                selectedMeal = target.mealType,
                onMealSelected = viewModel::setMeal
            )

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.myfoods_search)) },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.myfoods_tab_foods)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.myfoods_tab_dishes)) }
                )
            }

            when (selectedTab) {
                0 -> FoodsList(
                    foods = foods,
                    onFoodClick = { dialogFood = it },
                    onEdit = { onEditFood(it.id) },
                    onDelete = { foodToDelete = it },
                    onToggleFavorite = viewModel::toggleFavorite
                )

                else -> DishesList(
                    dishes = dishes,
                    onDishClick = { dialogDish = it },
                    onEdit = { onEditDish(it.dish.id) },
                    onDelete = { dishToDelete = it }
                )
            }
        }
    }

    dialogFood?.let { food ->
        AddToDiaryDialog(
            food = food,
            portions = dialogPortions,
            initialMeal = target.mealType,
            onDismiss = { dialogFood = null },
            onConfirm = { meal, amountGrams, unit ->
                viewModel.addFood(food, amountGrams, unit, meal)
                dialogFood = null
            }
        )
    }

    dialogDish?.let { dish ->
        AddDishDialog(
            dish = dish,
            initialMeal = target.mealType,
            onDismiss = { dialogDish = null },
            onConfirm = { meal, servings ->
                viewModel.addDish(dish, servings, meal)
                dialogDish = null
            }
        )
    }

    foodToDelete?.let { food ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.myfoods_delete_food_title),
            message = stringResource(R.string.myfoods_delete_food_message, food.name),
            onDismiss = { foodToDelete = null },
            onConfirm = {
                viewModel.deleteFood(food.id)
                foodToDelete = null
            }
        )
    }

    dishToDelete?.let { dish ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.myfoods_delete_dish_title),
            message = stringResource(R.string.myfoods_delete_dish_message, dish.dish.name),
            onDismiss = { dishToDelete = null },
            onConfirm = {
                viewModel.deleteDish(dish.dish.id)
                dishToDelete = null
            }
        )
    }
}

/** Kompakter Ziel-Kopf: Tag + umschaltbare Mahlzeit (wie auf dem Such-Screen). */
@Composable
private fun TargetHeader(
    epochDay: Long,
    selectedMeal: MealType,
    onMealSelected: (MealType) -> Unit
) {
    val date = LocalDate.ofEpochDay(epochDay)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.search_target_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
            style = MaterialTheme.typography.titleSmall
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

@Composable
private fun FoodsList(
    foods: List<FoodItem>,
    onFoodClick: (FoodItem) -> Unit,
    onEdit: (FoodItem) -> Unit,
    onDelete: (FoodItem) -> Unit,
    onToggleFavorite: (FoodItem) -> Unit
) {
    if (foods.isEmpty()) {
        EmptyHint(text = stringResource(R.string.myfoods_empty_foods))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(bottom = Spacing.xxl)
    ) {
        items(foods, key = { it.id }) { food ->
            EntryCard(
                title = food.name,
                subtitle = food.brand?.takeIf { it.isNotBlank() },
                detail = stringResource(
                    R.string.myfoods_kcal_per_100g,
                    food.caloriesPer100g.roundToInt()
                ),
                isFavorite = food.isFavorite,
                onToggleFavorite = { onToggleFavorite(food) },
                onClick = { onFoodClick(food) },
                onEdit = { onEdit(food) },
                onDelete = { onDelete(food) }
            )
        }
    }
}

@Composable
private fun DishesList(
    dishes: List<DishWithIngredients>,
    onDishClick: (DishWithIngredients) -> Unit,
    onEdit: (DishWithIngredients) -> Unit,
    onDelete: (DishWithIngredients) -> Unit
) {
    if (dishes.isEmpty()) {
        EmptyHint(text = stringResource(R.string.myfoods_empty_dishes))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        contentPadding = PaddingValues(bottom = Spacing.xxl)
    ) {
        items(dishes, key = { it.dish.id }) { dish ->
            val perServing = dish.ingredients.totals().perServing(dish.dish.servings)
            EntryCard(
                title = dish.dish.name,
                subtitle = stringResource(
                    R.string.myfoods_ingredient_count,
                    dish.ingredients.size
                ),
                detail = stringResource(
                    R.string.myfoods_per_serving,
                    perServing.kcal.roundToInt()
                ),
                onClick = { onDishClick(dish) },
                onEdit = { onEdit(dish) },
                onDelete = { onDelete(dish) }
            )
        }
    }
}

/** Eine Listen-Karte mit Titel/Untertitel/Detail und Überlauf-Menü (Bearbeiten/Löschen). */
@Composable
private fun EntryCard(
    title: String,
    subtitle: String?,
    detail: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isFavorite: Boolean? = null,
    onToggleFavorite: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.xxs)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isFavorite != null && onToggleFavorite != null) {
                IconButton(onClick = onToggleFavorite) {
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
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.myfoods_more_actions)
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.myfoods_action_edit)) },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Kleiner Dialog: Anzahl Portionen eines Gerichts + Mahlzeit wählen. */
@Composable
private fun AddDishDialog(
    dish: DishWithIngredients,
    initialMeal: MealType,
    onDismiss: () -> Unit,
    onConfirm: (mealType: MealType, servings: Double) -> Unit
) {
    var servings by remember { mutableStateOf("1") }
    var mealType by remember { mutableStateOf(initialMeal) }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()
    val servingsValue = parse(servings)
    val canConfirm = servingsValue != null && servingsValue > 0

    val perServing = dish.ingredients.totals().perServing(dish.dish.servings)
    val kcalTotal = ((servingsValue ?: 0.0) * perServing.kcal).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dish.dish.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = stringResource(R.string.scanner_meal_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = meal == mealType,
                            onClick = { mealType = meal },
                            label = { Text(stringResource(meal.labelRes)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = servings,
                    onValueChange = { servings = it },
                    label = { Text(stringResource(R.string.myfoods_servings_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "$kcalTotal kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { onConfirm(mealType, servingsValue ?: 0.0) }
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Generischer Lösch-Bestätigungsdialog. */
@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
