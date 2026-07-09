package com.fairtrack.app.data.network

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * `brands` kommt je nach Open-Food-Facts-Endpoint unterschiedlich:
 * Die Produkt-API (v2) liefert einen kommaseparierten String, der
 * Such-Dienst (search-a-licious) ein JSON-Array. Dieser Serializer
 * akzeptiert beides und normalisiert auf einen String.
 */
object FlexibleBrandsSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Brands", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonArray -> element
                .mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
                .joinToString(", ")
                .ifBlank { null }
            is JsonPrimitive -> element.contentOrNull
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        encoder.encodeString(value ?: "")
    }
}

/**
 * Antwort der Open Food Facts Product-API (v2).
 * `status == 1` bedeutet Produkt gefunden, `status == 0` nicht gefunden.
 */
@Serializable
data class ProductResponse(
    val status: Int = 0,
    val code: String? = null,
    val product: ProductDto? = null
)

@Serializable
data class ProductDto(
    @SerialName("code") val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_name_de") val productNameDe: String? = null,
    @Serializable(with = FlexibleBrandsSerializer::class) val brands: String? = null,
    @SerialName("categories_tags") val categoriesTags: List<String>? = null,
    @SerialName("image_front_small_url") val imageFrontSmallUrl: String? = null,
    @SerialName("image_small_url") val imageSmallUrl: String? = null,
    val nutriments: NutrimentsDto? = null
)

/**
 * Antwort der Open Food Facts Such-API. Wir nutzen den dedizierten
 * "search-a-licious"-Dienst (search.openfoodfacts.org), der die Treffer im
 * Feld `hits` liefert und – anders als die v2-Suche – relevant nach dem
 * Suchbegriff sortiert. Die Treffer-Objekte haben dieselbe Form wie ProductDto.
 */
@Serializable
data class SearchResponse(
    @SerialName("hits") val products: List<ProductDto> = emptyList(),
    val count: Int = 0,
    val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 0
)

@Serializable
data class NutrimentsDto(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null,
    // --- Mikronährstoffe (OFF liefert `_100g` in SI-Gramm; fehlende Felder -> null) ---
    @SerialName("vitamin-a_100g") val vitaminA100g: Double? = null,
    @SerialName("vitamin-d_100g") val vitaminD100g: Double? = null,
    @SerialName("vitamin-e_100g") val vitaminE100g: Double? = null,
    @SerialName("vitamin-c_100g") val vitaminC100g: Double? = null,
    @SerialName("vitamin-b6_100g") val vitaminB6100g: Double? = null,
    @SerialName("vitamin-b12_100g") val vitaminB12100g: Double? = null,
    @SerialName("folates_100g") val folates100g: Double? = null,
    @SerialName("calcium_100g") val calcium100g: Double? = null,
    @SerialName("iron_100g") val iron100g: Double? = null,
    @SerialName("magnesium_100g") val magnesium100g: Double? = null,
    @SerialName("zinc_100g") val zinc100g: Double? = null,
    @SerialName("potassium_100g") val potassium100g: Double? = null,
    @SerialName("phosphorus_100g") val phosphorus100g: Double? = null,
    @SerialName("iodine_100g") val iodine100g: Double? = null
) {
    /**
     * Wandelt die OFF-SI-Gramm in die Referenzeinheiten von [Micronutrients] um
     * (mg = g·1000, µg = g·1_000_000).
     */
    fun toMicronutrients(): com.fairtrack.app.data.Micronutrients {
        fun mg(g: Double?): Double? = g?.times(1_000.0)
        fun ug(g: Double?): Double? = g?.times(1_000_000.0)
        return com.fairtrack.app.data.Micronutrients(
            vitaminA = ug(vitaminA100g),
            vitaminD = ug(vitaminD100g),
            vitaminE = mg(vitaminE100g),
            vitaminC = mg(vitaminC100g),
            vitaminB6 = mg(vitaminB6100g),
            vitaminB12 = ug(vitaminB12100g),
            folate = ug(folates100g),
            calcium = mg(calcium100g),
            iron = mg(iron100g),
            magnesium = mg(magnesium100g),
            zinc = mg(zinc100g),
            potassium = mg(potassium100g),
            phosphorus = mg(phosphorus100g),
            iodine = ug(iodine100g)
        )
    }
}
