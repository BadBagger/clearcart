package com.clearcart.app.domain

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.preferences.ProductPreferenceFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPreferenceFilterTest {
    @Test
    fun hidesProductsWithSelectedAllergen() {
        val product = product(allergens = listOf("milk"))

        assertFalse(
            ProductPreferenceFilter.visibleInRecommendationLists(
                product,
                UserPreferences(allergensToAvoid = setOf("milk")),
            ),
        )
    }

    @Test
    fun dairyFreeAndGlutenFreeActAsListFilters() {
        assertFalse(ProductPreferenceFilter.visibleInRecommendationLists(product(allergens = listOf("wheat")), UserPreferences(glutenFree = true)))
        assertFalse(ProductPreferenceFilter.visibleInRecommendationLists(product(ingredients = "Water, whey protein."), UserPreferences(dairyFree = true)))
        assertTrue(ProductPreferenceFilter.visibleInRecommendationLists(product(allergens = emptyList()), UserPreferences(dairyFree = true, glutenFree = true)))
    }

    private fun product(
        allergens: List<String> = emptyList(),
        ingredients: String = "Water, pea protein.",
    ) = Product(
        id = "test",
        barcode = "test",
        name = "Test Protein",
        brand = "Test",
        category = "protein shakes",
        imageUrl = null,
        ingredientsText = ingredients,
        allergens = allergens,
        labels = emptyList(),
        nutrition = Nutrition(60.0, 1.0, 0.1, 0.2, 1.0, 12.0),
        additives = emptyList(),
        novaGroup = 2,
        nutriScore = "a",
        quantity = "11 fl oz",
        servingSize = "330 ml",
        source = ProductSource.Mock,
        dataSource = ProductSource.Mock,
        dataCompletenessScore = 90,
        lastUpdated = 1_700_000_000,
        confidenceLevel = ConfidenceLevel.High,
        type = ProductType.Drink,
        productType = ProductType.Drink,
    )
}
