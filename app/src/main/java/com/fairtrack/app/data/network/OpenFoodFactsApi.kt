package com.fairtrack.app.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit-Schnittstelle zur Open Food Facts Product-API (deutscher Markt).
 */
interface OpenFoodFactsApi {

    // WICHTIG: Vitamine/Mineralstoffe NICHT als eigene fields anfordern — sie
    // sind Unterfelder von `nutriments` (und dort bereits enthalten). Werden
    // sie zusätzlich als Top-Level-Felder angefragt, lässt die v2-API das
    // komplette `nutriments`-Objekt weg und der Barcode-Lookup findet keine kcal.
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,product_name_de,brands,categories_tags,image_front_small_url,image_small_url,nutriments",
        @Query("lc") lc: String = "de",
        @Query("cc") cc: String = "de"
    ): ProductResponse

    // Absolute URL auf den dedizierten Such-Dienst "search-a-licious".
    // Die v2-Suche (search_terms) ignoriert den Begriff praktisch und liefert
    // nur populäre Produkte; search.openfoodfacts.org sortiert dagegen relevant.
    // `lang=de` bevorzugt deutsche Namen. Der Barcode-Lookup oben bleibt auf de.
    @GET("https://search.openfoodfacts.org/search")
    suspend fun search(
        @Query("q") terms: String,
        @Query("fields") fields: String = "code,product_name,product_name_de,brands,categories_tags,image_front_small_url,image_small_url,nutriments",
        @Query("page_size") pageSize: Int = 25,
        @Query("lang") lang: String = "de"
    ): SearchResponse
}
