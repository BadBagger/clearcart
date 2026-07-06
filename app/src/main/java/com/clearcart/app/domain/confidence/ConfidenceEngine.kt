package com.clearcart.app.domain.confidence

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource

class ConfidenceEngine {
    fun confidenceFor(product: Product): ConfidenceLevel {
        if (product.source == ProductSource.UserEntered) return ConfidenceLevel.UserEntered
        if (product.source == ProductSource.Ocr) return ConfidenceLevel.Low
        val hasIngredients = product.ingredientsText.isNotBlank()
        val hasNutrition = product.nutrition != null
        val hasCategory = product.category.isNotBlank()
        val hasRecentDate = product.lastUpdated != null
        return when {
            hasIngredients && hasNutrition && hasCategory && hasRecentDate -> ConfidenceLevel.High
            listOf(hasIngredients, hasNutrition, hasCategory).count { it } >= 2 -> ConfidenceLevel.Medium
            hasIngredients || hasNutrition -> ConfidenceLevel.Low
            else -> ConfidenceLevel.MissingData
        }
    }
}
