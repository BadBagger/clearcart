package com.clearcart.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductDataQualityTest {
    @Test
    fun completeFoodProductGetsCompleteLabel() {
        val product = ProductDataQuality.normalize(
            product(
                ingredientsText = "Whole oats, apple, cinnamon.",
                nutrition = Nutrition(300.0, 5.0, 0.1, 0.5, 6.0, 8.0),
                category = "cereal",
                quantity = "12 oz",
                servingSize = "40 g",
                labels = listOf("vegetarian"),
                additives = listOf("ascorbic acid"),
                lastUpdated = 1_700_000_000,
            )
        )

        assertEquals(ProductDataQualityLabel.Complete, ProductDataQuality.qualityLabel(product))
        assertTrue(product.dataCompletenessScore >= 85)
    }

    @Test
    fun foodWithoutNutritionIsClearlyLabeled() {
        val product = ProductDataQuality.normalize(
            product(
                ingredientsText = "Water, cane sugar.",
                nutrition = null,
                category = "drink",
                productType = ProductType.Drink,
            )
        )

        assertEquals(ProductDataQualityLabel.MissingNutrition, ProductDataQuality.qualityLabel(product))
        assertTrue(ProductDataQuality.missingDataCopy(product).contains("nutrition"))
    }

    @Test
    fun manualProductIsUserAddedAndUserEdited() {
        val product = ProductDataQuality.normalize(
            product(
                ingredientsText = "Oats, salt.",
                source = ProductSource.UserEntered,
                dataSource = ProductSource.UserEntered,
                userEdited = true,
            )
        )

        assertEquals(ProductDataQualityLabel.UserAdded, ProductDataQuality.qualityLabel(product))
        assertEquals(ConfidenceLevel.UserEntered, product.confidenceLevel)
    }

    @Test
    fun ocrProductNeedsReviewEvenAfterUserReview() {
        val product = ProductDataQuality.normalize(
            product(
                ingredientsText = "Ingredients: water, sugar.",
                source = ProductSource.Ocr,
                dataSource = ProductSource.Ocr,
                userEdited = true,
            )
        )

        assertEquals(ProductDataQualityLabel.NeedsReview, ProductDataQuality.qualityLabel(product))
        assertEquals(ConfidenceLevel.Low, product.confidenceLevel)
    }

    private fun product(
        ingredientsText: String,
        nutrition: Nutrition? = Nutrition(null, null, null, null, null, null),
        category: String = "snack",
        quantity: String? = null,
        servingSize: String? = null,
        labels: List<String> = emptyList(),
        additives: List<String> = emptyList(),
        lastUpdated: Long? = null,
        productType: ProductType = ProductType.Food,
        source: ProductSource = ProductSource.OpenFoodFacts,
        dataSource: ProductSource = source,
        userEdited: Boolean = false,
    ) = Product(
        barcode = "test",
        name = "Test Product",
        brand = "Test Brand",
        category = category,
        imageUrl = "https://example.com/product.jpg",
        ingredientsText = ingredientsText,
        allergens = listOf("milk"),
        labels = labels,
        nutrition = nutrition,
        additives = additives,
        novaGroup = 2,
        nutriScore = "b",
        quantity = quantity,
        servingSize = servingSize,
        source = source,
        dataSource = dataSource,
        lastUpdated = lastUpdated,
        confidenceLevel = ConfidenceLevel.Medium,
        type = productType,
        productType = productType,
        userEdited = userEdited,
    )
}
