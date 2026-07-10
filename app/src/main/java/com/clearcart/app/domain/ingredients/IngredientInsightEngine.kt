package com.clearcart.app.domain.ingredients

import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences

data class IngredientInsight(
    val title: String,
    val detail: String,
    val evidence: String,
    val isPreferenceBased: Boolean = false,
)

class IngredientInsightEngine {
    fun explain(product: Product, preferences: UserPreferences): List<IngredientInsight> {
        val text = product.ingredientsText.trim()
        if (text.isBlank()) {
            return listOf(
                IngredientInsight(
                    title = "Ingredient data is incomplete",
                    detail = "ClearCart cannot explain ingredients that are missing from the product data. The score confidence should be treated accordingly.",
                    evidence = "No readable ingredient list was available.",
                )
            )
        }

        val normalized = text.lowercase()
        val ingredients = splitIngredients(text)
        val insights = mutableListOf<IngredientInsight>()

        if (ingredients.size >= 12) {
            insights += IngredientInsight(
                title = "Longer ingredient list",
                detail = "A longer list can be harder to review while shopping. This is an ingredient clarity signal, not a judgment.",
                evidence = "${ingredients.size} listed ingredients.",
                isPreferenceBased = preferences.simpleIngredients,
            )
        } else if (ingredients.size in 2..5) {
            insights += IngredientInsight(
                title = "Shorter ingredient list",
                detail = "The ingredient list is relatively short, which can make it easier to review quickly.",
                evidence = "${ingredients.size} listed ingredients.",
            )
        }

        matchedTerms(normalized, addedSugarTerms).takeIf { it.isNotEmpty() }?.let { terms ->
            insights += IngredientInsight(
                title = "Added sugar wording found",
                detail = "This may be worth comparing with the nutrition facts, especially if lower sugar is one of your preferences.",
                evidence = terms.joinToString(),
                isPreferenceBased = preferences.lowSugar,
            )
        }

        matchedTerms(normalized, sodiumTerms).takeIf { it.isNotEmpty() && preferences.lowSodium }?.let { terms ->
            insights += IngredientInsight(
                title = "Sodium-related wording found",
                detail = "This is flagged because you selected low sodium. Compare the sodium amount per serving or per 100g when available.",
                evidence = terms.joinToString(),
                isPreferenceBased = true,
            )
        }

        matchedTerms(normalized, colorTerms).takeIf { it.isNotEmpty() }?.let { terms ->
            insights += IngredientInsight(
                title = "Color additive wording found",
                detail = "Some shoppers prefer to limit color additives. ClearCart keeps this visible without treating it as a medical claim.",
                evidence = terms.joinToString(),
                isPreferenceBased = preferences.avoidArtificialColors,
            )
        }

        val foundFragranceTerms = matchedTerms(normalized, fragranceTerms)
        if (foundFragranceTerms.isNotEmpty()) {
            insights += IngredientInsight(
                title = "Fragrance/parfum wording found",
                detail = "Some sensitive-skin users prefer fragrance-free products. This is most relevant when it matches your preferences.",
                evidence = foundFragranceTerms.joinToString(),
                isPreferenceBased = preferences.avoidFragrance || preferences.sensitiveSkin || product.type == ProductType.Cosmetic,
            )
        }

        val allergenConflicts = product.allergens
            .map { it.lowercase() }
            .intersect(preferences.allergensToAvoid.map { it.lowercase() }.toSet())
        if (allergenConflicts.isNotEmpty()) {
            insights += IngredientInsight(
                title = "Matches allergens you chose to avoid",
                detail = "This reflects your saved preferences. For medical dietary needs, check with a professional.",
                evidence = allergenConflicts.joinToString(),
                isPreferenceBased = true,
            )
        }

        if (product.additives.isNotEmpty()) {
            insights += IngredientInsight(
                title = "Listed additives",
                detail = "Additives are listed by the product data source. The practical importance depends on the product and your preferences.",
                evidence = product.additives.take(5).joinToString(),
            )
        }

        return insights.ifEmpty {
            listOf(
                IngredientInsight(
                    title = "No major ingredient notes found",
                    detail = "ClearCart did not find obvious preference conflicts in the readable ingredient text. Raw facts remain visible.",
                    evidence = "Reviewed ${ingredients.size} listed ingredients.",
                )
            )
        }
    }

    private fun splitIngredients(text: String): List<String> =
        text.replace("Ingredients:", "", ignoreCase = true)
            .split(',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun matchedTerms(text: String, terms: List<String>): List<String> =
        terms.filter { term -> text.contains(term) }.distinct()

    private companion object {
        val addedSugarTerms = listOf(
            "sugar",
            "cane sugar",
            "syrup",
            "glucose",
            "fructose",
            "dextrose",
            "maltodextrin",
            "honey",
        )
        val sodiumTerms = listOf("salt", "sodium", "sea salt", "sodium chloride")
        val colorTerms = listOf("artificial color", "red 40", "yellow 5", "yellow 6", "blue 1", "caramel color")
        val fragranceTerms = listOf("fragrance", "parfum")
    }
}
