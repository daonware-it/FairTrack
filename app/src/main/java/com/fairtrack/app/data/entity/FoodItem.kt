package com.fairtrack.app.data.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fairtrack.app.data.Micronutrients
import kotlinx.serialization.Serializable

/**
 * Ein Lebensmittel mit Nährwerten pro 100 g.
 * Quelle kann später Open Food Facts, Barcode oder manuelle Eingabe sein.
 */
@Entity(tableName = "food_items")
@Serializable
data class FoodItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    /** Getränk → Menge wird in ml statt g erfasst. */
    val isBeverage: Boolean = false,
    /** true = vom Nutzer selbst angelegtes Lebensmittel, false = aus OFF gecachtes Produkt. */
    val isCustom: Boolean = false,
    /** URL zum Produkt-Vorschaubild (Open Food Facts), null wenn keins vorhanden. */
    val imageUrl: String? = null,
    /** true = vom Nutzer als Favorit markiert (Schnellzugriff im Such-Tab). */
    val isFavorite: Boolean = false,
    /**
     * Mikronährstoffe (Vitamine/Mineralstoffe) pro 100 g. Nullable Spalten, da die
     * Abdeckung lückenhaft ist (OFF nur bei angereicherten Produkten, BLS-Katalog
     * für Basislebensmittel). Als eigene Spalten via @Embedded.
     */
    @Embedded val micros: Micronutrients = Micronutrients()
)
