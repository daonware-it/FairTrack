package com.fairtrack.app.data

import com.fairtrack.app.data.entity.FoodItem
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Übergibt ein per Barcode gescanntes Produkt vom Scanner (im "für Ergebnis"-
 * Modus) zurück an den aufrufenden Screen – z. B. den Gericht-Editor, der die
 * gescannte Zutat übernimmt. Singleton, damit Scanner-VM und Editor-VM dieselbe
 * Instanz teilen. Der gepufferte Channel hält das Ergebnis, bis der Editor beim
 * Zurückkehren wieder collected.
 */
@Singleton
class FoodPickBus @Inject constructor() {
    private val _picked = Channel<FoodItem>(Channel.BUFFERED)
    val picked = _picked.receiveAsFlow()

    suspend fun emit(item: FoodItem) {
        _picked.send(item)
    }
}
