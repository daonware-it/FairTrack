package com.fairtrack.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.fairtrack.app.ui.LocalUnitSystem

/** Ergebnis eines Quick-Adds: bereits validierte Werte pro 100 g. */
data class QuickAddResult(
    val name: String,
    val amountGrams: Double,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val unit: MeasureUnit
)

/**
 * Einfacher Dialog zum manuellen Erfassen eines Eintrags. Übergangslösung,
 * bis Barcode-Scanner (v0.3.0) und Lebensmittelsuche (v0.4.0) verfügbar sind.
 */
@Composable
fun QuickAddDialog(
    mealType: MealType,
    onDismiss: () -> Unit,
    onConfirm: (QuickAddResult) -> Unit
) {
    // Menge im aktiven Einheitensystem eingeben/anzeigen; gespeichert wird metrisch.
    val unitSystem = LocalUnitSystem.current
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf(MeasureUnit.GRAMS) }
    var amount by remember {
        mutableStateOf(UnitFormatter.amountNumber(100.0, MeasureUnit.GRAMS, unitSystem))
    }
    var kcal by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

    // Eingabewert (Anzeigeeinheit) -> metrisch für Persistenz.
    val amountValue = parse(amount)
    val amountMetric = amountValue?.let { UnitFormatter.toMetricAmount(it, unit, unitSystem) }
    val kcalValue = parse(kcal)
    val canConfirm = name.isNotBlank() &&
        amountValue != null && amountValue > 0 &&
        kcalValue != null && kcalValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Restaurant, contentDescription = null) },
        title = { Text(stringResource(R.string.quick_add_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mahlzeit als Tonal-Pill, damit klar ist, wohin der Eintrag geht.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = stringResource(mealType.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.quick_add_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                // Einheit umschaltbar: Gramm für Speisen, Milliliter für Getränke.
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
                        QuickAddResult(
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
            ) { Text(stringResource(R.string.action_add)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
