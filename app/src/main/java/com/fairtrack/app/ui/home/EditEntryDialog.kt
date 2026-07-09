package com.fairtrack.app.ui.home

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EditNote
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
import androidx.compose.ui.unit.dp
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.UnitFormatter
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.ui.LocalUnitSystem

/** Ergebnis einer Bearbeitung: neue Angaben, Nährwerte pro 100 g. */
data class EditEntryResult(
    val mealType: MealType,
    val name: String,
    val amountGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val unit: MeasureUnit
)

/** Formatiert einen Wert ohne unnötige Nachkommastellen (z. B. "100", "12.5"). */
private fun Double.trimmed(): String =
    if (this % 1.0 == 0.0) toLong().toString() else toString()

/**
 * Dialog zum Bearbeiten eines bestehenden Tagebuch-Eintrags. Die gespeicherten
 * Gesamtwerte werden auf Werte pro 100 g zurückgerechnet und vorbefüllt; beim
 * Bestätigen werden sie mit der neuen Menge wieder zu Gesamtwerten verrechnet.
 */
@Composable
fun EditEntryDialog(
    entry: DiaryEntry,
    onDismiss: () -> Unit,
    onConfirm: (EditEntryResult) -> Unit
) {
    // Rückrechnung Gesamt -> pro 100 g (Menge > 0 durch Erfassung garantiert).
    val factor = if (entry.amountGrams > 0) entry.amountGrams / 100.0 else 1.0

    // Menge im aktiven Einheitensystem anzeigen/eingeben; gespeichert wird metrisch.
    val unitSystem = LocalUnitSystem.current
    var name by remember { mutableStateOf(entry.foodName) }
    var mealType by remember { mutableStateOf(entry.mealType) }
    var unit by remember { mutableStateOf(entry.unit) }
    var amount by remember {
        mutableStateOf(UnitFormatter.amountNumber(entry.amountGrams, entry.unit, unitSystem))
    }
    var kcal by remember { mutableStateOf((entry.calories / factor).trimmed()) }
    var protein by remember { mutableStateOf((entry.protein / factor).trimmed()) }
    var carbs by remember { mutableStateOf((entry.carbs / factor).trimmed()) }
    var fat by remember { mutableStateOf((entry.fat / factor).trimmed()) }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

    val amountValue = parse(amount)
    val amountMetric = amountValue?.let { UnitFormatter.toMetricAmount(it, unit, unitSystem) }
    val kcalValue = parse(kcal)
    val canConfirm = name.isNotBlank() &&
        amountValue != null && amountValue > 0 &&
        kcalValue != null && kcalValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.EditNote, contentDescription = null) },
        title = { Text(stringResource(R.string.edit_entry_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.quick_add_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.scanner_meal_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealType.entries.forEach { meal ->
                        FilterChip(
                            selected = meal == mealType,
                            onClick = { mealType = meal },
                            label = { Text(stringResource(meal.labelRes)) }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                OutlinedTextField(
                    value = kcal,
                    onValueChange = { kcal = it },
                    label = { Text(stringResource(R.string.quick_add_kcal)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = stringResource(R.string.quick_add_optional_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = protein,
                        onValueChange = { protein = it },
                        label = { Text(stringResource(R.string.macro_protein)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = { Text(stringResource(R.string.macro_carbs)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = { Text(stringResource(R.string.macro_fat)) },
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
                        EditEntryResult(
                            mealType = mealType,
                            name = name.trim(),
                            amountGrams = amountMetric ?: 0.0,
                            caloriesPer100g = kcalValue ?: 0.0,
                            proteinPer100g = parse(protein) ?: 0.0,
                            carbsPer100g = parse(carbs) ?: 0.0,
                            fatPer100g = parse(fat) ?: 0.0,
                            unit = unit
                        )
                    )
                }
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
