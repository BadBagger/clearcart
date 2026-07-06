package com.clearcart.app.domain

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.confidence.ConfidenceEngine
import com.clearcart.app.domain.scoring.ScoringEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringEngineTest {
    private val engine = ScoringEngine()

    @Test
    fun lowSugarPreferenceAddsPreferenceCautionWithoutHidingFacts() {
        val product = foodProduct(
            nutrition = Nutrition(
                energyKcal100g = 410.0,
                sugar100g = 18.0,
                sodium100g = 0.22,
                saturatedFat100g = 1.0,
                fiber100g = 6.0,
                protein100g = 8.0,
            ),
        )

        val score = engine.score(product, UserPreferences(lowSugar = true))

        assertTrue(score.cautionList.any { it.text.contains("low sugar preference", ignoreCase = true) })
        assertTrue(score.subscores.any { it.name == "Sugar" && it.detail.contains("18 g") })
    }

    @Test
    fun allergenPreferenceCreatesPreferenceBasedCaution() {
        val product = foodProduct(allergens = listOf("milk", "soy"))

        val score = engine.score(product, UserPreferences(allergensToAvoid = setOf("milk")))

        val allergenCaution = score.cautionList.first { it.text.contains("allergens", ignoreCase = true) }
        assertTrue(allergenCaution.isPreferenceBased)
        assertTrue(allergenCaution.evidence.contains("milk"))
    }

    @Test
    fun missingSaturatedFatIsNotPresentedAsLowSaturatedFat() {
        val product = foodProduct(
            nutrition = Nutrition(
                energyKcal100g = 380.0,
                sugar100g = 4.0,
                sodium100g = 0.1,
                saturatedFat100g = null,
                fiber100g = 7.0,
                protein100g = 9.0,
            ),
        )

        val score = engine.score(product, UserPreferences())

        assertTrue(score.positiveList.none { it.text.contains("Low saturated fat", ignoreCase = true) })
        assertTrue(score.subscores.any { it.name == "Saturated fat" && it.detail == "Missing per 100g." })
    }

    @Test
    fun confidenceEngineMarksCompleteBarcodeProductHighConfidence() {
        val product = foodProduct(lastUpdated = 1_700_000_000)

        val confidence = ConfidenceEngine().confidenceFor(product)

        assertEquals(ConfidenceLevel.High, confidence)
    }

    @Test
    fun confidenceEngineMarksUserEnteredProductsSeparately() {
        val product = foodProduct(source = ProductSource.UserEntered)

        val confidence = ConfidenceEngine().confidenceFor(product)

        assertEquals(ConfidenceLevel.UserEntered, confidence)
    }

    private fun foodProduct(
        nutrition: Nutrition? = Nutrition(380.0, 4.0, 0.1, 0.8, 7.0, 9.0),
        allergens: List<String> = emptyList(),
        source: ProductSource = ProductSource.OpenFoodFacts,
        lastUpdated: Long? = 1_700_000_000,
    ) = Product(
        barcode = "test",
        name = "Test Product",
        brand = "Test Brand",
        category = "cereal",
        imageUrl = null,
        ingredientsText = "Whole oats, sea salt.",
        allergens = allergens,
        labels = emptyList(),
        nutrition = nutrition,
        additives = emptyList(),
        novaGroup = 2,
        nutriScore = "b",
        source = source,
        lastUpdated = lastUpdated,
        confidenceLevel = ConfidenceLevel.High,
    )
}
