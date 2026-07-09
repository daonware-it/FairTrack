package com.fairtrack.app.data

import com.fairtrack.app.data.dao.FoodItemDao
import com.fairtrack.app.data.entity.FoodItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Zugriff auf als Favorit markierte Lebensmittel (v0.6.0). Favoriten sind
 * [FoodItem]-Zeilen mit `isFavorite = 1` – das können eigene Lebensmittel oder
 * aus Open Food Facts gecachte Produkte sein.
 */
@Singleton
class FavoritesRepository @Inject constructor(private val foodItemDao: FoodItemDao) {

    fun observeFavorites(): Flow<List<FoodItem>> = foodItemDao.observeFavorites()

    suspend fun setFavorite(id: Long, favorite: Boolean) = foodItemDao.setFavorite(id, favorite)

    /**
     * Schaltet den Favoriten-Status eines Treffers um. Suchtreffer sind nicht
     * zwingend persistiert (id == 0); dann wird per Barcode eine vorhandene
     * Zeile gesucht bzw. der Treffer neu gespeichert und direkt favorisiert.
     */
    suspend fun toggleFavorite(item: FoodItem) {
        val existing = when {
            item.id != 0L -> foodItemDao.findById(item.id)
            !item.barcode.isNullOrBlank() -> foodItemDao.findByBarcode(item.barcode)
            else -> null
        }
        if (existing != null) {
            foodItemDao.setFavorite(existing.id, !existing.isFavorite)
        } else {
            foodItemDao.upsert(item.copy(id = 0, isFavorite = true))
        }
    }

    /** Prüft anhand der aktuellen Favoritenliste, ob ein Treffer favorisiert ist. */
    fun isFavorite(item: FoodItem, favorites: List<FoodItem>): Boolean =
        favorites.any { fav ->
            if (!item.barcode.isNullOrBlank()) fav.barcode == item.barcode
            else fav.name == item.name && fav.brand == item.brand
        }
}
