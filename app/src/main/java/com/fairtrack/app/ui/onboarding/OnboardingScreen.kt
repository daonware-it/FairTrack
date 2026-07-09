package com.fairtrack.app.ui.onboarding

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fairtrack.app.R
import com.fairtrack.app.data.ActivityLevel
import com.fairtrack.app.data.BodyMetrics
import com.fairtrack.app.data.Sex
import com.fairtrack.app.data.UserProfile
import com.fairtrack.app.data.WeightGoal
import com.fairtrack.app.data.toNutritionGoals
import com.fairtrack.app.ui.theme.Spacing
import kotlin.math.roundToInt

/** Anzeige-Label für [Sex]. */
private val Sex.labelRes: Int
    get() = when (this) {
        Sex.MALE -> R.string.sex_male
        Sex.FEMALE -> R.string.sex_female
    }

/** Anzeige-Label für [ActivityLevel]. */
private val ActivityLevel.labelRes: Int
    get() = when (this) {
        ActivityLevel.SEDENTARY -> R.string.activity_sedentary
        ActivityLevel.LIGHT -> R.string.activity_light
        ActivityLevel.MODERATE -> R.string.activity_moderate
        ActivityLevel.ACTIVE -> R.string.activity_active
        ActivityLevel.VERY_ACTIVE -> R.string.activity_very_active
    }

/** Anzeige-Label für [WeightGoal]. */
private val WeightGoal.labelRes: Int
    get() = when (this) {
        WeightGoal.LOSE -> R.string.goal_lose
        WeightGoal.MAINTAIN -> R.string.goal_maintain
        WeightGoal.GAIN -> R.string.goal_gain
    }

/** WHO-Kategorie-Label für einen BMI-Wert. */
private fun bmiCategoryRes(bmi: Double): Int = when {
    bmi < BodyMetrics.MIN_HEALTHY_BMI -> R.string.bmi_underweight
    bmi < 25.0 -> R.string.bmi_normal
    bmi < 30.0 -> R.string.bmi_overweight
    else -> R.string.bmi_obese
}

// Plausible Eingabebereiche — alles außerhalb wird als Fehler markiert.
private val WEIGHT_RANGE_KG = 30.0..300.0
private val HEIGHT_RANGE_CM = 100.0..250.0
private val AGE_RANGE_YEARS = 10..120
private val MANUAL_CALORIES_RANGE = 1200..10000

/** Lässt nur Ziffern und höchstens ein Dezimaltrennzeichen (Komma/Punkt) durch. */
private fun sanitizeDecimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
    val sepIndex = filtered.indexOfFirst { it == ',' || it == '.' }
    return if (sepIndex == -1) {
        filtered
    } else {
        filtered.take(sepIndex + 1) + filtered.drop(sepIndex + 1).filter { it.isDigit() }
    }
}

/** Lässt nur Ziffern durch. */
private fun sanitizeInt(input: String): String = input.filter { it.isDigit() }.take(5)

