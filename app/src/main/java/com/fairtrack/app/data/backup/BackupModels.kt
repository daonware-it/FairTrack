package com.fairtrack.app.data.backup

import com.fairtrack.app.data.ActivityLevel
import com.fairtrack.app.data.Sex
import com.fairtrack.app.data.UserProfile
import com.fairtrack.app.data.WeightGoal
import com.fairtrack.app.data.entity.ActivityEntry
import com.fairtrack.app.data.entity.BodyMeasurement
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.FastingSession
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.FoodPortion
import com.fairtrack.app.data.entity.MealTemplate
import com.fairtrack.app.data.entity.MealTemplateItem
import com.fairtrack.app.data.entity.WaterEntry
import com.fairtrack.app.data.entity.WeightEntry
import kotlinx.serialization.Serializable

/**
 * Serialisierte Sicherung sämtlicher Nutzerdaten.
 *
 * ACHTUNG — Formatvertrag: Die Entities werden direkt serialisiert, ihre
 * Feldnamen sind damit Teil des Dateiformats. Ein umbenanntes Feld liest sich
 * aus alten Sicherungen still als Default zurück, statt zu scheitern. Wer eine
 * Entity umbenennt, muss [BACKUP_FORMAT_VERSION] erhöhen und den Import um eine
 * Migration ergänzen. `BackupFormatTest` hält die Feldnamen fest, damit ein
 * Rename nicht unbemerkt durchrutscht.
 *
 * [schemaVersion] ist die Room-Version zum Zeitpunkt des Exports. Sie ist
 * informativ: Der Import verlässt sich auf [formatVersion], nicht auf sie.
 */
@Serializable
data class FairTrackBackup(
    val formatVersion: Int,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAtEpochMillis: Long,
    val profile: BackupProfile? = null,
    val foodItems: List<FoodItem> = emptyList(),
    val foodPortions: List<FoodPortion> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val dishes: List<Dish> = emptyList(),
    val dishIngredients: List<DishIngredient> = emptyList(),
    val mealTemplates: List<MealTemplate> = emptyList(),
    val mealTemplateItems: List<MealTemplateItem> = emptyList(),
    val weightEntries: List<WeightEntry> = emptyList(),
    val bodyMeasurements: List<BodyMeasurement> = emptyList(),
    val waterEntries: List<WaterEntry> = emptyList(),
    val fastingSessions: List<FastingSession> = emptyList(),
    val activityEntries: List<ActivityEntry> = emptyList()
)

/**
 * Das Nutzerprofil aus dem `user_prefs`-DataStore. Bewusst ein eigenes DTO statt
 * [UserProfile]: Das Profil ist eine Domänenklasse, deren Enums sich unabhängig
 * vom Dateiformat entwickeln dürfen. Unbekannte Enum-Werte lassen den Import
 * scheitern — das ist gewollt, ein halbes Profil wäre schlechter als keins.
 */
@Serializable
data class BackupProfile(
    val weightKg: Double,
    val heightCm: Double,
    val age: Int,
    val sex: String,
    val activity: String,
    val goal: String,
    val targetWeightKg: Double? = null,
    val manualCalories: Int? = null,
    val manualProteinGrams: Int? = null,
    val manualCarbsGrams: Int? = null,
    val manualFatGrams: Int? = null
)

/**
 * Version des Dateiformats — unabhängig von der Room-Schema-Version, weil nicht
 * jede Schema-Migration das Format bricht (eine neue Spalte mit Default etwa
 * nicht). Erhöhen, sobald alte Sicherungen nicht mehr verlustfrei einlesbar sind.
 */
const val BACKUP_FORMAT_VERSION = 1

fun UserProfile.toBackup() = BackupProfile(
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    sex = sex.name,
    activity = activity.name,
    goal = goal.name,
    targetWeightKg = targetWeightKg,
    manualCalories = manualCalories,
    manualProteinGrams = manualProteinGrams,
    manualCarbsGrams = manualCarbsGrams,
    manualFatGrams = manualFatGrams
)

/** @throws IllegalArgumentException bei unbekanntem Enum-Wert. */
fun BackupProfile.toProfile() = UserProfile(
    weightKg = weightKg,
    heightCm = heightCm,
    age = age,
    sex = enumValueOf<Sex>(sex),
    activity = enumValueOf<ActivityLevel>(activity),
    goal = enumValueOf<WeightGoal>(goal),
    targetWeightKg = targetWeightKg,
    manualCalories = manualCalories,
    manualProteinGrams = manualProteinGrams,
    manualCarbsGrams = manualCarbsGrams,
    manualFatGrams = manualFatGrams
)
