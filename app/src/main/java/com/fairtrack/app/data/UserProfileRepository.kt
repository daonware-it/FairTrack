package com.fairtrack.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPrefsDataStore by preferencesDataStore(name = "user_prefs")

private val weightKgKey = doublePreferencesKey("weight_kg")
private val heightCmKey = doublePreferencesKey("height_cm")
private val ageKey = intPreferencesKey("age")
private val sexKey = stringPreferencesKey("sex")
private val activityKey = stringPreferencesKey("activity")
private val goalKey = stringPreferencesKey("goal")
private val onboardingCompleteKey = booleanPreferencesKey("onboarding_complete")
private val targetWeightKgKey = doublePreferencesKey("target_weight_kg")
private val manualCaloriesKey = intPreferencesKey("manual_calories")
private val manualProteinKey = intPreferencesKey("manual_protein")
private val manualCarbsKey = intPreferencesKey("manual_carbs")
private val manualFatKey = intPreferencesKey("manual_fat")

@Singleton
class UserProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Das persistierte Nutzerprofil oder `null`, solange das Onboarding nicht
     * abgeschlossen ist bzw. Pflichtfelder fehlen oder ungültig sind.
     */
    val profile: Flow<UserProfile?> = context.userPrefsDataStore.data.map { prefs ->
        if (prefs[onboardingCompleteKey] != true) return@map null

        val weightKg = prefs[weightKgKey] ?: return@map null
        val heightCm = prefs[heightCmKey] ?: return@map null
        val age = prefs[ageKey] ?: return@map null
        val sexRaw = prefs[sexKey] ?: return@map null
        val activityRaw = prefs[activityKey] ?: return@map null
        val goalRaw = prefs[goalKey] ?: return@map null

        val sex = try {
            enumValueOf<Sex>(sexRaw)
        } catch (e: IllegalArgumentException) {
            return@map null
        }
        val activity = try {
            enumValueOf<ActivityLevel>(activityRaw)
        } catch (e: IllegalArgumentException) {
            return@map null
        }
        val goal = try {
            enumValueOf<WeightGoal>(goalRaw)
        } catch (e: IllegalArgumentException) {
            return@map null
        }

        UserProfile(
            weightKg, heightCm, age, sex, activity, goal,
            targetWeightKg = prefs[targetWeightKgKey],
            manualCalories = prefs[manualCaloriesKey],
            manualProteinGrams = prefs[manualProteinKey],
            manualCarbsGrams = prefs[manualCarbsKey],
            manualFatGrams = prefs[manualFatKey]
        )
    }

    /** Ziel-Nährwerte aus dem Profil, oder [NutritionGoals.DEFAULT] ohne Profil. */
    val goals: Flow<NutritionGoals> = profile.map { it?.toNutritionGoals() ?: NutritionGoals.DEFAULT }

    /** Ob das Onboarding abgeschlossen wurde. */
    val isOnboardingComplete: Flow<Boolean> =
        context.userPrefsDataStore.data.map { it[onboardingCompleteKey] ?: false }

    /** Persistiert das Profil und markiert das Onboarding als abgeschlossen. */
    suspend fun saveProfile(profile: UserProfile) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[weightKgKey] = profile.weightKg
            prefs[heightCmKey] = profile.heightCm
            prefs[ageKey] = profile.age
            prefs[sexKey] = profile.sex.name
            prefs[activityKey] = profile.activity.name
            prefs[goalKey] = profile.goal.name
            prefs[onboardingCompleteKey] = true
            profile.targetWeightKg?.let { prefs[targetWeightKgKey] = it } ?: prefs.remove(targetWeightKgKey)
            profile.manualCalories?.let { prefs[manualCaloriesKey] = it } ?: prefs.remove(manualCaloriesKey)
            profile.manualProteinGrams?.let { prefs[manualProteinKey] = it } ?: prefs.remove(manualProteinKey)
            profile.manualCarbsGrams?.let { prefs[manualCarbsKey] = it } ?: prefs.remove(manualCarbsKey)
            profile.manualFatGrams?.let { prefs[manualFatKey] = it } ?: prefs.remove(manualFatKey)
        }
    }

    /** Aktualisiert nur das aktuelle Gewicht (z. B. nach einem Gewichtseintrag). */
    suspend fun updateCurrentWeight(weightKg: Double) {
        context.userPrefsDataStore.edit { prefs ->
            prefs[weightKgKey] = weightKg
        }
    }

    /**
     * Löscht das gesamte Profil inkl. Onboarding-Flag (für "Daten löschen",
     * v0.11.0). Danach startet die App wieder mit dem Onboarding.
     */
    suspend fun clear() {
        context.userPrefsDataStore.edit { prefs -> prefs.clear() }
    }
}
