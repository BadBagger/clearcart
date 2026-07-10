package com.clearcart.app.domain

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.categories.CategoryBestEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBestEngineTest {
    private val engine = CategoryBestEngine()

    @Test
    fun ranksProteinShakesWhenCategoryDataExists() {
        val picks = engine.bestByCategory(
            products = listOf(
                proteinShake(
                    barcode = "shake-1",
                    name = "Sugary Protein Shake",
                    sugar = 11.0,
                    protein = 9.0,
                    nutriScore = "c",
                ),
                proteinShake(
                    barcode = "shake-2",
                    name = "Simple Protein Shake",
                    sugar = 1.5,
                    protein = 12.0,
                    nutriScore = "a",
                ),
            ),
            preferences = UserPreferences(highProtein = true, lowSugar = true),
            categories = listOf("Protein shakes"),
        )

        assertEquals("Simple Protein Shake", picks.single().product.name)
        assertTrue(picks.single().whyPicked.contains("ClearCart Score"))
    }

    @Test
    fun skipsProductsWhenDataIsTooWeakForCategoryRanking() {
        val picks = engine.bestByCategory(
            products = listOf(
                Product(
                    id = "weak",
                    barcode = "weak",
                    name = "Mystery Shake",
                    brand = "",
                    category = "",
                    imageUrl = null,
                    ingredientsText = "",
                    allergens = emptyList(),
                    labels = emptyList(),
                    nutrition = null,
                    additives = emptyList(),
                    novaGroup = null,
                    nutriScore = null,
                    source = ProductSource.Mock,
                    dataSource = ProductSource.Mock,
                    dataCompletenessScore = 25,
                    lastUpdated = null,
                    confidenceLevel = ConfidenceLevel.MissingData,
                    type = ProductType.Drink,
                    productType = ProductType.Drink,
                    rawResponse = "categories_tags=[en:protein-shakes]",
                ),
            ),
            preferences = UserPreferences(),
            categories = listOf("Protein shakes"),
        )

        assertTrue(picks.isEmpty())
    }

    private fun proteinShake(
        barcode: String,
        name: String,
        sugar: Double?,
        protein: Double?,
        nutriScore: String? = "b",
        completeness: Int = 90,
    ) = Product(
        id = barcode,
        barcode = barcode,
        name = name,
        brand = "Sample Fuel",
        category = "protein shakes",
        imageUrl = null,
        ingredientsText = "Water, pea protein, vanilla.",
        allergens = emptyList(),
        labels = listOf("high protein"),
        nutrition = Nutrition(60.0, sugar, 0.12, 0.2, 1.0, protein),
        additives = emptyList(),
        novaGroup = 2,
        nutriScore = nutriScore,
        quantity = "11 fl oz",
        servingSize = "330 ml",
        source = ProductSource.Mock,
        dataSource = ProductSource.Mock,
        dataCompletenessScore = completeness,
        lastUpdated = 1_700_000_000,
        confidenceLevel = ConfidenceLevel.High,
        type = ProductType.Drink,
        productType = ProductType.Drink,
    )
}
