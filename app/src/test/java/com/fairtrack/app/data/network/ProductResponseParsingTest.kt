package com.fairtrack.app.data.network

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Parst die echte Open-Food-Facts-Antwort (v2 Product-API) für EAN 4001956253539
 * ("Salamissimo"), mit der der Scanner im Live-Test fälschlich im
 * ManualEntry-Zweig gelandet ist.
 */
class ProductResponseParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val body = """
        {"code":"4001956253539","product":{"brands":"Wiltmann","nutriments":{"carbohydrates":1,"carbohydrates_100g":1,"carbohydrates_unit":"g","carbohydrates_value":1,"energy":2095,"energy-kcal":507,"energy-kcal_100g":507,"energy-kcal_unit":"kcal","energy-kcal_value":507,"energy-kj":2095,"energy-kj_100g":2095,"energy-kj_modifier":"~","energy-kj_unit":"kJ","energy-kj_value":2095,"energy_100g":2095,"energy_modifier":"~","energy_unit":"kJ","energy_value":2095,"fat":41,"fat_100g":41,"fat_unit":"g","fat_value":41,"proteins":33,"proteins_100g":33,"proteins_unit":"g","proteins_value":33,"salt":3.935,"salt_100g":3.935,"salt_unit":"g","salt_value":3.935,"saturated-fat":16,"saturated-fat_100g":16,"saturated-fat_unit":"g","saturated-fat_value":16,"sodium":1.574,"sodium_100g":1.574,"sodium_unit":"g","sodium_value":1.574,"sugars":1,"sugars_100g":1,"sugars_unit":"g","sugars_value":1},"nutrition_data":"on","nutrition_data_per":"100g","nutrition_data_prepared_per":"100g","product_name":"Salamissimo"},"status":1,"status_verbose":"product found"}
    """.trimIndent()

    @Test
    fun `salamissimo response liefert status 1 und kcal`() {
        val resp = json.decodeFromString<ProductResponse>(body)
        assertEquals(1, resp.status)
        assertNotNull(resp.product)
        assertEquals("Salamissimo", resp.product?.productName)
        assertEquals("Wiltmann", resp.product?.brands)
        assertEquals(507.0, resp.product?.nutriments?.energyKcal100g)
    }
}
