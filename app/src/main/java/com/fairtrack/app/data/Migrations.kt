package com.fairtrack.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room-Migrationen. Erhalten die Nutzerdaten (Tagebuch, Gewicht, eigene
 * Lebensmittel/Gerichte) über Schema-Änderungen hinweg – KEIN destruktiver
 * Wipe mehr bei einem Versions-Sprung. Bei jeder künftigen Schema-Änderung
 * die DB-Version erhöhen UND hier eine passende Migration ergänzen.
 * Die SQL-Statements sind 1:1 aus dem exportierten Room-Schema (app/schemas).
 */

/** v3 → v4: isCustom-Spalte + Tabellen für eigene Lebensmittel & Gerichte (v0.5.0). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isCustom` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `food_portions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`foodItemId` INTEGER NOT NULL, `label` TEXT NOT NULL, `grams` REAL NOT NULL, " +
                "FOREIGN KEY(`foodItemId`) REFERENCES `food_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_food_portions_foodItemId` ON `food_portions` (`foodItemId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dishes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `servings` REAL NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `dish_ingredients` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`dishId` INTEGER NOT NULL, `name` TEXT NOT NULL, `amountGrams` REAL NOT NULL, " +
                "`caloriesPer100g` REAL NOT NULL, `proteinPer100g` REAL NOT NULL, `carbsPer100g` REAL NOT NULL, " +
                "`fatPer100g` REAL NOT NULL, " +
                "FOREIGN KEY(`dishId`) REFERENCES `dishes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_dish_ingredients_dishId` ON `dish_ingredients` (`dishId`)")
    }
}

/** v4 → v5: Produkt-Vorschaubild-URL (Open Food Facts). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `imageUrl` TEXT")
    }
}

/**
 * v5 → v6 (v0.6.0): Favoriten-Flag an Lebensmitteln + Tabellen für
 * Mahlzeiten-Vorlagen (Vorlage + Elemente als Nährwert-Snapshot).
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `food_items` ADD COLUMN `isFavorite` INTEGER NOT NULL DEFAULT 0")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `meal_template_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`templateId` INTEGER NOT NULL, `name` TEXT NOT NULL, `amountGrams` REAL NOT NULL, " +
                "`unit` TEXT NOT NULL, `caloriesPer100g` REAL NOT NULL, `proteinPer100g` REAL NOT NULL, " +
                "`carbsPer100g` REAL NOT NULL, `fatPer100g` REAL NOT NULL, `isBeverage` INTEGER NOT NULL, " +
                "FOREIGN KEY(`templateId`) REFERENCES `meal_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_meal_template_items_templateId` ON `meal_template_items` (`templateId`)"
        )
    }
}

/** v6 → v7 (v0.7.0): Wasser-Tracker – ein aggregierter Datensatz je Tag. */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `water_entries` (`epochDay` INTEGER NOT NULL, " +
                "`amountMl` INTEGER NOT NULL, PRIMARY KEY(`epochDay`))"
        )
    }
}

/**
 * v7 → v8: Mikronährstoff-Tracking (Vitamine/Mineralstoffe). 14 nullable
 * REAL-Spalten je 100 g an `food_items` und als Mengen-Snapshot an
 * `diary_entries` (via @Embedded Micronutrients). Werte in mg/µg.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    private val microColumns = listOf(
        "vitaminA", "vitaminD", "vitaminE", "vitaminC", "vitaminB6", "vitaminB12",
        "folate", "calcium", "iron", "magnesium", "zinc", "potassium",
        "phosphorus", "iodine"
    )

    override fun migrate(db: SupportSQLiteDatabase) {
        for (table in listOf("food_items", "diary_entries")) {
            for (column in microColumns) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` REAL")
            }
        }
    }
}

/**
 * v8 → v9 (v0.12.0): Körpermaße-Tracking – ein aggregierter Datensatz je Tag
 * mit vier nullable Maßen (Taille, Körperfett%, Brust, Arm), metrisch (cm bzw. %).
 */
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `body_measurements` (`epochDay` INTEGER NOT NULL, " +
                "`waistCm` REAL, `bodyFatPercent` REAL, `chestCm` REAL, `armCm` REAL, " +
                "PRIMARY KEY(`epochDay`))"
        )
    }
}

/**
 * v9 → v10 (v0.13.0): Fasten-Verlauf – je Zeile ein abgeschlossenes Fasten-
 * Intervall (Start/Ende in Millis, Zielstunden, Preset-Name). Ein laufendes
 * Fasten liegt nur im DataStore, nicht in dieser Tabelle.
 */
val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fasting_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`startEpochMillis` INTEGER NOT NULL, `endEpochMillis` INTEGER NOT NULL, " +
                "`targetHours` INTEGER NOT NULL, `presetName` TEXT NOT NULL)"
        )
    }
}

/**
 * v10 → v11 (v0.14.0): Health-Connect-Import – ein Bewegungs-Tagesaggregat je
 * Quelle (Schritte + Aktivkalorien). Eindeutiger Index auf (date, source) für
 * den idempotenten Sync-Upsert.
 */
val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `activity_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`date` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `activeKcal` INTEGER NOT NULL, " +
                "`source` TEXT NOT NULL, `lastSyncEpochMillis` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_activity_entries_date_source` " +
                "ON `activity_entries` (`date`, `source`)"
        )
    }
}

/** Alle registrierten Migrationen (chronologisch). */
val ALL_MIGRATIONS = arrayOf(
    MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
    MIGRATION_9_10, MIGRATION_10_11
)
