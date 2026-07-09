package com.fairtrack.app.ui.scanner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.QrCodeScanner
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fairtrack.app.R
import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.MicronutrientType
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.UnitFormatter
import com.fairtrack.app.ui.LocalUnitSystem
import com.fairtrack.app.ui.components.ProductThumbnail
import com.fairtrack.app.ui.home.labelRes
import com.fairtrack.app.ui.statistics.formatMicroValue
import com.fairtrack.app.ui.theme.Spacing
import java.time.LocalTime

/** Standard-Mahlzeit nach Tageszeit (bis 10 Frühstück, bis 15 Mittag, bis 21 Abend). */
private fun defaultMealForNow(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 10 -> MealType.BREAKFAST
        hour < 15 -> MealType.LUNCH
        hour < 21 -> MealType.DINNER
        else -> MealType.SNACK
    }
}

/**
 * Bestätigungs-Dialog für ein gescanntes Produkt. Deckt sowohl "gefunden"
 * (vorbefüllte Felder) als auch "nicht gefunden" (leere Felder) ab. Validiert
 * analog zum QuickAddDialog und liefert die Werte pro 100 g zurück.
 */
@Composable
fun ProductConfirmDialog(
    initialName: String = "",
    initialBrand: String? = null,
    initialAmountGrams: String = "100",
    initialCaloriesPer100g: String = "",
    initialProteinPer100g: String = "",
    initialCarbsPer100g: String = "",
    initialFatPer100g: String = "",
    isBeverage: Boolean = false,
    imageUrl: String? = null,
    initialMeal: MealType = defaultMealForNow(),
    /** Mikronährstoffe pro 100 g (read-only, ausklappbar). Leer = keine Angaben. */
    initialMicros: Micronutrients = Micronutrients(),
    onDismiss: () -> Unit,
    onConfirm: (
        mealType: MealType,
        name: String,
        amountGrams: Double,
        kcal: Double,
        protein: Double,
        carbs: Double,
        fat: Double,
        unit: MeasureUnit,
        microsPer100g: Micronutrients
    ) -> Unit
) {
    // Menge im aktiven Einheitensystem anzeigen/eingeben; gespeichert wird metrisch.
    val unitSystem = LocalUnitSystem.current
    var name by remember { mutableStateOf(initialName) }
    var brand by remember { mutableStateOf(initialBrand.orEmpty()) }
    // Getränke werden standardmäßig in Millilitern erfasst.
    val initialUnit = if (isBeverage) MeasureUnit.MILLILITERS else MeasureUnit.GRAMS
    var unit by remember { mutableStateOf(initialUnit) }
    var amount by remember {
        val metric = if (isBeverage) 330.0
        else initialAmountGrams.replace(',', '.').trim().toDoubleOrNull() ?: 100.0
        mutableStateOf(UnitFormatter.amountNumber(metric, initialUnit, unitSystem))
    }
    var kcal by remember { mutableStateOf(initialCaloriesPer100g) }
    var protein by remember { mutableStateOf(initialProteinPer100g) }
    var carbs by remember { mutableStateOf(initialCarbsPer100g) }
    var fat by remember { mutableStateOf(initialFatPer100g) }
    var mealType by remember { mutableStateOf(initialMeal) }
    var microsExpanded by remember { mutableStateOf(false) }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

    val amountValue = parse(amount)
    // Eingabewert (Anzeigeeinheit) -> metrisch für Persistenz.
    val amountMetric = amountValue?.let { UnitFormatter.toMetricAmount(it, unit, unitSystem) }
    val kcalValue = parse(kcal)
    val canConfirm = name.isNotBlank() &&
        amountMetric != null && amountMetric > 0 &&
        kcalValue != null && kcalValue >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
        title = { Text(stringResource(R.string.scanner_confirm_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        ProductThumbnail(imageUrl = imageUrl, size = 96.dp)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.quick_add_name)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text(stringResource(R.string.scanner_brand)) },
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
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
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

                // Mikronährstoffe pro 100 g (read-only, ausklappbar) – nur wenn Angaben da sind.
                if (!initialMicros.isEmpty) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { microsExpanded = !microsExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(R.string.micro_section_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (microsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (microsExpanded) {
                        MicronutrientType.entries.forEach { type ->
                            val value = type.selector(initialMicros)
                            if (value != null && value > 0.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(type.labelRes),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${formatMicroValue(value)} ${stringResource(type.unit.labelRes)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    onConfirm(
                        mealType,
                        name.trim(),
                        amountMetric ?: 0.0,
                        kcalValue ?: 0.0,
                        parse(protein) ?: 0.0,
                        parse(carbs) ?: 0.0,
                        parse(fat) ?: 0.0,
                        unit,
                        initialMicros
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
