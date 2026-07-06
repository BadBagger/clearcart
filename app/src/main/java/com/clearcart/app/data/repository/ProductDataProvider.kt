package com.clearcart.app.data.repository

import com.clearcart.app.data.api.OffProduct
import com.clearcart.app.data.api.OpenBeautyFactsApi
import com.clearcart.app.data.api.OpenFoodFactsApi
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.domain.confidence.ConfidenceEngine

interface ProductDataProvider {
    val name: String
    suspend fun lookup(barcode: String): Product?
}

class OpenFoodFactsProvider(
    private val api: OpenFoodFactsApi,
    private val confidenceEngine: ConfidenceEngine,
) : ProductDataProvider {
    override val name = "Open Food Facts"

    override suspend fun lookup(barcode: String): Product? {
        val response = api.product(barcode)
        val product = response.product?.takeIf { response.status == 1 } ?: return null
        return product.toProduct(
            barcode = barcode,
            source = ProductSource.OpenFoodFacts,
            type = ProductType.Food,
            raw = response.toString(),
            confidenceEngine = confidenceEngine,
        )
    }
}

class OpenBeautyFactsProvider(
    private val api: OpenBeautyFactsApi,
    private val confidenceEngine: ConfidenceEngine,
) : ProductDataProvider {
    override val name = "Open Beauty Facts"

    override suspend fun lookup(barcode: String): Product? {
        val response = api.product(barcode)
        val product = response.product?.takeIf { response.status == 1 } ?: return null
        return product.toProduct(
            barcode = barcode,
            source = ProductSource.OpenBeautyFacts,
            type = ProductType.Cosmetic,
            raw = response.toString(),
            confidenceEngine = confidenceEngine,
        )
    }
}

private fun OffProduct.toProduct(
    barcode: String,
    source: ProductSource,
    type: ProductType,
    raw: String,
    confidenceEngine: ConfidenceEngine,
): Product {
    val product = Product(
        barcode = barcode,
        name = product_name?.takeIf { it.isNotBlank() } ?: "Unnamed product",
        brand = brands.orEmpty(),
        category = categories_tags?.firstOrNull()?.removePrefix("en:")?.replace('-', ' ').orEmpty(),
        imageUrl = image_front_url,
        ingredientsText = ingredients_text.orEmpty(),
        allergens = allergens_tags.orEmpty().map { it.removePrefix("en:").replace('-', ' ') },
        labels = labels_tags.orEmpty().map { it.removePrefix("en:").replace('-', ' ') },
        nutrition = Nutrition(
            energyKcal100g = nutriments?.energy_kcal_100g,
            sugar100g = nutriments?.sugars_100g,
            sodium100g = nutriments?.sodium_100g,
            saturatedFat100g = nutriments?.saturated_fat_100g,
            fiber100g = nutriments?.fiber_100g,
            protein100g = nutriments?.proteins_100g,
        ),
        additives = additives_tags.orEmpty().map { it.removePrefix("en:").replace('-', ' ') },
        novaGroup = nova_group,
        nutriScore = nutriscore_grade,
        source = source,
        lastUpdated = last_modified_t,
        confidenceLevel = ConfidenceLevel.MissingData,
        type = type,
        rawResponse = raw,
    )
    return product.copy(confidenceLevel = confidenceEngine.confidenceFor(product))
}
