package com.fairtrack.app.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit-Schnittstelle zur Open Food Facts Product-API (deutscher Markt).
 */
interface OpenFoodFactsApi {

    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = "product_name,product_name_de,brands,categories_tags,image_front_small_url,image_small_url,nutriments,vitamin-a_100g,vitamin-d_100g,vitamin-e_100g,vitamin-c_100g,vitamin-b6_100g,vitamin-b12_100g,folates_100g,calcium_100g,iron_100g,magnesium_100g,zinc_100g,potassium_100g,phosphorus_100g,iodine_100g",
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
        @Query("fields") fields: String = "code,product_name,product_name_de,brands,categories_tags,image_front_small_url,image_small_url,nutriments,vitamin-a_100g,vitamin-d_100g,vitamin-e_100g,vitamin-c_100g,vitamin-b6_100g,vitamin-b12_100g,folates_100g,calcium_100g,iron_100g,magnesium_100g,zinc_100g,potassium_100g,phosphorus_100g,iodine_100g",
        @Query("page_size") pageSize: Int = 25,
        @Query("lang") lang: String = "de"
    ): SearchResponse
}