/**
 * Onboarding-Screen (WP-2) zur Erfassung des Nutzerprofils. Dient sowohl dem
 * Erststart (leere Felder mit sinnvollen Chip-Defaults) als auch der späteren
 * Bearbeitung (Prefill aus dem gespeicherten Profil). Zeigt eine Live-Vorschau
 * der berechneten Tagesziele, sobald alle Pflichtfelder gültig sind, und ruft
 * [onDone] nach erfolgreichem Speichern auf.
 */
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.finished.collect { onDone() }
    }

    val existing by viewModel.existingProfile.collectAsStateWithLifecycle()

    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var activity by remember { mutableStateOf(ActivityLevel.MODERATE) }
    var goal by remember { mutableStateOf(WeightGoal.MAINTAIN) }
    var targetWeight by remember { mutableStateOf("") }
    var manualCalories by remember { mutableStateOf("") }
    var manualProtein by remember { mutableStateOf("") }
    var manualCarbs by remember { mutableStateOf("") }
    var manualFat by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }

    // Einmaliger Prefill, sobald das gespeicherte Profil eintrifft.
    LaunchedEffect(existing) {
        val profile = existing
        if (profile != null && !prefilled) {
            weight = profile.weightKg.toString()
            height = profile.heightCm.toString()
            age = profile.age.toString()
            sex = profile.sex
            activity = profile.activity
            goal = profile.goal
            targetWeight = profile.targetWeightKg?.toString() ?: ""
            manualCalories = profile.manualCalories?.toString() ?: ""
            manualProtein = profile.manualProteinGrams?.toString() ?: ""
            manualCarbs = profile.manualCarbsGrams?.toString() ?: ""
            manualFat = profile.manualFatGrams?.toString() ?: ""
            prefilled = true
        }
    }

    fun parse(value: String): Double? = value.replace(',', '.').trim().toDoubleOrNull()

    val weightValue = parse(weight)
    val heightValue = parse(height)
    val ageValue = age.trim().toIntOrNull()
    val targetWeightValue = parse(targetWeight)
    val manualCaloriesValue = manualCalories.trim().toIntOrNull()
    val manualProteinValue = manualProtein.trim().toIntOrNull()
    val manualCarbsValue = manualCarbs.trim().toIntOrNull()
    val manualFatValue = manualFat.trim().toIntOrNull()

    // Plausibilitätsprüfung: nicht-leere Eingaben müssen im realistischen Bereich liegen.
    val weightError = weight.isNotBlank() &&
        (weightValue == null || weightValue !in WEIGHT_RANGE_KG)
    val heightError = height.isNotBlank() &&
        (heightValue == null || heightValue !in HEIGHT_RANGE_CM)
    val ageError = age.isNotBlank() &&
        (ageValue == null || ageValue !in AGE_RANGE_YEARS)
    val targetRangeError = targetWeight.isNotBlank() &&
        (targetWeightValue == null || targetWeightValue !in WEIGHT_RANGE_KG)
    val manualCaloriesError = manualCalories.isNotBlank() &&
        (manualCaloriesValue == null || manualCaloriesValue !in MANUAL_CALORIES_RANGE)

    // Zielgewichte unter dem minimal gesunden Gewicht (BMI 18,5) werden blockiert,
    // damit sich niemand gesundheitsschädliche Ziele setzen kann.
    val minHealthyWeight = heightValue?.takeIf { !heightError && height.isNotBlank() }
        ?.let { BodyMetrics.minHealthyWeightKg(it) }
    val targetTooLow = targetWeightValue != null && !targetRangeError &&
        minHealthyWeight != null && targetWeightValue < minHealthyWeight

    val isValid = weightValue != null && !weightError &&
        heightValue != null && !heightError &&
        ageValue != null && !ageError &&
        !targetRangeError && !targetTooLow && !manualCaloriesError

    val profile = if (isValid) {
        UserProfile(
            weightKg = weightValue,
            heightCm = heightValue,
            age = ageValue,
            sex = sex,
            activity = activity,
            goal = goal,
            targetWeightKg = targetWeightValue,
            manualCalories = manualCaloriesValue,
            manualProteinGrams = manualProteinValue,
            manualCarbsGrams = manualCarbsValue,
            manualFatGrams = manualFatValue
        )
    } else {
        null
    }

    val isEdit = existing != null

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(modifier)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.xs))

            OutlinedTextField(
                value = weight,
                onValueChange = { weight = sanitizeDecimal(it) },
                label = { Text(stringResource(R.string.onboarding_weight)) },
                singleLine = true,
                isError = weightError,
                supportingText = if (weightError) {
                    { Text(stringResource(R.string.onboarding_weight_range)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = height,
                onValueChange = { height = sanitizeDecimal(it) },
                label = { Text(stringResource(R.string.onboarding_height)) },
                singleLine = true,
                isError = heightError,
                supportingText = if (heightError) {
                    { Text(stringResource(R.string.onboarding_height_range)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = age,
                onValueChange = { age = sanitizeInt(it) },
                label = { Text(stringResource(R.string.onboarding_age)) },
                singleLine = true,
                isError = ageError,
                supportingText = if (ageError) {
                    { Text(stringResource(R.string.onboarding_age_range)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            // BMI + gesunder Gewichtsbereich, sobald Gewicht/Größe/Alter gültig sind
            if (weightValue != null && !weightError &&
                heightValue != null && !heightError &&
                ageValue != null && !ageError
            ) {
                val bmi = BodyMetrics.bmi(weightValue, heightValue)
                val healthyRange = BodyMetrics.desirableWeightRangeKg(heightValue, ageValue)
                val optimalWeight = BodyMetrics.optimalWeightKg(heightValue, ageValue)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.onboarding_bmi,
                                String.format("%.1f", bmi),
                                stringResource(bmiCategoryRes(bmi))
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.onboarding_bmi_detail,
                                healthyRange.start.roundToInt(),
                                healthyRange.endInclusive.roundToInt(),
                                optimalWeight.roundToInt()
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Geschlecht
            Text(
                text = stringResource(R.string.onboarding_sex),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Sex.entries.forEach { option ->
                    FilterChip(
                        selected = option == sex,
                        onClick = { sex = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // Aktivitätslevel (horizontal scrollbar, da fünf Optionen)
            Text(
                text = stringResource(R.string.onboarding_activity),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                ActivityLevel.entries.forEach { option ->
                    FilterChip(
                        selected = option == activity,
                        onClick = { activity = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // Ziel
            Text(
                text = stringResource(R.string.onboarding_goal),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                WeightGoal.entries.forEach { option ->
                    FilterChip(
                        selected = option == goal,
                        onClick = { goal = option },
                        label = { Text(stringResource(option.labelRes)) }
                    )
                }
            }

            // Zielgewicht (optional, aber realistisch und nie unter dem minimal gesunden Gewicht)
            OutlinedTextField(
                value = targetWeight,
                onValueChange = { targetWeight = sanitizeDecimal(it) },
                label = { Text(stringResource(R.string.onboarding_target_weight)) },
                singleLine = true,
                isError = targetRangeError || targetTooLow,
                supportingText = when {
                    targetRangeError -> {
                        { Text(stringResource(R.string.onboarding_weight_range)) }
                    }
                    targetTooLow -> {
                        {
                            Text(
                                stringResource(
                                    R.string.onboarding_target_weight_too_low,
                                    minHealthyWeight.roundToInt()
                                )
                            )
                        }
                    }
                    else -> null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            // Live-Vorschau der berechneten Tagesziele
            if (profile != null) {
                val goals = profile.toNutritionGoals()
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_result,
                            goals.calories,
                            goals.proteinGrams,
                            goals.carbsGrams,
                            goals.fatGrams
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }

            // Manuelle Ziel-Overrides (optional)
            Text(
                text = stringResource(R.string.onboarding_manual_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.onboarding_manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = manualCalories,
                onValueChange = { manualCalories = sanitizeInt(it) },
                label = { Text(stringResource(R.string.onboarding_manual_calories)) },
                singleLine = true,
                isError = manualCaloriesError,
                supportingText = if (manualCaloriesError) {
                    { Text(stringResource(R.string.onboarding_manual_calories_range)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualProtein,
                onValueChange = { manualProtein = sanitizeInt(it) },
                label = { Text(stringResource(R.string.onboarding_manual_protein)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualCarbs,
                onValueChange = { manualCarbs = sanitizeInt(it) },
                label = { Text(stringResource(R.string.onboarding_manual_carbs)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = manualFat,
                onValueChange = { manualFat = sanitizeInt(it) },
                label = { Text(stringResource(R.string.onboarding_manual_fat)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.sm))

            Button(
                onClick = { profile?.let { viewModel.save(it) } },
                enabled = profile != null,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(
                        if (isEdit) R.string.onboarding_save else R.string.onboarding_finish
                    )
                )
            }
        }
    }
}
