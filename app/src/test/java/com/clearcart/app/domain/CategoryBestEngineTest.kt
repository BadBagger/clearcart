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
        val picks = engine.rankedProductsForCategory(
            category = "Protein shakes",
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
        )

        assertEquals("Simple Protein Shake", picks.first().product.name)
        assertEquals("Sugary Protein Shake", picks[1].product.name)
    }

    @Test
    fun skipsProductsWhenDataIsTooWeakForCategoryRanking() {
        val picks = engine.rankedProductsForCategory(
            category = "Protein shakes",
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
        )

        assertTrue(picks.isEmpty())
    }

    @Test
    fun categoryRankingsHideSelectedAllergens() {
        val products = listOf(
            proteinShake(
                barcode = "milk-shake",
                name = "Milk Protein Shake",
                sugar = 1.0,
                protein = 16.0,
                allergens = listOf("milk"),
                nutriScore = "a",
            ),
            proteinShake(
                barcode = "pea-shake",
                name = "Pea Protein Shake",
                sugar = 2.0,
                protein = 12.0,
                allergens = emptyList(),
                nutriScore = "b",
            ),
        )
        val preferences = UserPreferences(allergensToAvoid = setOf("milk"))
        val picks = engine.rankedProductsForCategory(
            category = "Protein shakes",
            products = products,
            preferences = preferences,
        )

        assertEquals(listOf("Pea Protein Shake"), picks.map { it.product.name })
        assertEquals(1, engine.hiddenByPreferenceCount("Protein shakes", products, preferences))
    }

    private fun proteinShake(
        barcode: String,
        name: String,
        sugar: Double?,
        protein: Double?,
        allergens: List<String> = emptyList(),
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
        allergens = allergens,
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
