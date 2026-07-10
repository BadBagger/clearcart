package com.clearcart.app.domain.alternatives

import com.clearcart.app.data.model.AlternativeSuggestion
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductDataQuality
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.data.repository.MockProducts
import com.clearcart.app.domain.scoring.ScoringEngine
import kotlin.math.abs

class AlternativeEngine(private val scoringEngine: ScoringEngine) {
    fun suggestFor(
        product: Product,
        preferences: UserPreferences,
        candidates: List<Product> = MockProducts.all(),
        hiddenBarcodes: Set<String> = emptySet(),
        includeMockFallback: Boolean = true,
    ): List<AlternativeSuggestion> {
        val current = ProductDataQuality.normalize(product)
        val currentScore = scoringEngine.score(current, preferences)
        if (currentScore.scoreLabel.label == "Great fit" && currentScore.personalFitScore >= 85) return emptyList()

        val candidatePool = if (includeMockFallback) candidates + MockProducts.all() else candidates
        return candidatePool
            .map { ProductDataQuality.normalize(it) }
            .distinctBy { it.barcode }
            .filter { it.barcode != current.barcode }
            .filter { it.barcode !in hiddenBarcodes }
            .filter { hasUsefulData(it) }
            .mapNotNull { alt ->
                val altScore = scoringEngine.score(alt, preferences)
                val fit = matchScore(current, alt)
                val conflictDelta = currentScore.preferenceConflicts.size - altScore.preferenceConflicts.size
                val isBetter = altScore.overallScore >= currentScore.overallScore + 5 ||
                    altScore.personalFitScore >= currentScore.personalFitScore + 8 ||
                    conflictDelta > 0
                if (!isBetter || fit < 45) return@mapNotNull null
                RankedAlternative(
                    product = alt,
                    score = altScore,
                    matchScore = fit +
                        ((altScore.overallScore - currentScore.overallScore).coerceAtLeast(0) / 2) +
                        ((altScore.personalFitScore - currentScore.personalFitScore).coerceAtLeast(0) / 2) +
                        (conflictDelta.coerceAtLeast(0) * 10) +
                        confidenceBonus(alt),
                    suggestion = buildSuggestion(current, currentScore, alt, altScore, fit),
                )
            }
            .sortedByDescending { it.matchScore }
            .take(3)
            .map { it.suggestion.copy(matchScore = it.matchScore) }
    }

    private fun buildSuggestion(
        current: Product,
        currentScore: ProductScore,
        alt: Product,
        altScore: ProductScore,
        fit: Int,
    ): AlternativeSuggestion {
        val better = whatIsBetter(current, currentScore, alt, altScore)
        val concern = concern(altScore, alt)
        return AlternativeSuggestion(
            product = alt,
            score = altScore,
            whyBetter = whySuggested(current, alt, fit),
            whatIsBetter = better,
            tradeoff = tradeoff(current, alt),
            concern = concern,
            dataConfidence = alt.confidenceLevel,
        )
    }

    private fun whySuggested(current: Product, alt: Product, fit: Int): String = when {
        sameCategory(current, alt) -> "Same category with a stronger ClearCart or Personal Fit signal."
        sameUseCase(current, alt) -> "Similar use case with a stronger fit based on available data."
        current.productType == alt.productType -> "Similar product type with better available signals."
        else -> "Related product with better available signals."
    } + " Match confidence: ${fit.coerceIn(0, 100)}/100."

    private fun whatIsBetter(current: Product, currentScore: ProductScore, alt: Product, altScore: ProductScore): String {
        val improvements = buildList {
            val currentSugar = current.nutrition?.sugar100g
            val altSugar = alt.nutrition?.sugar100g
            if (currentSugar != null && altSugar != null && altSugar < currentSugar) add("lower sugar")
            if (alt.additives.size < current.additives.size) add("fewer listed additives")
            if (alt.ingredientsText.split(',').size < current.ingredientsText.split(',').size) add("simpler visible ingredient list")
            if (altScore.preferenceConflicts.size < currentScore.preferenceConflicts.size) add("fewer preference conflicts")
            if (altScore.personalFitScore > currentScore.personalFitScore) add("better Personal Fit")
            if (altScore.overallScore > currentScore.overallScore) add("higher ClearCart Score")
        }
        return improvements.take(3).ifEmpty { listOf("better available score signals") }.joinToString()
    }

