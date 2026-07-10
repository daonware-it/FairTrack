package com.fairtrack.app.data.backup

import com.fairtrack.app.data.MealType
import com.fairtrack.app.data.MeasureUnit
import com.fairtrack.app.data.Micronutrients
import com.fairtrack.app.data.entity.DiaryEntry
import com.fairtrack.app.data.entity.Dish
import com.fairtrack.app.data.entity.DishIngredient
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.entity.WeightEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Hält das Dateiformat der Sicherung fest.
 *
 * Die Entities werden direkt serialisiert, ihre Feldnamen sind damit öffentlicher
 * Vertrag: Eine Umbenennung liest sich aus alten Sicherungen still als Default
 * zurück — der Nutzer verlöre Daten, ohne dass irgendetwas fehlschlägt. Dieser
 * Test scheitert stattdessen laut. Wer ihn anpasst, muss [BACKUP_FORMAT_VERSION]
 * erhöhen und den Import um eine Migration ergänzen.
 */
class BackupFormatTest {

    private fun emptyBackup() = FairTrackBackup(
        formatVersion = BACKUP_FORMAT_VERSION,
        schemaVersion = 11,
        appVersion = "1.0.1",
        createdAtEpochMillis = 0L
    )

    private val plainJson = Json { encodeDefaults = true }

    @Test
    fun `envelope carries the fields the importer keys on`() {
        val obj = plainJson.encodeToJsonElement(FairTrackBackup.serializer(), emptyBackup()).jsonObject

        assertTrue("formatVersion" in obj.keys)
        assertTrue("schemaVersion" in obj.keys)
        assertTrue("appVersion" in obj.keys)
        assertTrue("createdAtEpochMillis" in obj.keys)
    }

    @Test
    fun `every table has a slot in the envelope`() {
        val obj = plainJson.encodeToJsonElement(FairTrackBackup.serializer(), emptyBackup()).jsonObject

        val tables = setOf(
            "foodItems", "foodPortions", "diaryEntries", "dishes", "dishIngredients",
            "mealTemplates", "mealTemplateItems", "weightEntries", "bodyMeasurements",
            "waterEntries", "fastingSessions", "activityEntries"
        )
        assertTrue(
            "Fehlende Tabellen im Backup: ${tables - obj.keys}",
            obj.keys.containsAll(tables)
        )
    }

    @Test
    fun `food item field names are frozen`() {
        val item = FoodItem(
            id = 1, name = "Hafer", caloriesPer100g = 370.0, proteinPer100g = 13.0,
            carbsPer100g = 59.0, fatPer100g = 7.0
        )
        val keys = plainJson.encodeToJsonElement(FoodItem.serializer(), item).jsonObject.keys

        assertEquals(
            setOf(
                "id", "name", "brand", "barcode", "caloriesPer100g", "proteinPer100g",
                "carbsPer100g", "fatPer100g", "isBeverage", "isCustom", "imageUrl",
                "isFavorite", "micros"
            ),
            keys
        )
    }

    @Test
    fun `diary entry field names are frozen`() {
        val entry = DiaryEntry(
            id = 1, epochDay = 20_000, mealType = MealType.LUNCH, foodName = "Hafer",
            amountGrams = 50.0, calories = 185.0, protein = 6.5, carbs = 29.5, fat = 3.5
        )
        val keys = plainJson.encodeToJsonElement(DiaryEntry.serializer(), entry).jsonObject.keys

        assertEquals(
            setOf(
                "id", "epochDay", "mealType", "foodName", "amountGrams", "unit",
                "calories", "protein", "carbs", "fat", "micros"
            ),
            keys
        )
    }

    @Test
    fun `micronutrient field names are frozen`() {
        val keys = plainJson
            .encodeToJsonElement(Micronutrients.serializer(), Micronutrients())
            .jsonObject.keys

        assertEquals(
            setOf(
                "vitaminA", "vitaminD", "vitaminE", "vitaminC", "vitaminB6", "vitaminB12",
                "folate", "calcium", "iron", "magnesium", "zinc", "potassium",
                "phosphorus", "iodine"
            ),
            keys
        )
    }

