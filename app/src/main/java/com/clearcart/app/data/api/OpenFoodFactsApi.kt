package com.clearcart.app.data.api

import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json?fields=code,status,product_name,brands,categories_tags,image_front_url,ingredients_text,allergens_tags,labels_tags,additives_tags,nova_group,nutriscore_grade,nutriments,last_modified_t")
    suspend fun product(@Path("barcode") barcode: String): OffResponse
}

interface OpenBeautyFactsApi {
    @GET("api/v2/product/{barcode}.json?fields=code,status,product_name,brands,categories_tags,image_front_url,ingredients_text,allergens_tags,labels_tags,additives_tags,nova_group,nutriscore_grade,nutriments,last_modified_t")
    suspend fun product(@Path("barcode") barcode: String): OffResponse
}

data class OffResponse(
    val status: Int?,
    val product: OffProduct?,
)

data class OffProduct(
    val product_name: String?,
    val brands: String?,
    val categories_tags: List<String>?,
    val image_front_url: String?,
    val ingredients_text: String?,
    val allergens_tags: List<String>?,
    val labels_tags: List<String>?,
    val additives_tags: List<String>?,
    val nova_group: Int?,
    val nutriscore_grade: String?,
    val nutriments: OffNutriments?,
    val last_modified_t: Long?,
)

data class OffNutriments(
    val energy_kcal_100g: Double?,
    val sugars_100g: Double?,
    val sodium_100g: Double?,
    val saturated_fat_100g: Double?,
    val fiber_100g: Double?,
    val proteins_100g: Double?,
)