    private fun concern(score: ProductScore, product: Product): String? = when {
        score.confidenceScore < 55 -> "Data is partial, so compare the label before relying on this suggestion."
        score.preferenceConflicts.isNotEmpty() -> score.preferenceConflicts.first().text
        product.ingredientsText.isBlank() -> "Ingredient data is incomplete."
        else -> null
    }

    private fun tradeoff(current: Product, alt: Product): String? {
        val currentSugar = current.nutrition?.sugar100g
        val altSugar = alt.nutrition?.sugar100g
        val currentSodium = current.nutrition?.sodium100g
        val altSodium = alt.nutrition?.sodium100g
        return when {
            currentSugar != null && altSugar != null && altSugar < currentSugar && altSodium != null && currentSodium != null && altSodium > currentSodium ->
                "Lower sugar but higher sodium."
            similarQuantity(current.quantity, alt.quantity).not() && !alt.quantity.isNullOrBlank() ->
                "Similar product, but package size may differ."
            else -> null
        }
    }

    private fun matchScore(current: Product, alt: Product): Int {
        var score = 0
        if (sameCategory(current, alt)) score += 42
        if (sameUseCase(current, alt)) score += 24
        if (current.productType == alt.productType) score += 18
        if (similarQuantity(current.quantity, alt.quantity)) score += 8
        if (current.category.isNotBlank() && alt.category.contains(current.category, ignoreCase = true)) score += 8
        return score.coerceIn(0, 100)
    }

    private fun hasUsefulData(product: Product): Boolean {
        if (product.confidenceLevel == ConfidenceLevel.MissingData || product.confidenceLevel == ConfidenceLevel.UserEntered) return false
        if (product.dataCompletenessScore < 45) return false
        return product.name.isNotBlank() && product.category.isNotBlank()
    }

    private fun confidenceBonus(product: Product): Int = when (product.confidenceLevel) {
        ConfidenceLevel.High -> 10
        ConfidenceLevel.Medium -> 5
        ConfidenceLevel.Low -> 0
        ConfidenceLevel.MissingData, ConfidenceLevel.UserEntered -> -20
    }

    private fun sameCategory(current: Product, alt: Product): Boolean =
        current.category.isNotBlank() && current.category.equals(alt.category, ignoreCase = true)

    private fun sameUseCase(current: Product, alt: Product): Boolean {
        val currentTerms = useCaseTerms(current)
        val altTerms = useCaseTerms(alt)
        return currentTerms.intersect(altTerms).isNotEmpty()
    }

    private fun useCaseTerms(product: Product): Set<String> {
        val text = "${product.category} ${product.name}".lowercase()
        return buildSet {
            if (text.contains("soda") || text.contains("cola") || text.contains("sparkling")) add("carbonated drink")
            if (text.contains("cereal") || text.contains("oat") || text.contains("flakes")) add("breakfast cereal")
            if (text.contains("shampoo") || text.contains("hair")) add("hair care")
            if (text.contains("body wash") || text.contains("soap")) add("body wash")
            if (text.contains("snack") || text.contains("chip")) add("snack")
            if (product.productType == ProductType.Cosmetic) add("personal care")
            if (product.productType == ProductType.Household) add("household")
        }
    }

    private fun similarQuantity(current: String?, alt: String?): Boolean {
        if (current.isNullOrBlank() || alt.isNullOrBlank()) return false
        val currentValue = current.extractFirstNumber() ?: return false
        val altValue = alt.extractFirstNumber() ?: return false
        if (currentValue == 0.0) return false
        return abs(currentValue - altValue) / currentValue <= 0.35
    }

    private fun String.extractFirstNumber(): Double? =
        Regex("""\d+(\.\d+)?""").find(this)?.value?.toDoubleOrNull()

    private data class RankedAlternative(
        val product: Product,
        val score: ProductScore,
        val matchScore: Int,
        val suggestion: AlternativeSuggestion,
    )
}
