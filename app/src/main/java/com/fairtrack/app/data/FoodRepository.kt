package com.fairtrack.app.data

import com.fairtrack.app.data.dao.FoodItemDao
import com.fairtrack.app.data.entity.FoodItem
import com.fairtrack.app.data.network.OpenFoodFactsApi
import com.fairtrack.app.data.network.ProductDto
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Ergebnis einer Barcode-Suche.
 */
sealed interface ProductLookupResult {
    data class Found(val item: FoodItem) : ProductLookupResult
    data class NotFound(val barcode: String) : ProductLookupResult
    data object NetworkError : ProductLookupResult
}

/**
 * Ergebnis einer Volltextsuche.
 */
sealed interface SearchOutcome {
    data class Success(val items: List<FoodItem>) : SearchOutcome
    data object Empty : SearchOutcome
    data object NetworkError : SearchOutcome
}

/**
 * Zugriff auf Lebensmittel: erst lokaler Cache (Room), dann Open Food Facts.
 */
@Singleton
class FoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val api: OpenFoodFactsApi
) {

    /**
     * Sucht ein Produkt anhand des Barcodes.
     * Cache-first: gefundene Produkte werden lokal gespeichert.
     */
    suspend fun lookupByBarcode(barcode: String): ProductLookupResult {
        foodItemDao.findByBarcode(barcode)?.let { return ProductLookupResult.Found(it) }

        return try {
            val resp = api.getProduct(barcode)
            val item = resp.product?.toFoodItem(barcode)
            if (resp.status == 1 && item != null) {
                foodItemDao.upsert(item)
                ProductLookupResult.Found(item)
            } else {
                ProductLookupResult.NotFound(barcode)
            }
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404) ProductLookupResult.NotFound(barcode) else ProductLookupResult.NetworkError
        } catch (e: java.io.IOException) {
            ProductLookupResult.NetworkError
        }
    }

    /**
     * Sucht Produkte per Volltext über Open Food Facts.
     */
    suspend fun searchByText(query: String): SearchOutcome {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return SearchOutcome.Empty
        return try {
            val response = api.search(terms = trimmed)
            val items = response.products.mapNotNull { it.toFoodItem(it.code) }
            // Treffer mit Barcode best effort cachen (offline-Fallback für Scanner)
            items.filter { !it.barcode.isNullOrBlank() }.forEach { runCatching { foodItemDao.upsert(it) } }
            if (items.isEmpty()) SearchOutcome.Empty else SearchOutcome.Success(items)
        } catch (e: java.io.IOException) {
            SearchOutcome.NetworkError
        } catch (e: retrofit2.HttpException) {
            SearchOutcome.NetworkError
        } catch (e: kotlinx.serialization.SerializationException) {
            // Unerwartetes JSON-Schema soll die App nicht abstürzen lassen.
            SearchOutcome.NetworkError
        }
    }

    /**
     * Wandelt ein OFF-Produkt in ein [FoodItem] um.
     * Ohne Kalorienangabe kann kein sinnvoller Eintrag erzeugt werden -> null.
     */
    private fun ProductDto.toFoodItem(barcode: String?): FoodItem? {
        val name = productNameDe?.takeIf { it.isNotBlank() }
            ?: productName?.takeIf { it.isNotBlank() }
            ?: "Unbekanntes Produkt"
        val kcal = nutriments?.energyKcal100g ?: return null
        // Getränke erkennt Open Food Facts über die Kategorie-Tags.
        val isBeverage = categoriesTags?.any {
            it == "en:beverages" || it.startsWith("en:beverages")
        } == true
        return FoodItem(
            name = name,
            brand = brands?.takeIf { it.isNotBlank() }?.substringBefore(',')?.trim(),
            barcode = barcode,
            caloriesPer100g = kcal,
            proteinPer100g = nutriments.proteins100g ?: 0.0,
            carbsPer100g = nutriments.carbohydrates100g ?: 0.0,
            fatPer100g = nutriments.fat100g ?: 0.0,
            isBeverage = isBeverage,
            imageUrl = imageFrontSmallUrl?.takeIf { it.isNotBlank() }
                ?: imageSmallUrl?.takeIf { it.isNotBlank() },
            micros = nutriments.toMicronutrients()
        )
    }
}
