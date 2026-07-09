package com.fairtrack.app.ui.myfoods

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.UnitFormatter
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import com.fairtrack.app.ui.LocalUnitSystem
import com.fairtrack.app.ui.home.labelRes
import com.fairtrack.app.ui.theme.Spacing
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Dialog zum Eintragen eines eigenen Lebensmittels ins Tagebuch. Zeigt den
 * (unveränderlichen) Namen, lässt Mahlzeit, Einheit und Menge wählen, bietet die
 * hinterlegten Portionen als Schnellauswahl an und zeigt eine Live-Vorschau der
 * Nährwerte für die aktuelle Menge.
 */
@Composable
fun AddToDiaryDialog(
    food: FoodItem,
    portions: List<FoodPortion>,
    initialMeal: MealType,
    onDismiss: () -> Unit,
    onConfirm: (mealType: MealType, amountGrams: Double, unit: MeasureUnit) -> Unit
) {
    // Menge im aktiven Einheitensystem anzeigen/eingeben; gespeichert wird metrisch.
    val unitSystem = LocalUnitSystem.current
    val initialUnit = if (food.isBeverage) MeasureUnit.MILLILITERS else MeasureUnit.GRAMS
    var unit by remember { mutableStateOf(initialUnit) }
    var amount by remember {
        mutableStateOf(
            UnitFormatter.amountNumber(if (food.isBeverage) 330.0 else 100.0, initialUnit, unitSystem)
        )
    }
    var mealType by remember { mutableStateOf(initialMeal) }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()
    fun fmtGrams(g: Double): String = if (g % 1.0 == 0.0) g.toInt().toString() else g.toString()

    val amountValue = parse(amount)
    // Eingabewert (Anzeigeeinheit) -> metrisch für Vorschau und Persistenz.
    val amountMetric = amountValue?.let { UnitFormatter.toMetricAmount(it, unit, unitSystem) }
    val canConfirm = amountMetric != null && amountMetric > 0

    val factor = (amountMetric ?: 0.0) / 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null) },
        title = { Text(food.name) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                food.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                    Text(
                        text = brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

                // Einheit umschaltbar: Gramm für Speisen, Milliliter für Getränke.
                Text(
                    text = stringResource(R.string.unit_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(
                        selected = unit == MeasureUnit.GRAMS,
                        onClick = { unit = MeasureUnit.GRAMS },
                        label = { Text(stringResource(R.string.unit_grams)) }
                    )
                    FilterChip(
                        selected = unit == MeasureUnit.MILLILITERS,
                        onClick = { unit = MeasureUnit.MILLILITERS },
                        label = { Text(stringResource(R.string.unit_milliliters)) }
                    )
                }

                // Portionen als Schnellauswahl (setzen die Menge auf ihren Gramm-Wert).
                if (portions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.edit_food_portions),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        portions.forEach { portion ->
                            FilterChip(
                                selected = amountMetric != null && abs(amountMetric - portion.grams) < 0.5,
                                onClick = {
                                    amount = UnitFormatter.amountNumber(portion.grams, unit, unitSystem)
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.portion_label_format,
                                            portion.label,
                                            fmtGrams(portion.grams)
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = {
                        Text(
                            stringResource(
                                R.string.amount_label_with_unit,
                                UnitFormatter.amountSymbol(unit, unitSystem)
                            )
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                // Live-Vorschau der Nährwerte für die aktuelle Menge.
                Text(
                    text = "${(food.caloriesPer100g * factor).roundToInt()} kcal · " +
                        "${stringResource(R.string.macro_protein)} ${(food.proteinPer100g * factor).roundToInt()} g · " +
                        "${stringResource(R.string.macro_carbs)} ${(food.carbsPer100g * factor).roundToInt()} g · " +
                        "${stringResource(R.string.macro_fat)} ${(food.fatPer100g * factor).roundToInt()} g",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = { onConfirm(mealType, amountMetric ?: 0.0, unit) }
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
