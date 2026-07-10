package com.clearcart.app.domain.categories

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.preferences.ProductPreferenceFilter
import com.clearcart.app.domain.scoring.ScoringEngine

data class CategoryPick(
    val categoryName: String,
    val product: Product,
    val score: ProductScore,
)

class CategoryBestEngine(
    private val scoringEngine: ScoringEngine = ScoringEngine(),
) {
    fun bestByCategory(
        products: List<Product>,
        preferences: UserPreferences,
        categories: List<String> = defaultCategories,
    ): List<CategoryPick> {
        return categories.mapNotNull { category ->
            rankedProductsForCategory(category, products, preferences).firstOrNull()
        }
    }

    fun rankedProductsForCategory(
        category: String,
        products: List<Product>,
        preferences: UserPreferences,
        limit: Int = 20,
    ): List<CategoryPick> {
        return products
            .filter { product -> product.matchesCategory(category) }
            .filter { product -> ProductPreferenceFilter.visibleInRecommendationLists(product, preferences) }
            .distinctBy { it.barcode.ifBlank { it.id } }
            .map { product -> product to scoringEngine.score(product, preferences) }
            .filter { (_, score) -> score.confidenceScore >= 45 }
            .sortedWith(
                compareByDescending<Pair<Product, ProductScore>> { it.second.overallScore }
                    .thenByDescending { it.second.personalFitScore }
                    .thenByDescending { it.first.confidenceLevel.rank() }
                    .thenByDescending { it.first.dataCompletenessScore }
            )
            .take(limit)
            .map { (product, score) ->
                CategoryPick(
                    categoryName = category,
                    product = product,
                    score = score,
                )
            }
    }

    fun hasCategoryMatch(category: String, products: List<Product>): Boolean =
        products.any { it.matchesCategory(category) }

    fun hiddenByPreferenceCount(
        category: String,
        products: List<Product>,
        preferences: UserPreferences,
    ): Int =
        products
            .filter { it.matchesCategory(category) }
            .count { !ProductPreferenceFilter.visibleInRecommendationLists(it, preferences) }

    private fun Product.matchesCategory(category: String): Boolean {
        val normalizedCategory = category.lowercase()
        val searchable = listOf(name, brand, this.category, ingredientsText, rawResponse.orEmpty())
            .joinToString(" ")
            .lowercase()
        return normalizedCategory.split(" ")
            .filter { it.length >= 3 }
            .any { searchable.contains(it) }
    }

    private fun ConfidenceLevel.rank(): Int = when (this) {
        ConfidenceLevel.High -> 5
        ConfidenceLevel.Medium -> 4
        ConfidenceLevel.Low -> 3
        ConfidenceLevel.UserEntered -> 2
        ConfidenceLevel.MissingData -> 1
    }
}

val defaultCategories = listOf(
    "Protein shakes",
    "Cereal",
    "Soda",
    "Sparkling drinks",
    "Shampoo",
    "Body wash",
)
