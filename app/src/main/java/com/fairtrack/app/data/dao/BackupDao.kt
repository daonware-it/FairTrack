package com.fairtrack.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
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

/**
 * Vollständiger Tabellenzugriff für Export und Import.
 *
 * Eigener DAO statt Erweiterung der bestehenden zehn: Deren Methoden sind auf die
 * Bildschirme zugeschnitten (gefiltert, sortiert, als Flow). Backup braucht das
 * Gegenteil — alles, roh, einmalig.
 */
@Dao
interface BackupDao {

    @Query("SELECT * FROM food_items") suspend fun allFoodItems(): List<FoodItem>
    @Query("SELECT * FROM food_portions") suspend fun allFoodPortions(): List<FoodPortion>
    @Query("SELECT * FROM diary_entries") suspend fun allDiaryEntries(): List<DiaryEntry>
    @Query("SELECT * FROM dishes") suspend fun allDishes(): List<Dish>
    @Query("SELECT * FROM dish_ingredients") suspend fun allDishIngredients(): List<DishIngredient>
    @Query("SELECT * FROM meal_templates") suspend fun allMealTemplates(): List<MealTemplate>
    @Query("SELECT * FROM meal_template_items") suspend fun allMealTemplateItems(): List<MealTemplateItem>
    @Query("SELECT * FROM weight_entries") suspend fun allWeightEntries(): List<WeightEntry>
    @Query("SELECT * FROM body_measurements") suspend fun allBodyMeasurements(): List<BodyMeasurement>
    @Query("SELECT * FROM water_entries") suspend fun allWaterEntries(): List<WaterEntry>
    @Query("SELECT * FROM fasting_sessions") suspend fun allFastingSessions(): List<FastingSession>
    @Query("SELECT * FROM activity_entries") suspend fun allActivityEntries(): List<ActivityEntry>

    @Insert suspend fun insertFoodItems(rows: List<FoodItem>)
    @Insert suspend fun insertFoodPortions(rows: List<FoodPortion>)
    @Insert suspend fun insertDiaryEntries(rows: List<DiaryEntry>)
    @Insert suspend fun insertDishes(rows: List<Dish>)
    @Insert suspend fun insertDishIngredients(rows: List<DishIngredient>)
    @Insert suspend fun insertMealTemplates(rows: List<MealTemplate>)
    @Insert suspend fun insertMealTemplateItems(rows: List<MealTemplateItem>)
    @Insert suspend fun insertWeightEntries(rows: List<WeightEntry>)
    @Insert suspend fun insertBodyMeasurements(rows: List<BodyMeasurement>)
    @Insert suspend fun insertWaterEntries(rows: List<WaterEntry>)
    @Insert suspend fun insertFastingSessions(rows: List<FastingSession>)
    @Insert suspend fun insertActivityEntries(rows: List<ActivityEntry>)

    @Query("DELETE FROM food_items") suspend fun deleteFoodItems()
    @Query("DELETE FROM diary_entries") suspend fun deleteDiaryEntries()
    @Query("DELETE FROM dishes") suspend fun deleteDishes()
    @Query("DELETE FROM meal_templates") suspend fun deleteMealTemplates()
    @Query("DELETE FROM weight_entries") suspend fun deleteWeightEntries()
    @Query("DELETE FROM body_measurements") suspend fun deleteBodyMeasurements()
    @Query("DELETE FROM water_entries") suspend fun deleteWaterEntries()
    @Query("DELETE FROM fasting_sessions") suspend fun deleteFastingSessions()
    @Query("DELETE FROM activity_entries") suspend fun deleteActivityEntries()

    /**
     * Ersetzt den gesamten Tabelleninhalt in einer Transaktion. Schlägt ein Insert
     * fehl, bleibt der alte Stand erhalten — ein halb eingespieltes Backup wäre
     * schlimmer als ein abgebrochener Import.
     *
     * Reihenfolge ist nicht beliebig: Eltern vor Kindern, sonst greifen die
     * Fremdschlüssel. `food_portions`, `dish_ingredients` und `meal_template_items`
     * werden nicht einzeln geleert — das erledigt CASCADE über die Elterntabellen.
     */
    @Transaction
    suspend fun replaceAll(backup: com.fairtrack.app.data.backup.FairTrackBackup) {
        deleteDiaryEntries()
        deleteFoodItems()
        deleteDishes()
        deleteMealTemplates()
        deleteWeightEntries()
        deleteBodyMeasurements()
        deleteWaterEntries()
        deleteFastingSessions()
        deleteActivityEntries()

        insertFoodItems(backup.foodItems)
        insertFoodPortions(backup.foodPortions)
        insertDishes(backup.dishes)
        insertDishIngredients(backup.dishIngredients)
        insertMealTemplates(backup.mealTemplates)
        insertMealTemplateItems(backup.mealTemplateItems)
        insertDiaryEntries(backup.diaryEntries)
        insertWeightEntries(backup.weightEntries)
        insertBodyMeasurements(backup.bodyMeasurements)
        insertWaterEntries(backup.waterEntries)
        insertFastingSessions(backup.fastingSessions)
        insertActivityEntries(backup.activityEntries)
    }
}
