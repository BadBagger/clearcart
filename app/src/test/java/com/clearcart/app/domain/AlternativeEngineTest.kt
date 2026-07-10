package com.clearcart.app.domain

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.alternatives.AlternativeEngine
import com.clearcart.app.domain.scoring.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlternativeEngineTest {
    private val engine = AlternativeEngine(ScoringEngine())

    @Test
    fun sodaSuggestsLowerSugarSimilarDrink() {
        val current = drink(
            barcode = "soda-current",
            name = "Classic Cola",
            sugar = 11.0,
            ingredients = "Carbonated water, high fructose corn syrup, caramel color, natural flavor.",
            additives = listOf("caramel color"),
        )
        val lowerSugar = drink(
            barcode = "soda-alt",
            name = "Light Sparkling Citrus",
            sugar = 1.0,
            ingredients = "Carbonated water, citrus juice, natural flavor.",
            additives = emptyList(),
        )

        val suggestions = engine.suggestFor(current, UserPreferences(lowSugar = true), listOf(lowerSugar), includeMockFallback = false)

        assertEquals("soda-alt", suggestions.first().product.barcode)
        assertTrue(suggestions.first().whatIsBetter.contains("lower sugar", ignoreCase = true))
        assertTrue(suggestions.first().whyBetter.contains("Same category", ignoreCase = true))
    }

    @Test
    fun shampooSuggestsFragranceFreeHairCareForFragrancePreference() {
        val current = cosmetic(
            barcode = "shampoo-current",
            name = "Daily Shine Shampoo",
            ingredients = "Water, sodium laureth sulfate, fragrance, glycerin.",
            allergens = listOf("fragrance"),
        )
        val fragranceFree = cosmetic(
            barcode = "shampoo-alt",
            name = "Fragrance-Free Gentle Shampoo",
            ingredients = "Water, decyl glucoside, glycerin, aloe leaf juice.",
            allergens = emptyList(),
        )

        val suggestions = engine.suggestFor(current, UserPreferences(avoidFragrance = true), listOf(fragranceFree), includeMockFallback = false)

        assertEquals("shampoo-alt", suggestions.first().product.barcode)
        assertTrue(suggestions.first().whatIsBetter.contains("fewer preference conflicts", ignoreCase = true))
        assertTrue(suggestions.first().dataConfidence != ConfidenceLevel.MissingData)
    }

    @Test
    fun weakDataAlternativesAreNotSuggested() {
        val current = cereal("current", "Sweet Cereal", sugar = 16.0)
        val weak = cereal("weak", "Mystery Cereal", sugar = 2.0).copy(
            ingredientsText = "",
            category = "",
            nutrition = null,
            confidenceLevel = ConfidenceLevel.MissingData,
        )

        val suggestions = engine.suggestFor(current, UserPreferences(lowSugar = true), listOf(weak), includeMockFallback = false)

        assertTrue(suggestions.none { it.product.barcode == "weak" })
    }

    private fun cereal(barcode: String, name: String, sugar: Double) = Product(
        barcode = barcode,
        name = name,
        brand = "Test Brand",
        category = "cereal",
        imageUrl = null,
        ingredientsText = "Whole oats, cane sugar, sea salt.",
        allergens = emptyList(),
        labels = emptyList(),
        nutrition = Nutrition(380.0, sugar, 0.2, 1.0, 5.0, 8.0),
        additives = emptyList(),
        novaGroup = 3,
        nutriScore = "c",
        quantity = "12 oz",
        servingSize = "40 g",
        source = ProductSource.Mock,
        dataSource = ProductSource.Mock,
        lastUpdated = 1_700_000_000,
        confidenceLevel = ConfidenceLevel.High,
        type = ProductType.Food,
        productType = ProductType.Food,
    )

    private fun drink(
        barcode: String,
        name: String,
        sugar: Double,
        ingredients: String,
        additives: List<String>,
    ) = Product(
        barcode = barcode,
        name = name,
        brand = "Test Drinks",
        category = "soda",
        imageUrl = null,
        ingredientsText = ingredients,
        allergens = emptyList(),
        labels = emptyList(),
        nutrition = Nutrition(40.0, sugar, 0.01, 0.0, 0.0, 0.0),
        additives = additives,
        novaGroup = 4,
        nutriScore = "d",
        quantity = "12 fl oz",
        servingSize = "355 ml",
        source = ProductSource.Mock,
        dataSource = ProductSource.Mock,
        lastUpdated = 1_700_000_000,
        confidenceLevel = ConfidenceLevel.High,
        type = ProductType.Drink,
        productType = ProductType.Drink,
    )

    private fun cosmetic(
        barcode: String,
        name: String,
        ingredients: String,
        allergens: List<String>,
    ) = Product(
        barcode = barcode,
        name = name,
        brand = "Test Care",
        category = "hair care shampoo",
        imageUrl = null,
        ingredientsText = ingredients,
        allergens = allergens,
        labels = emptyList(),
        nutrition = null,
        additives = emptyList(),
        novaGroup = null,
        nutriScore = null,
        quantity = "16 fl oz",
        servingSize = null,
        source = ProductSource.Mock,
        dataSource = ProductSource.Mock,
        lastUpdated = 1_700_000_000,
        confidenceLevel = ConfidenceLevel.High,
        type = ProductType.Cosmetic,
        productType = ProductType.Cosmetic,
    )
}
