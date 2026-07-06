package com.clearcart.app.domain.alternatives

import com.clearcart.app.data.model.AlternativeSuggestion
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.data.repository.MockProducts
import com.clearcart.app.domain.scoring.ScoringEngine

class AlternativeEngine(private val scoringEngine: ScoringEngine) {
    fun suggestFor(product: Product, preferences: UserPreferences): List<AlternativeSuggestion> {
        val currentScore = scoringEngine.score(product, preferences)
        return MockProducts.alternatives(product.category)
            .filter { it.barcode != product.barcode }
            .map { alt -> alt to scoringEngine.score(alt, preferences) }
            .filter { (_, score) -> score.overallScore > currentScore.overallScore }
            .take(3)
            .map { (alt, score) ->
                AlternativeSuggestion(
                    product = alt,
                    score = score,
                    whyBetter = "Better match based on the same category and your selected preferences.",
                    tradeoff = compareTradeoff(product, alt),
                )
            }
    }

    private fun compareTradeoff(current: Product, alt: Product): String? {
        val currentSugar = current.nutrition?.sugar100g
        val altSugar = alt.nutrition?.sugar100g
        val currentSodium = current.nutrition?.sodium100g
        val altSodium = alt.nutrition?.sodium100g
        return when {
            currentSugar != null && altSugar != null && altSugar < currentSugar && altSodium != null && currentSodium != null && altSodium > currentSodium ->
                "Lower sugar but higher sodium."
            else -> null
        }
    }
}
