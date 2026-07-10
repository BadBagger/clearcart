package com.clearcart.app.domain.ingredients

import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences

enum class IngredientTag(val label: String) {
    Sweetener("Sweetener"),
    Preservative("Preservative"),
    Coloring("Coloring"),
    Thickener("Thickener"),
    Fragrance("Fragrance"),
    Allergen("Allergen"),
    UserAvoided("User-avoided"),
    UserFavorite("User-favorite"),
    Unknown("Unknown"),
}

data class IngredientInsight(
    val title: String,
    val detail: String,
    val evidence: String,
    val isPreferenceBased: Boolean = false,
    val ingredient: String = title,
    val tags: Set<IngredientTag> = setOf(IngredientTag.Unknown),
    val isHighlighted: Boolean = true,
)

class IngredientInsightEngine {
    fun explain(product: Product, preferences: UserPreferences): List<IngredientInsight> {
        val ingredients = explainIngredients(product, preferences)
        if (ingredients.isEmpty()) {
            return listOf(
                IngredientInsight(
                    title = "Ingredient data is incomplete",
                    detail = "ClearCart cannot explain ingredients that are missing from the product data. The score confidence should be treated accordingly.",
                    evidence = "No readable ingredient list was available.",
                    ingredient = "Ingredient list",
                )
            )
        }

        val listNotes = ingredientListNotes(ingredients, preferences)
        val highlighted = ingredients.filter { it.isHighlighted }.take(8)
        return (listNotes + highlighted).ifEmpty {
            listOf(
                IngredientInsight(
                    title = "No major ingredient notes found",
                    detail = "ClearCart did not find obvious preference conflicts in the readable ingredient text. Raw facts remain visible.",
                    evidence = "Reviewed ${ingredients.size} listed ingredients.",
                    ingredient = "Ingredient list",
                )
            )
        }
    }

    fun explainIngredients(product: Product, preferences: UserPreferences): List<IngredientInsight> {
        val ingredients = splitIngredients(product.ingredientsText)
        if (ingredients.isEmpty()) return emptyList()
        val productAllergens = product.allergens.map { cleanKey(it) }.toSet()
        return ingredients.map { ingredient ->
            val key = cleanKey(ingredient)
            val tags = tagsFor(ingredient, key, productAllergens, preferences)
            val highlighted = tags.any { it != IngredientTag.Unknown && it != IngredientTag.UserFavorite }
            IngredientInsight(
                title = ingredient,
                detail = explanationFor(ingredient, tags, preferences, product),
                evidence = evidenceFor(ingredient, tags),
                isPreferenceBased = isPreferenceBased(tags, preferences),
                ingredient = ingredient,
                tags = tags,
                isHighlighted = highlighted,
            )
        }
    }

    private fun ingredientListNotes(
        ingredients: List<IngredientInsight>,
        preferences: UserPreferences,
    ): List<IngredientInsight> = buildList {
        if (ingredients.size >= 12) {
            add(
                IngredientInsight(
                    title = "Longer ingredient list",
                    detail = "A longer list may take more time to review while shopping. This is an ingredient clarity signal, not a judgment.",
                    evidence = "${ingredients.size} listed ingredients.",
                    isPreferenceBased = preferences.simpleIngredients,
                    ingredient = "Ingredient list",
                )
            )
        } else if (ingredients.size in 2..5) {
            add(
                IngredientInsight(
                    title = "Shorter ingredient list",
                    detail = "The ingredient list is relatively short, which may make it easier to review quickly.",
                    evidence = "${ingredients.size} listed ingredients.",
                    ingredient = "Ingredient list",
                    isHighlighted = false,
                )
            )
        }
    }