    @Test
    fun `enums serialize by name, not by ordinal`() {
        val entry = DiaryEntry(
            epochDay = 1, mealType = MealType.SNACK, foodName = "x", amountGrams = 1.0,
            unit = MeasureUnit.MILLILITERS, calories = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0
        )
        val obj = plainJson.encodeToJsonElement(DiaryEntry.serializer(), entry).jsonObject

        // Ordinals wären beim Einfügen eines neuen Enum-Werts stillschweigend falsch.
        assertEquals("\"SNACK\"", obj["mealType"].toString())
        assertEquals("\"MILLILITERS\"", obj["unit"].toString())
    }

    @Test
    fun `round trip preserves ids and foreign keys`() {
        val backup = emptyBackup().copy(
            dishes = listOf(Dish(id = 7, name = "Porridge", servings = 2.0)),
            dishIngredients = listOf(
                DishIngredient(
                    id = 3, dishId = 7, name = "Hafer", amountGrams = 80.0,
                    caloriesPer100g = 370.0, proteinPer100g = 13.0,
                    carbsPer100g = 59.0, fatPer100g = 7.0
                )
            ),
            weightEntries = listOf(WeightEntry(epochDay = 20_000, weightKg = 74.5))
        )

        val encoded = plainJson.encodeToString(FairTrackBackup.serializer(), backup)
        val decoded = plainJson.decodeFromString(FairTrackBackup.serializer(), encoded)

        assertEquals(backup, decoded)
        // Der Fremdschlüssel muss die Runde überleben, sonst hängt die Zutat am Nichts.
        assertEquals(decoded.dishes.single().id, decoded.dishIngredients.single().dishId)
    }

    @Test
    fun `older backups load when new fields have defaults`() {
        // Eine Sicherung aus einer Version ohne Mikronährstoffe und ohne "unit".
        val old = """
            {
              "formatVersion": 1,
              "schemaVersion": 5,
              "appVersion": "0.9.0",
              "createdAtEpochMillis": 1,
              "diaryEntries": [
                {"id":1,"epochDay":20000,"mealType":"BREAKFAST","foodName":"Hafer",
                 "amountGrams":50.0,"calories":185.0,"protein":6.5,"carbs":29.5,"fat":3.5}
              ]
            }
        """.trimIndent()

        val tolerant = Json { ignoreUnknownKeys = true }
        val decoded = tolerant.decodeFromString(FairTrackBackup.serializer(), old)

        val entry = decoded.diaryEntries.single()
        assertEquals(MeasureUnit.GRAMS, entry.unit)
        assertEquals(Micronutrients(), entry.micros)
        assertEquals("Hafer", entry.foodName)
    }

    @Test
    fun `unknown fields from newer backups do not break parsing`() {
        val future = """
            {"formatVersion":1,"schemaVersion":99,"appVersion":"9.9.9",
             "createdAtEpochMillis":1,"somethingNew":{"a":1},"weightEntries":[]}
        """.trimIndent()

        val tolerant = Json { ignoreUnknownKeys = true }
        val decoded = tolerant.decodeFromString(FairTrackBackup.serializer(), future)

        assertEquals(99, decoded.schemaVersion)
    }

    @Test
    fun `profile round trips through its dto`() {
        val profile = com.fairtrack.app.data.UserProfile(
            weightKg = 74.5,
            heightCm = 180.0,
            age = 30,
            sex = com.fairtrack.app.data.Sex.MALE,
            activity = com.fairtrack.app.data.ActivityLevel.MODERATE,
            goal = com.fairtrack.app.data.WeightGoal.LOSE,
            targetWeightKg = 70.0
        )

        assertEquals(profile, profile.toBackup().toProfile())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown enum value in profile fails loudly`() {
        BackupProfile(
            weightKg = 70.0, heightCm = 180.0, age = 30,
            sex = "ROBOT", activity = "MODERATE", goal = "LOSE"
        ).toProfile()
    }
}
