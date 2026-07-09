package com.fairtrack.app.data.produce

import com.fairtrack.app.R

/**
 * Offline-Katalog gängiger roher Obst- und Gemüsesorten mit Nährwerten pro 100 g.
 *
 * Datenquelle: Open Food Facts / Standard-Nährwerttabellen (roh, pro 100 g).
 * Namen sind aktuell reine Daten-Strings (deutsch); Lokalisierung erst ab v0.11.0.
 */
enum class ProduceGroup(val labelRes: Int) {
    FRUIT(R.string.produce_group_fruit),
    VEGETABLE(R.string.produce_group_vegetable)
}

enum class ProduceCategory(val group: ProduceGroup, val labelRes: Int) {
    // Obst
    BERRIES(ProduceGroup.FRUIT, R.string.produce_cat_berries),
    POME(ProduceGroup.FRUIT, R.string.produce_cat_pome),
    STONE(ProduceGroup.FRUIT, R.string.produce_cat_stone),
    CITRUS(ProduceGroup.FRUIT, R.string.produce_cat_citrus),
    EXOTIC(ProduceGroup.FRUIT, R.string.produce_cat_exotic),
    MELON(ProduceGroup.FRUIT, R.string.produce_cat_melon),

    // Gemüse
    LEAFY(ProduceGroup.VEGETABLE, R.string.produce_cat_leafy),
    CABBAGE(ProduceGroup.VEGETABLE, R.string.produce_cat_cabbage),
    ROOT(ProduceGroup.VEGETABLE, R.string.produce_cat_root),
    FRUIT_VEG(ProduceGroup.VEGETABLE, R.string.produce_cat_fruitveg),
    ONION(ProduceGroup.VEGETABLE, R.string.produce_cat_onion),
    LEGUME(ProduceGroup.VEGETABLE, R.string.produce_cat_legume),
    MUSHROOM(ProduceGroup.VEGETABLE, R.string.produce_cat_mushroom)
}