    private fun tagsFor(
        ingredient: String,
        key: String,
        productAllergens: Set<String>,
        preferences: UserPreferences,
    ): Set<IngredientTag> {
        val lower = ingredient.lowercase()
        val tags = linkedSetOf<IngredientTag>()
        if (matches(lower, sweetenerTerms)) tags += IngredientTag.Sweetener
        if (matches(lower, preservativeTerms)) tags += IngredientTag.Preservative
        if (matches(lower, colorTerms)) tags += IngredientTag.Coloring
        if (matches(lower, thickenerTerms)) tags += IngredientTag.Thickener
        if (matches(lower, fragranceTerms)) tags += IngredientTag.Fragrance
        if (productAllergens.any { key.contains(it) || it.contains(key) } || commonAllergens.any { lower.contains(it) }) {
            tags += IngredientTag.Allergen
        }
        if (preferences.ingredientAvoidList.any { cleanKey(it) == key }) tags += IngredientTag.UserAvoided
        if (preferences.ingredientOkayList.any { cleanKey(it) == key }) tags += IngredientTag.UserFavorite
        return tags.ifEmpty { setOf(IngredientTag.Unknown) }
    }

    private fun explanationFor(
        ingredient: String,
        tags: Set<IngredientTag>,
        preferences: UserPreferences,
        product: Product,
    ): String {
        val notes = mutableListOf<String>()
        if (IngredientTag.Sweetener in tags) notes += "Often used to add sweetness. It may be worth comparing with the nutrition facts."
        if (IngredientTag.Preservative in tags) notes += "Commonly used as a preservative or to help product freshness."
        if (IngredientTag.Coloring in tags) notes += "Commonly used to add or stabilize color. Context matters, including amount and product type."
        if (IngredientTag.Thickener in tags) notes += "Often used to improve texture, thickness, or stability."
        if (IngredientTag.Fragrance in tags) notes += "Common fragrance ingredient wording. This may matter if you are avoiding added fragrance."
        if (IngredientTag.Allergen in tags) notes += "Worth checking if this matches an allergen or sensitivity you track."
        if (IngredientTag.UserAvoided in tags) notes += "This ingredient is flagged because of your preferences."
        if (IngredientTag.UserFavorite in tags) notes += "You marked this ingredient as okay, so ClearCart keeps it visible without treating it as a conflict."
        if (notes.isEmpty()) notes += "ClearCart does not have a specific note for this ingredient yet. Context matters, and the full label remains visible."
        if (product.type == ProductType.Cosmetic && IngredientTag.Fragrance in tags && preferences.sensitiveSkin) {
            notes += "For skin sensitivity preferences, patch guidance or professional advice may be useful."
        }
        return notes.joinToString(" ")
    }

    private fun evidenceFor(ingredient: String, tags: Set<IngredientTag>): String {
        val labels = tags.joinToString { it.label }
        return "Found \"$ingredient\". Tags: $labels."
    }

    private fun isPreferenceBased(tags: Set<IngredientTag>, preferences: UserPreferences): Boolean =
        IngredientTag.UserAvoided in tags ||
            IngredientTag.UserFavorite in tags ||
            IngredientTag.Allergen in tags ||
            (IngredientTag.Sweetener in tags && preferences.lowSugar) ||
            (IngredientTag.Coloring in tags && preferences.avoidArtificialColors) ||
            (IngredientTag.Fragrance in tags && (preferences.avoidFragrance || preferences.sensitiveSkin))

    private fun splitIngredients(text: String): List<String> =
        text.replace("Ingredients:", "", ignoreCase = true)
            .split(',', ';')
            .map { it.trim().trim('.') }
            .filter { it.isNotBlank() }

    private fun cleanKey(value: String): String =
        value.lowercase()
            .replace("en:", "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun matches(text: String, terms: List<String>): Boolean =
        terms.any { term -> text.contains(term) }

    private companion object {
        val sweetenerTerms = listOf(
            "sugar",
            "cane sugar",
            "syrup",
            "glucose",
            "fructose",
            "dextrose",
            "maltodextrin",
            "honey",
            "sucralose",
            "stevia",
            "aspartame",
        )
        val preservativeTerms = listOf(
            "sorbate",
            "benzoate",
            "nitrate",
            "nitrite",
            "citric acid",
            "ascorbic acid",
            "tocopherol",
            "preservative",
        )
        val colorTerms = listOf("artificial color", "red 40", "yellow 5", "yellow 6", "blue 1", "caramel color", "color")
        val thickenerTerms = listOf("gum", "pectin", "starch", "carrageenan", "cellulose", "gelatin", "thickener")
        val fragranceTerms = listOf("fragrance", "parfum")
        val commonAllergens = listOf("milk", "wheat", "soy", "peanut", "almond", "cashew", "egg", "sesame", "shellfish")
    }
}
