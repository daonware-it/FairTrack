package com.fairtrack.app.ui.myfoods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.ui.theme.Spacing

/**
 * Editor zum Anlegen/Bearbeiten eines eigenen Lebensmittels: Name, Marke,
 * Nährwerte pro 100 g, Getränk-Umschalter und optionale benannte Portionen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFoodScreen(
    onClose: () -> Unit,
    viewModel: EditFoodViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isNew) R.string.edit_food_new_title
                            else R.string.edit_food_edit_title
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.edit_food_name)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.brand,
                onValueChange = viewModel::setBrand,
                label = { Text(stringResource(R.string.edit_food_brand)) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state.calories,
                onValueChange = viewModel::setCalories,
                label = { Text(stringResource(R.string.quick_add_kcal)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedTextField(
                    value = state.protein,
                    onValueChange = viewModel::setProtein,
                    label = { Text(stringResource(R.string.macro_protein)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.carbs,
                    onValueChange = viewModel::setCarbs,
                    label = { Text(stringResource(R.string.macro_carbs)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = state.fat,
                    onValueChange = viewModel::setFat,
                    label = { Text(stringResource(R.string.macro_fat)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.weight(1f)
                )
            }

            // Getränk-Umschalter: Menge wird später in ml statt g erfasst.
            FilterChip(
                selected = state.isBeverage,
                onClick = { viewModel.setBeverage(!state.isBeverage) },
                label = { Text(stringResource(R.string.edit_food_beverage)) }
            )

            // Portionen-Abschnitt.
            Text(
                text = stringResource(R.string.edit_food_portions),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.edit_food_portions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            state.portions.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = row.label,
                        onValueChange = { viewModel.updatePortion(index, it, row.grams) },
                        label = { Text(stringResource(R.string.edit_food_portion_label)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(2f)
                    )
                    OutlinedTextField(
                        value = row.grams,
                        onValueChange = { viewModel.updatePortion(index, row.label, it) },
                        label = { Text(stringResource(R.string.edit_food_portion_grams)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removePortion(index) }) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = stringResource(R.string.edit_food_remove_portion)
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = viewModel::addPortionRow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = Spacing.sm)
                )
                Text(stringResource(R.string.edit_food_add_portion))
            }

            Button(
                onClick = { viewModel.save(onClose) },
                enabled = viewModel.isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.sm)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
