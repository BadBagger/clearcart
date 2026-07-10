package com.clearcart.app.domain.categories

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.scoring.ScoringEngine

data class CategoryPick(
    val categoryName: String,
    val product: Product,
    val score: ProductScore,
    val whyPicked: String,
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
            val candidates = products
                .filter { product -> product.matchesCategory(category) }
                .distinctBy { it.barcode.ifBlank { it.id } }
                .map { product -> product to scoringEngine.score(product, preferences) }
                .filter { (_, score) -> score.confidenceScore >= 45 }

            val best = candidates.maxWithOrNull(
                compareBy<Pair<Product, ProductScore>> { it.second.overallScore }
                    .thenBy { it.second.personalFitScore }
                    .thenBy { it.first.confidenceLevel.rank() }
                    .thenBy { it.first.dataCompletenessScore }
            ) ?: return@mapNotNull null

            CategoryPick(
                categoryName = category,
                product = best.first,
                score = best.second,
                whyPicked = whyPicked(best.first, best.second),
            )
        }
    }

    private fun Product.matchesCategory(category: String): Boolean {
        val normalizedCategory = category.lowercase()
        val searchable = listOf(name, brand, this.category, ingredientsText, rawResponse.orEmpty())
            .joinToString(" ")
            .lowercase()
        return normalizedCategory.split(" ")
            .filter { it.length >= 3 }
            .any { searchable.contains(it) }
    }

    private fun whyPicked(product: Product, score: ProductScore): String {
        val reasons = buildList {
            add("highest ClearCart Score in this category set")
            if (score.personalFitScore >= score.overallScore) add("strong Personal Fit")
            if (product.dataCompletenessScore >= 80) add("good data completeness")
            score.positiveList.firstOrNull()?.let { add(it.text.replaceFirstChar { char -> char.lowercase() }) }
        }
        return reasons.distinct().take(3).joinToString(", ").replaceFirstChar { it.uppercase() } + "."
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