data class ProduceItem(
    val name: String,              // deutscher Name (Daten-String; Lokalisierung erst v0.11.0)
    val category: ProduceCategory,
    val kcal: Double,              // pro 100 g roh
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

object ProduceCatalog {

    val items: List<ProduceItem> = listOf(
        // ---------------------------------------------------------------
        // BEERENOBST
        // ---------------------------------------------------------------
        ProduceItem("Blaubeere", ProduceCategory.BERRIES, 57.0, 0.7, 14.0, 0.3),
        ProduceItem("Erdbeere", ProduceCategory.BERRIES, 32.0, 0.7, 7.7, 0.3),
        ProduceItem("Himbeere", ProduceCategory.BERRIES, 52.0, 1.2, 12.0, 0.7),
        ProduceItem("Brombeere", ProduceCategory.BERRIES, 43.0, 1.4, 10.0, 0.5),
        ProduceItem("Johannisbeere", ProduceCategory.BERRIES, 56.0, 1.4, 14.0, 0.2),
        ProduceItem("Stachelbeere", ProduceCategory.BERRIES, 44.0, 0.9, 10.0, 0.6),
        ProduceItem("Weintraube", ProduceCategory.BERRIES, 69.0, 0.7, 18.0, 0.2),
        ProduceItem("Preiselbeere", ProduceCategory.BERRIES, 46.0, 0.4, 12.0, 0.1),
        ProduceItem("Heidelbeere", ProduceCategory.BERRIES, 57.0, 0.7, 14.0, 0.3),
        ProduceItem("Cranberry", ProduceCategory.BERRIES, 46.0, 0.4, 12.0, 0.1),
        ProduceItem("Holunderbeere", ProduceCategory.BERRIES, 73.0, 0.7, 18.0, 0.5),

        // ---------------------------------------------------------------
        // KERNOBST
        // ---------------------------------------------------------------
        ProduceItem("Apfel", ProduceCategory.POME, 52.0, 0.3, 14.0, 0.2),
        ProduceItem("Birne", ProduceCategory.POME, 57.0, 0.4, 15.0, 0.1),
        ProduceItem("Quitte", ProduceCategory.POME, 57.0, 0.4, 15.0, 0.1),
        ProduceItem("Nashi-Birne", ProduceCategory.POME, 42.0, 0.5, 11.0, 0.2),
        ProduceItem("Mispel", ProduceCategory.POME, 47.0, 0.4, 12.0, 0.2),

        // ---------------------------------------------------------------
        // STEINOBST
        // ---------------------------------------------------------------
        ProduceItem("Pfirsich", ProduceCategory.STONE, 39.0, 0.9, 10.0, 0.3),
        ProduceItem("Nektarine", ProduceCategory.STONE, 44.0, 1.1, 11.0, 0.3),
        ProduceItem("Aprikose", ProduceCategory.STONE, 48.0, 1.4, 11.0, 0.4),
        ProduceItem("Kirsche", ProduceCategory.STONE, 63.0, 1.1, 16.0, 0.2),
        ProduceItem("Sauerkirsche", ProduceCategory.STONE, 50.0, 1.0, 12.0, 0.3),
        ProduceItem("Pflaume", ProduceCategory.STONE, 46.0, 0.7, 11.0, 0.3),
        ProduceItem("Zwetschge", ProduceCategory.STONE, 47.0, 0.7, 11.0, 0.2),
        ProduceItem("Mirabelle", ProduceCategory.STONE, 62.0, 0.8, 15.0, 0.3),

        // ---------------------------------------------------------------
        // ZITRUSFRÜCHTE
        // ---------------------------------------------------------------
        ProduceItem("Orange", ProduceCategory.CITRUS, 47.0, 0.9, 12.0, 0.1),
        ProduceItem("Zitrone", ProduceCategory.CITRUS, 29.0, 1.1, 9.0, 0.3),
        ProduceItem("Limette", ProduceCategory.CITRUS, 30.0, 0.7, 11.0, 0.2),
        ProduceItem("Mandarine", ProduceCategory.CITRUS, 53.0, 0.8, 13.0, 0.3),
        ProduceItem("Clementine", ProduceCategory.CITRUS, 47.0, 0.9, 12.0, 0.2),
        ProduceItem("Grapefruit", ProduceCategory.CITRUS, 42.0, 0.8, 11.0, 0.1),
        ProduceItem("Pomelo", ProduceCategory.CITRUS, 38.0, 0.8, 10.0, 0.0),
        ProduceItem("Kumquat", ProduceCategory.CITRUS, 71.0, 1.9, 16.0, 0.9),

        // ---------------------------------------------------------------
        // EXOTISCHE FRÜCHTE
        // ---------------------------------------------------------------
        ProduceItem("Banane", ProduceCategory.EXOTIC, 89.0, 1.1, 23.0, 0.3),
        ProduceItem("Mango", ProduceCategory.EXOTIC, 60.0, 0.8, 15.0, 0.4),
        ProduceItem("Ananas", ProduceCategory.EXOTIC, 50.0, 0.5, 13.0, 0.1),
        ProduceItem("Kiwi", ProduceCategory.EXOTIC, 61.0, 1.1, 15.0, 0.5),
        ProduceItem("Papaya", ProduceCategory.EXOTIC, 43.0, 0.5, 11.0, 0.3),
        ProduceItem("Feige", ProduceCategory.EXOTIC, 74.0, 0.8, 19.0, 0.3),
        ProduceItem("Granatapfel", ProduceCategory.EXOTIC, 83.0, 1.7, 19.0, 1.2),
        ProduceItem("Passionsfrucht", ProduceCategory.EXOTIC, 97.0, 2.2, 23.0, 0.7),
        ProduceItem("Litschi", ProduceCategory.EXOTIC, 66.0, 0.8, 17.0, 0.4),
        ProduceItem("Dattel", ProduceCategory.EXOTIC, 282.0, 2.5, 75.0, 0.4),
        ProduceItem("Kaki", ProduceCategory.EXOTIC, 70.0, 0.6, 18.0, 0.2),
        ProduceItem("Guave", ProduceCategory.EXOTIC, 68.0, 2.6, 14.0, 1.0),

        // ---------------------------------------------------------------
        // MELONEN
        // ---------------------------------------------------------------
        ProduceItem("Wassermelone", ProduceCategory.MELON, 30.0, 0.6, 8.0, 0.2),
        ProduceItem("Honigmelone", ProduceCategory.MELON, 36.0, 0.8, 9.0, 0.1),
        ProduceItem("Cantaloupe-Melone", ProduceCategory.MELON, 34.0, 0.8, 8.0, 0.2),
        ProduceItem("Galiamelone", ProduceCategory.MELON, 35.0, 0.8, 8.0, 0.1),
        ProduceItem("Charentais-Melone", ProduceCategory.MELON, 34.0, 0.9, 8.0, 0.2),

        // ---------------------------------------------------------------
        // BLATTGEMÜSE
        // ---------------------------------------------------------------
        ProduceItem("Spinat", ProduceCategory.LEAFY, 23.0, 2.9, 3.6, 0.4),
        ProduceItem("Kopfsalat", ProduceCategory.LEAFY, 15.0, 1.4, 2.9, 0.2),
        ProduceItem("Feldsalat", ProduceCategory.LEAFY, 21.0, 2.0, 3.6, 0.4),
        ProduceItem("Rucola", ProduceCategory.LEAFY, 25.0, 2.6, 3.7, 0.7),
        ProduceItem("Grünkohl", ProduceCategory.LEAFY, 49.0, 4.3, 9.0, 0.9),
        ProduceItem("Mangold", ProduceCategory.LEAFY, 19.0, 1.8, 3.7, 0.2),
        ProduceItem("Endivie", ProduceCategory.LEAFY, 17.0, 1.3, 3.4, 0.2),
        ProduceItem("Eisbergsalat", ProduceCategory.LEAFY, 14.0, 0.9, 3.0, 0.1),
        ProduceItem("Chicorée", ProduceCategory.LEAFY, 17.0, 1.0, 4.0, 0.1),
        ProduceItem("Radicchio", ProduceCategory.LEAFY, 23.0, 1.4, 4.5, 0.3),

        // ---------------------------------------------------------------
        // KOHLGEMÜSE
        // ---------------------------------------------------------------
        ProduceItem("Brokkoli", ProduceCategory.CABBAGE, 34.0, 2.8, 7.0, 0.4),
        ProduceItem("Blumenkohl", ProduceCategory.CABBAGE, 25.0, 1.9, 5.0, 0.3),
        ProduceItem("Rosenkohl", ProduceCategory.CABBAGE, 43.0, 3.4, 9.0, 0.3),
        ProduceItem("Weißkohl", ProduceCategory.CABBAGE, 25.0, 1.3, 6.0, 0.1),
        ProduceItem("Rotkohl", ProduceCategory.CABBAGE, 31.0, 1.4, 7.0, 0.2),
        ProduceItem("Wirsing", ProduceCategory.CABBAGE, 27.0, 2.0, 6.0, 0.1),
        ProduceItem("Kohlrabi", ProduceCategory.CABBAGE, 27.0, 1.7, 6.0, 0.1),
        ProduceItem("Chinakohl", ProduceCategory.CABBAGE, 16.0, 1.2, 3.2, 0.2),
        ProduceItem("Spitzkohl", ProduceCategory.CABBAGE, 25.0, 1.4, 5.0, 0.2),
        ProduceItem("Pak Choi", ProduceCategory.CABBAGE, 13.0, 1.5, 2.2, 0.2),
        ProduceItem("Romanesco", ProduceCategory.CABBAGE, 25.0, 1.9, 5.0, 0.3),

        // ---------------------------------------------------------------
        // WURZEL- & KNOLLENGEMÜSE
        // ---------------------------------------------------------------
        ProduceItem("Karotte", ProduceCategory.ROOT, 41.0, 0.9, 10.0, 0.2),
        ProduceItem("Kartoffel", ProduceCategory.ROOT, 77.0, 2.0, 17.0, 0.1),
        ProduceItem("Süßkartoffel", ProduceCategory.ROOT, 86.0, 1.6, 20.0, 0.1),
        ProduceItem("Rote Bete", ProduceCategory.ROOT, 43.0, 1.6, 10.0, 0.2),
        ProduceItem("Radieschen", ProduceCategory.ROOT, 16.0, 0.7, 3.4, 0.1),
        ProduceItem("Sellerie", ProduceCategory.ROOT, 16.0, 0.7, 3.0, 0.2),
        ProduceItem("Pastinake", ProduceCategory.ROOT, 75.0, 1.2, 18.0, 0.3),
        ProduceItem("Rettich", ProduceCategory.ROOT, 16.0, 0.7, 3.4, 0.1),
        ProduceItem("Steckrübe", ProduceCategory.ROOT, 37.0, 1.1, 9.0, 0.2),
        ProduceItem("Petersilienwurzel", ProduceCategory.ROOT, 55.0, 2.9, 11.0, 0.6),
        ProduceItem("Ingwer", ProduceCategory.ROOT, 80.0, 1.8, 18.0, 0.8),
        ProduceItem("Topinambur", ProduceCategory.ROOT, 73.0, 2.0, 17.0, 0.0),

        // ---------------------------------------------------------------
        // FRUCHTGEMÜSE
        // ---------------------------------------------------------------
        ProduceItem("Tomate", ProduceCategory.FRUIT_VEG, 18.0, 0.9, 3.9, 0.2),
        ProduceItem("Gurke", ProduceCategory.FRUIT_VEG, 15.0, 0.7, 3.6, 0.1),
        ProduceItem("Paprika", ProduceCategory.FRUIT_VEG, 31.0, 1.0, 6.0, 0.3),
        ProduceItem("Zucchini", ProduceCategory.FRUIT_VEG, 17.0, 1.2, 3.1, 0.3),
        ProduceItem("Aubergine", ProduceCategory.FRUIT_VEG, 25.0, 1.0, 6.0, 0.2),
        ProduceItem("Kürbis", ProduceCategory.FRUIT_VEG, 26.0, 1.0, 7.0, 0.1),
        ProduceItem("Avocado", ProduceCategory.FRUIT_VEG, 160.0, 2.0, 9.0, 15.0),
        ProduceItem("Cocktailtomate", ProduceCategory.FRUIT_VEG, 18.0, 0.9, 3.9, 0.2),
        ProduceItem("Chili", ProduceCategory.FRUIT_VEG, 40.0, 1.9, 9.0, 0.4),

        // ---------------------------------------------------------------
        // ZWIEBELGEWÄCHSE
        // ---------------------------------------------------------------
        ProduceItem("Zwiebel", ProduceCategory.ONION, 40.0, 1.1, 9.0, 0.1),
        ProduceItem("Knoblauch", ProduceCategory.ONION, 149.0, 6.4, 33.0, 0.5),
        ProduceItem("Lauch", ProduceCategory.ONION, 61.0, 1.5, 14.0, 0.3),
        ProduceItem("Frühlingszwiebel", ProduceCategory.ONION, 32.0, 1.8, 7.0, 0.2),
        ProduceItem("Schalotte", ProduceCategory.ONION, 72.0, 2.5, 17.0, 0.1),
        ProduceItem("Rote Zwiebel", ProduceCategory.ONION, 40.0, 1.1, 9.0, 0.1),
        ProduceItem("Bärlauch", ProduceCategory.ONION, 19.0, 0.9, 2.9, 0.3),

        // ---------------------------------------------------------------
        // HÜLSENFRÜCHTE
        // ---------------------------------------------------------------
        ProduceItem("Erbsen", ProduceCategory.LEGUME, 81.0, 5.4, 14.0, 0.4),
        ProduceItem("Grüne Bohnen", ProduceCategory.LEGUME, 31.0, 1.8, 7.0, 0.1),
        ProduceItem("Kichererbsen", ProduceCategory.LEGUME, 164.0, 8.9, 27.0, 2.6),
        ProduceItem("Linsen", ProduceCategory.LEGUME, 116.0, 9.0, 20.0, 0.4),
        ProduceItem("Edamame", ProduceCategory.LEGUME, 121.0, 12.0, 9.0, 5.2),
        ProduceItem("Zuckerschoten", ProduceCategory.LEGUME, 42.0, 2.8, 7.0, 0.2),
        ProduceItem("Kidneybohnen", ProduceCategory.LEGUME, 127.0, 8.7, 23.0, 0.5),
        ProduceItem("Dicke Bohnen", ProduceCategory.LEGUME, 88.0, 8.0, 12.0, 0.7),
        ProduceItem("Sojabohnen", ProduceCategory.LEGUME, 147.0, 13.0, 11.0, 6.8),

        // ---------------------------------------------------------------
        // PILZE
        // ---------------------------------------------------------------
        ProduceItem("Champignon", ProduceCategory.MUSHROOM, 22.0, 3.1, 3.3, 0.3),
        ProduceItem("Pfifferling", ProduceCategory.MUSHROOM, 38.0, 1.5, 7.0, 0.5),
        ProduceItem("Steinpilz", ProduceCategory.MUSHROOM, 30.0, 3.0, 3.0, 0.4),
        ProduceItem("Austernpilz", ProduceCategory.MUSHROOM, 33.0, 3.3, 6.0, 0.4),
        ProduceItem("Shiitake", ProduceCategory.MUSHROOM, 34.0, 2.2, 7.0, 0.5),
        ProduceItem("Kräuterseitling", ProduceCategory.MUSHROOM, 35.0, 3.3, 6.0, 0.4),
        ProduceItem("Portobello", ProduceCategory.MUSHROOM, 22.0, 2.1, 3.9, 0.4),
        ProduceItem("Enoki", ProduceCategory.MUSHROOM, 37.0, 2.7, 8.0, 0.3)
    )

    fun categories(group: ProduceGroup): List<ProduceCategory> =
        ProduceCategory.entries.filter { it.group == group }

    fun items(category: ProduceCategory): List<ProduceItem> =
        items.filter { it.category == category }.sortedBy { it.name.lowercase() }
}
