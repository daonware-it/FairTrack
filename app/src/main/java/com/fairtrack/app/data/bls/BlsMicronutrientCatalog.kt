package com.fairtrack.app.data.bls

import com.fairtrack.app.data.Micronutrients

/**
 * Kuratierter Offline-Katalog für Mikronährstoffe von Basislebensmitteln.
 *
 * Zweite, lizenzkostenfreie Quelle neben Open Food Facts (dessen Mikronährstoff-
 * Abdeckung lückenhaft ist, da Vitamine/Mineralstoffe nur bei angereicherten
 * Produkten Pflicht sind). Basislebensmittel ohne Barcode – v. a. rohes Obst und
 * Gemüse aus dem [com.fairtrack.app.data.produce.ProduceCatalog] – erhalten so
 * dennoch Mikronährstoffwerte.
 *
 * Datenquelle: Bundeslebensmittelschlüssel (BLS) 4.0, Max Rubner-Institut (MRI),
 * seit 12/2025 als Open Data unter CC BY 4.0 verfügbar (www.blsdb.de). Da der BLS
 * nur als Datenbank-Download bereitsteht und KEINEN offenen Laufzeit-API-Zugang
 * bietet, sind hier repräsentative Werte pro 100 g roh von Hand kuratiert
 * (ergänzt um Standard-Nährwerttabellen). Werte in mg bzw. µg – exakt den
 * Referenzeinheiten aus [Micronutrients].
 *
 * Der Lookup erfolgt über den (deutschen) Lebensmittelnamen, sodass die im
 * ProduceCatalog gepflegten Namen 1:1 gematcht werden.
 */
object BlsMicronutrientCatalog {

    /**
     * mg/µg pro 100 g. Reihenfolge der Argumente entspricht [Micronutrients]:
     * A(µg), D(µg), E(mg), C(mg), B6(mg), B12(µg), Folat(µg),
     * Calcium(mg), Eisen(mg), Magnesium(mg), Zink(mg), Kalium(mg),
     * Phosphor(mg), Jod(µg).
     */
    private val byName: Map<String, Micronutrients> = buildMap {
        // ---- Obst ----
        put("Apfel", Micronutrients(3.0, 0.0, 0.5, 12.0, 0.05, 0.0, 3.0, 6.0, 0.1, 5.0, 0.04, 120.0, 11.0, 1.0))
        put("Banane", Micronutrients(5.0, 0.0, 0.3, 12.0, 0.37, 0.0, 20.0, 8.0, 0.4, 31.0, 0.2, 358.0, 28.0, 2.0))
        put("Orange", Micronutrients(11.0, 0.0, 0.2, 50.0, 0.06, 0.0, 30.0, 42.0, 0.4, 14.0, 0.1, 181.0, 22.0, 2.0))
        put("Erdbeere", Micronutrients(1.0, 0.0, 0.3, 60.0, 0.05, 0.0, 24.0, 24.0, 0.4, 15.0, 0.1, 160.0, 24.0, 8.0))
        put("Kiwi", Micronutrients(4.0, 0.0, 1.5, 90.0, 0.06, 0.0, 25.0, 34.0, 0.3, 17.0, 0.1, 312.0, 34.0, 1.0))
        put("Blaubeere", Micronutrients(3.0, 0.0, 0.6, 22.0, 0.05, 0.0, 6.0, 10.0, 0.5, 2.0, 0.1, 78.0, 13.0, 1.0))
        put("Himbeere", Micronutrients(2.0, 0.0, 0.9, 25.0, 0.08, 0.0, 30.0, 40.0, 1.0, 30.0, 0.4, 200.0, 44.0, 3.0))
        put("Weintraube", Micronutrients(3.0, 0.0, 0.7, 4.0, 0.1, 0.0, 4.0, 18.0, 0.5, 8.0, 0.1, 250.0, 25.0, 1.0))
        put("Mango", Micronutrients(54.0, 0.0, 0.9, 37.0, 0.12, 0.0, 36.0, 13.0, 0.3, 18.0, 0.1, 170.0, 12.0, 1.0))
        put("Ananas", Micronutrients(3.0, 0.0, 0.1, 20.0, 0.09, 0.0, 15.0, 16.0, 0.4, 17.0, 0.1, 170.0, 8.0, 1.0))

        // ---- Gemüse ----
        put("Brokkoli", Micronutrients(80.0, 0.0, 1.4, 90.0, 0.21, 0.0, 111.0, 105.0, 1.3, 24.0, 0.6, 340.0, 78.0, 15.0))
        put("Spinat", Micronutrients(469.0, 0.0, 2.0, 28.0, 0.22, 0.0, 145.0, 99.0, 2.7, 79.0, 0.5, 558.0, 49.0, 12.0))
        put("Karotte", Micronutrients(1580.0, 0.0, 0.5, 7.0, 0.11, 0.0, 19.0, 33.0, 0.3, 12.0, 0.2, 320.0, 35.0, 15.0))
        put("Paprika", Micronutrients(157.0, 0.0, 1.6, 140.0, 0.27, 0.0, 46.0, 10.0, 0.4, 12.0, 0.2, 210.0, 26.0, 3.0))
        put("Tomate", Micronutrients(42.0, 0.0, 0.8, 25.0, 0.1, 0.0, 15.0, 10.0, 0.5, 11.0, 0.2, 237.0, 24.0, 2.0))
        put("Kartoffel", Micronutrients(1.0, 0.0, 0.1, 17.0, 0.21, 0.0, 15.0, 12.0, 0.4, 21.0, 0.3, 411.0, 50.0, 4.0))
        put("Grünkohl", Micronutrients(861.0, 0.0, 1.7, 105.0, 0.25, 0.0, 187.0, 212.0, 1.9, 31.0, 0.3, 490.0, 87.0, 12.0))
        put("Blumenkohl", Micronutrients(1.0, 0.0, 0.1, 48.0, 0.2, 0.0, 57.0, 22.0, 0.4, 15.0, 0.3, 299.0, 44.0, 1.0))
        put("Zwiebel", Micronutrients(0.0, 0.0, 0.1, 7.0, 0.12, 0.0, 19.0, 23.0, 0.2, 10.0, 0.2, 146.0, 29.0, 2.0))
        put("Erbsen", Micronutrients(40.0, 0.0, 0.1, 40.0, 0.16, 0.0, 65.0, 25.0, 1.5, 33.0, 1.2, 244.0, 108.0, 4.0))
        put("Linsen", Micronutrients(1.0, 0.0, 0.5, 1.0, 0.18, 0.0, 35.0, 19.0, 3.3, 36.0, 1.3, 369.0, 180.0, 3.0))
    }

    /** Mikronährstoffe pro 100 g für ein Basislebensmittel, oder null wenn unbekannt. */
    fun forFood(name: String): Micronutrients? = byName[name.trim()]
}
