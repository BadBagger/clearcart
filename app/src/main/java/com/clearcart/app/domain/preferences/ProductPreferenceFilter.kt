package com.clearcart.app.domain.preferences

import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.UserPreferences

object ProductPreferenceFilter {
    fun visibleInRecommendationLists(product: Product, preferences: UserPreferences): Boolean =
        allergenConflicts(product, preferences).isEmpty()

    fun hiddenAllergenCount(products: List<Product>, preferences: UserPreferences): Int =
        products.count { !visibleInRecommendationLists(it, preferences) }

    fun allergenConflicts(product: Product, preferences: UserPreferences): Set<String> {
        val terms = allergyTerms(preferences)
        if (terms.isEmpty()) return emptySet()
        val searchable = (product.allergens + product.ingredientsText + product.labels)
            .joinToString(" ")
            .lowercase()
        return terms.filter { term -> searchable.contains(term.lowercase()) }.toSet()
    }

    private fun allergyTerms(preferences: UserPreferences): Set<String> = buildSet {
        addAll(preferences.allergensToAvoid.map { it.trim() }.filter { it.isNotBlank() })
        if (preferences.glutenFree) addAll(listOf("gluten", "wheat", "barley", "rye"))
        if (preferences.dairyFree) addAll(listOf("milk", "dairy", "casein", "whey", "lactose"))
    }
}
