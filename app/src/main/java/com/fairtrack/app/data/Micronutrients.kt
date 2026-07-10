package com.fairtrack.app.data

import androidx.room.ColumnInfo
import com.fairtrack.app.R
import kotlinx.serialization.Serializable

/**
 * Mikronährstoffe (Vitamine, Mineralstoffe, Spurenelemente) pro 100 g bzw. – als
 * eingefrorener Snapshot im [com.fairtrack.app.data.entity.DiaryEntry] – für die
 * erfasste Menge. Alle Werte sind nullable, da die Abdeckung je Quelle lückenhaft
 * ist (Open Food Facts liefert Mikronährstoffe nur bei angereicherten Produkten;
 * der kuratierte BLS-Offlinekatalog deckt Basislebensmittel ab).
 *
 * Einheiten je Feld sind die konventionellen Referenzeinheiten (mg bzw. µg),
 * NICHT die SI-Gramm von Open Food Facts. Die Umrechnung passiert beim Mapping
 * aus dem OFF-DTO ([com.fairtrack.app.data.network.NutrimentsDto.toMicronutrients]).
 *
 * Als `@Embedded` in FoodItem und DiaryEntry eingebettet -> jede Eigenschaft wird
 * zu einer eigenen nullable REAL-Spalte mit exakt diesem Spaltennamen.
 */
@Serializable
data class Micronutrients(
    @ColumnInfo(name = "vitaminA") val vitaminA: Double? = null,       // µg
    @ColumnInfo(name = "vitaminD") val vitaminD: Double? = null,       // µg
    @ColumnInfo(name = "vitaminE") val vitaminE: Double? = null,       // mg
    @ColumnInfo(name = "vitaminC") val vitaminC: Double? = null,       // mg
    @ColumnInfo(name = "vitaminB6") val vitaminB6: Double? = null,     // mg
    @ColumnInfo(name = "vitaminB12") val vitaminB12: Double? = null,   // µg
    @ColumnInfo(name = "folate") val folate: Double? = null,           // µg
    @ColumnInfo(name = "calcium") val calcium: Double? = null,         // mg
    @ColumnInfo(name = "iron") val iron: Double? = null,               // mg
    @ColumnInfo(name = "magnesium") val magnesium: Double? = null,     // mg
    @ColumnInfo(name = "zinc") val zinc: Double? = null,               // mg
    @ColumnInfo(name = "potassium") val potassium: Double? = null,     // mg
    @ColumnInfo(name = "phosphorus") val phosphorus: Double? = null,   // mg
    @ColumnInfo(name = "iodine") val iodine: Double? = null            // µg
) {
    /** Skaliert alle vorhandenen Werte mit [factor] (z. B. amount/100 beim Tagebuch). */
    fun scale(factor: Double): Micronutrients = Micronutrients(
        vitaminA = vitaminA?.times(factor),
        vitaminD = vitaminD?.times(factor),
        vitaminE = vitaminE?.times(factor),
        vitaminC = vitaminC?.times(factor),
        vitaminB6 = vitaminB6?.times(factor),
        vitaminB12 = vitaminB12?.times(factor),
        folate = folate?.times(factor),
        calcium = calcium?.times(factor),
        iron = iron?.times(factor),
        magnesium = magnesium?.times(factor),
        zinc = zinc?.times(factor),
        potassium = potassium?.times(factor),
        phosphorus = phosphorus?.times(factor),
        iodine = iodine?.times(factor)
    )

    /** true, wenn kein einziger Mikronährstoff gesetzt ist. */
    val isEmpty: Boolean
        get() = MicronutrientType.entries.all { it.selector(this) == null }
}

/** Referenzeinheit eines Mikronährstoffs für Anzeige und NRV-Vergleich. */
enum class MicroUnit(val labelRes: Int) {
    MG(R.string.micro_unit_mg),
    UG(R.string.micro_unit_ug)
}

/**
 * Katalog der getrackten Mikronährstoffe mit Referenzwert (EU-NRV,
 * Nährstoffbezugswerte nach VO (EU) Nr. 1169/2011 Anhang XIII) für die
 * prozentuale Tagesdeckung. Die [selector]-Lambda liest den Wert aus einem
 * [Micronutrients]-Objekt.
 */
enum class MicronutrientType(
    val labelRes: Int,
    val unit: MicroUnit,
    /** EU-NRV (Nährstoffbezugswert) in [unit] – 100 % Tagesdeckung. */
    val nrv: Double,
    val selector: (Micronutrients) -> Double?
) {
    VITAMIN_A(R.string.micro_vitamin_a, MicroUnit.UG, 800.0, { it.vitaminA }),
    VITAMIN_D(R.string.micro_vitamin_d, MicroUnit.UG, 5.0, { it.vitaminD }),
    VITAMIN_E(R.string.micro_vitamin_e, MicroUnit.MG, 12.0, { it.vitaminE }),
    VITAMIN_C(R.string.micro_vitamin_c, MicroUnit.MG, 80.0, { it.vitaminC }),
    VITAMIN_B6(R.string.micro_vitamin_b6, MicroUnit.MG, 1.4, { it.vitaminB6 }),
    VITAMIN_B12(R.string.micro_vitamin_b12, MicroUnit.UG, 2.5, { it.vitaminB12 }),
    FOLATE(R.string.micro_folate, MicroUnit.UG, 200.0, { it.folate }),
    CALCIUM(R.string.micro_calcium, MicroUnit.MG, 800.0, { it.calcium }),
    IRON(R.string.micro_iron, MicroUnit.MG, 14.0, { it.iron }),
    MAGNESIUM(R.string.micro_magnesium, MicroUnit.MG, 375.0, { it.magnesium }),
    ZINC(R.string.micro_zinc, MicroUnit.MG, 10.0, { it.zinc }),
    POTASSIUM(R.string.micro_potassium, MicroUnit.MG, 2000.0, { it.potassium }),
    PHOSPHORUS(R.string.micro_phosphorus, MicroUnit.MG, 700.0, { it.phosphorus }),
    IODINE(R.string.micro_iodine, MicroUnit.UG, 150.0, { it.iodine })
}
