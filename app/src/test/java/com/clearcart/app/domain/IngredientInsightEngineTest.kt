package com.clearcart.app.domain

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.ingredients.IngredientTag
import com.clearcart.app.domain.ingredients.IngredientInsightEngine
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientInsightEngineTest {
    private val engine = IngredientInsightEngine()

    @Test
    fun addedSugarTermIsExplainedAsPreferenceAwareWithoutMedicalClaim() {
        val insights = engine.explain(
            product = product(ingredientsText = "Whole oats, cane sugar, sea salt."),
            preferences = UserPreferences(lowSugar = true),
        )

        val sugarInsight = insights.first { it.title.contains("sugar", ignoreCase = true) }
        assertTrue(sugarInsight.isPreferenceBased)
        assertTrue(sugarInsight.detail.contains("nutrition facts", ignoreCase = true))
        assertTrue(sugarInsight.tags.contains(IngredientTag.Sweetener))
        assertNoMedicalClaimLanguage(insights.joinToString(" ") { "${it.title} ${it.detail}" })
    }

    @Test
    fun fragranceIsExplainedForCosmeticPreferences() {
        val insights = engine.explain(
            product = product(
                ingredientsText = "Water, glycerin, parfum.",
                type = ProductType.Cosmetic,
            ),
            preferences = UserPreferences(avoidFragrance = true, sensitiveSkin = true),
        )

        val fragranceInsight = insights.first { it.tags.contains(IngredientTag.Fragrance) }
        assertTrue(fragranceInsight.isPreferenceBased)
        assertTrue(fragranceInsight.detail.contains("avoiding added fragrance", ignoreCase = true))
        assertTrue(fragranceInsight.tags.contains(IngredientTag.Fragrance))
        assertNoMedicalClaimLanguage(insights.joinToString(" ") { "${it.title} ${it.detail}" })
    }

    @Test
    fun perIngredientExplanationsIncludeTagsAndCalmPurpose() {
        val insights = engine.explainIngredients(
            product = product(ingredientsText = "Water, xanthan gum, potassium sorbate, caramel color."),
            preferences = UserPreferences(),
        )

        assertTrue(insights.first { it.ingredient == "xanthan gum" }.tags.contains(IngredientTag.Thickener))
        assertTrue(insights.first { it.ingredient == "potassium sorbate" }.tags.contains(IngredientTag.Preservative))
        assertTrue(insights.first { it.ingredient == "caramel color" }.tags.contains(IngredientTag.Coloring))
        assertTrue(insights.any { it.detail.contains("commonly used", ignoreCase = true) || it.detail.contains("often used", ignoreCase = true) })
        assertNoMedicalClaimLanguage(insights.joinToString(" ") { "${it.title} ${it.detail}" })
    }

    @Test
    fun userAvoidedAndOkayIngredientsAreTagged() {
        val insights = engine.explainIngredients(
            product = product(ingredientsText = "Water, cane sugar, sea salt."),
            preferences = UserPreferences(
                ingredientAvoidList = setOf("cane sugar"),
                ingredientOkayList = setOf("sea salt"),
            ),
        )

        val avoided = insights.first { it.ingredient == "cane sugar" }
        val okay = insights.first { it.ingredient == "sea salt" }

        assertTrue(avoided.tags.contains(IngredientTag.UserAvoided))
        assertTrue(avoided.isPreferenceBased)
        assertTrue(okay.tags.contains(IngredientTag.UserFavorite))
    }

    @Test
    fun missingIngredientsExplainDataGap() {
        val insights = engine.explain(product(ingredientsText = ""), UserPreferences())

        assertTrue(insights.first().title.contains("incomplete", ignoreCase = true))
        assertFalse(insights.first().isPreferenceBased)
    }

    private fun assertNoMedicalClaimLanguage(text: String) {
        val lower = text.lowercase()
        assertFalse(lower.contains("dangerous"))
        assertFalse(lower.contains("toxic"))
        assertFalse(lower.contains("safe"))
        assertFalse(lower.contains("cure"))
        assertFalse(lower.contains("prevent disease"))
    }

    private fun product(
        ingredientsText: String,
        type: ProductType = ProductType.Food,
    ) = Product(
        barcode = "test",
        name = "Test Product",
        brand = "Test Brand",
        category = "snack",
        imageUrl = null,
        ingredientsText = ingredientsText,
        allergens = emptyList(),
        labels = emptyList(),
        nutrition = Nutrition(null, 8.0, 0.1, null, null, null),
        additives = emptyList(),
        novaGroup = null,
        nutriScore = null,
        source = ProductSource.Mock,
        lastUpdated = null,
        confidenceLevel = ConfidenceLevel.Medium,
        type = type,
    )
}
