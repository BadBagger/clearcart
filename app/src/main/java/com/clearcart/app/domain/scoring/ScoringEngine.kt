package com.clearcart.app.domain.scoring

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Grade
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.ScoreNote
import com.clearcart.app.data.model.ScoreSubscore
import com.clearcart.app.data.model.UserPreferences
import kotlin.math.roundToInt

class ScoringEngine(
    private val foodWeights: Map<String, Double> = mapOf(
        "nutrition" to 0.22,
        "processing" to 0.14,
        "additives" to 0.12,
        "sugar" to 0.10,
        "sodium" to 0.08,
        "saturatedFat" to 0.08,
        "fiber" to 0.07,
        "protein" to 0.06,
        "allergen" to 0.05,
        "preference" to 0.05,
        "clarity" to 0.02,
        "completeness" to 0.01,
    ),
    private val cosmeticWeights: Map<String, Double> = mapOf(
        "ingredientConcern" to 0.30,
        "fragrance" to 0.18,
        "allergen" to 0.16,
        "sensitivity" to 0.16,
        "clarity" to 0.10,
        "completeness" to 0.10,
    ),
) {
    fun score(product: Product, preferences: UserPreferences): ProductScore {
        val subscores = if (product.type == ProductType.Cosmetic) cosmeticSubscores(product, preferences) else foodSubscores(product, preferences)
        val weights = (if (product.type == ProductType.Cosmetic) cosmeticWeights else foodWeights) + preferences.scoringWeightOverrides
        val overall = subscores.sumOf { (weights[it.nameKey()] ?: 0.0) * it.score }.roundToInt().coerceIn(0, 100)
        val positives = mutableListOf<ScoreNote>()
        val cautions = mutableListOf<ScoreNote>()
        val explanations = mutableListOf<ScoreNote>()

        product.nutrition?.let {
            if ((it.sugar100g ?: 0.0) > 12) cautions += ScoreNote("Higher sugar than many similar products", "${it.sugar100g}g sugar per 100g.")
            if ((it.fiber100g ?: 0.0) >= 5) positives += ScoreNote("Good fiber content", "${it.fiber100g}g fiber per 100g.")
            if ((it.saturatedFat100g ?: 0.0) <= 1.5) positives += ScoreNote("Low saturated fat", "${it.saturatedFat100g}g saturated fat per 100g.")
        }
        if ((product.novaGroup ?: 0) >= 4) cautions += ScoreNote("More processed product profile", "NOVA group ${product.novaGroup}; this is a general food-processing signal.")
        if (product.additives.isNotEmpty()) cautions += ScoreNote("Contains ${product.additives.size} listed additives", "Additives are listed by the source database; concern depends on context.")
        if (preferences.lowSugar && (product.nutrition?.sugar100g ?: 0.0) > 8) cautions += ScoreNote("Flagged based on your low sugar preference", "This is a preference match issue, not a medical claim.", true)
        if (preferences.lowSodium && (product.nutrition?.sodium100g ?: 0.0) > 0.3) cautions += ScoreNote("Flagged based on your low sodium preference", "Sodium is ${product.nutrition?.sodium100g}g per 100g.", true)
        if (preferences.avoidFragrance && product.ingredientsText.contains("fragrance", true)) cautions += ScoreNote("Contains fragrance", "Some sensitive-skin users prefer to avoid fragrance.", true)
        val allergenConflicts = product.allergens.map { it.lowercase() }.intersect(preferences.allergensToAvoid.map { it.lowercase() }.toSet())
        if (allergenConflicts.isNotEmpty()) cautions += ScoreNote("Contains allergens you chose to avoid", allergenConflicts.joinToString(), true)
        if (product.confidenceLevel != ConfidenceLevel.High) explanations += ScoreNote("Ingredient or nutrition data is incomplete", "Confidence is ${product.confidenceLevel.display()}.")
        explanations += ScoreNote("Score combines separate signals", "Weights are configurable in code and raw facts remain visible.")

        return ProductScore(
            overallScore = overall,
            grade = gradeFor(overall),
            confidenceLevel = product.confidenceLevel,
            subscores = subscores,
            explanationList = explanations,
            positiveList = positives,
            cautionList = cautions,
        )
    }

    private fun foodSubscores(product: Product, preferences: UserPreferences): List<ScoreSubscore> {
        val n = product.nutrition
        val conflicts = product.allergens.map { it.lowercase() }.intersect(preferences.allergensToAvoid.map { it.lowercase() }.toSet())
        return listOf(
            ScoreSubscore("Nutrition quality", scoreNutri(product.nutriScore), "Based on public nutrition grade when available."),
            ScoreSubscore("Ultra-processing / NOVA group", when (product.novaGroup) { 1 -> 95; 2 -> 80; 3 -> 62; 4 -> 38; else -> 55 }, "NOVA is a processing signal, not a moral label."),
            ScoreSubscore("Additives", (100 - product.additives.size * 12).coerceAtLeast(35), "${product.additives.size} additives listed."),
            ScoreSubscore("Sugar", thresholdScore(n?.sugar100g, 5.0, 12.0, true), "${n?.sugar100g ?: "Missing"}g per 100g."),
            ScoreSubscore("Sodium", thresholdScore(n?.sodium100g, 0.12, 0.5, true), "${n?.sodium100g ?: "Missing"}g per 100g."),
            ScoreSubscore("Saturated fat", thresholdScore(n?.saturatedFat100g, 1.5, 5.0, true), "${n?.saturatedFat100g ?: "Missing"}g per 100g."),
            ScoreSubscore("Fiber", thresholdScore(n?.fiber100g, 3.0, 6.0, false), "${n?.fiber100g ?: "Missing"}g per 100g."),
            ScoreSubscore("Protein", thresholdScore(n?.protein100g, 5.0, 12.0, false), "${n?.protein100g ?: "Missing"}g per 100g."),
            ScoreSubscore("Allergen match", if (conflicts.isEmpty()) 100 else 25, if (conflicts.isEmpty()) "No selected allergen conflict found." else "Matches ${conflicts.joinToString()}."),
            ScoreSubscore("User preference match", preferenceScore(product, preferences), "Reflects the preferences you selected."),
            ScoreSubscore("Ingredient clarity", ingredientClarity(product), "Readable ingredient list signal."),
            ScoreSubscore("Data completeness", completeness(product), "Confidence depends on available product fields."),
        )
    }

    private fun cosmeticSubscores(product: Product, preferences: UserPreferences): List<ScoreSubscore> {
        val hasFragrance = product.ingredientsText.contains("fragrance", true) || product.ingredientsText.contains("parfum", true)
        return listOf(
            ScoreSubscore("Ingredient concern level", if (product.ingredientsText.isBlank()) 45 else 72, "Basic ingredient list review; no medical claim."),
            ScoreSubscore("Fragrance/parfum presence", if (hasFragrance) 55 else 95, if (hasFragrance) "Fragrance is present." else "No fragrance wording found."),
            ScoreSubscore("Allergen/sensitivity flags", if (product.allergens.isEmpty()) 85 else 60, "${product.allergens.size} sensitivity flags listed."),
            ScoreSubscore("User sensitivity match", if ((preferences.avoidFragrance || preferences.sensitiveSkin) && hasFragrance) 45 else 90, "Reflects your skin preferences."),
            ScoreSubscore("Ingredient clarity", ingredientClarity(product), "Readable ingredient list signal."),
            ScoreSubscore("Data completeness", completeness(product), "Confidence depends on available product fields."),
        )
    }

    private fun ScoreSubscore.nameKey() = name.lowercase()
        .replace(" / nova group", "")
        .replace("ultra-processing", "processing")
        .replace("nutrition quality", "nutrition")
        .replace("saturated fat", "saturatedFat")
        .replace("allergen match", "allergen")
        .replace("user preference match", "preference")
        .replace("ingredient clarity", "clarity")
        .replace("data completeness", "completeness")
        .replace("fragrance/parfum presence", "fragrance")
        .replace("allergen/sensitivity flags", "allergen")
        .replace("user sensitivity match", "sensitivity")
        .replace(" ", "")

    private fun scoreNutri(grade: String?) = when (grade?.lowercase()) {
        "a" -> 95; "b" -> 82; "c" -> 65; "d" -> 45; "e" -> 28; else -> 58
    }

    private fun thresholdScore(value: Double?, good: Double, caution: Double, lowerIsBetter: Boolean): Int {
        if (value == null) return 50
        return if (lowerIsBetter) {
            when { value <= good -> 95; value >= caution -> 35; else -> 75 }
        } else {
            when { value >= caution -> 95; value >= good -> 78; else -> 48 }
        }
    }

    private fun preferenceScore(product: Product, preferences: UserPreferences): Int {
        var score = 100
        val text = product.ingredientsText.lowercase()
        if (preferences.simpleIngredients && product.ingredientsText.split(',').size > 12) score -= 15
        if (preferences.preferOrganic && product.labels.none { it.contains("organic", true) }) score -= 6
        if (preferences.glutenFree && ("gluten" in product.allergens.map { it.lowercase() } || text.contains("wheat"))) score -= 35
        if (preferences.dairyFree && ("milk" in product.allergens.map { it.lowercase() } || text.contains("milk"))) score -= 35
        if (preferences.avoidArtificialColors && text.contains("color")) score -= 15
        return score.coerceIn(20, 100)
    }

    private fun ingredientClarity(product: Product): Int = when {
        product.ingredientsText.isBlank() -> 35
        product.ingredientsText.length < 40 -> 72
        product.ingredientsText.split(',').size <= 8 -> 90
        else -> 70
    }

    private fun completeness(product: Product): Int = when (product.confidenceLevel) {
        ConfidenceLevel.High -> 95
        ConfidenceLevel.Medium -> 72
        ConfidenceLevel.Low -> 48
        ConfidenceLevel.MissingData -> 30
        ConfidenceLevel.UserEntered -> 45
    }

    private fun gradeFor(score: Int) = when {
        score >= 86 -> Grade.Excellent
        score >= 72 -> Grade.Good
        score >= 56 -> Grade.Okay
        score >= 40 -> Grade.Caution
        else -> Grade.AvoidOften
    }
}

fun ConfidenceLevel.display(): String = when (this) {
    ConfidenceLevel.High -> "High confidence"
    ConfidenceLevel.Medium -> "Medium confidence"
    ConfidenceLevel.Low -> "Low confidence"
    ConfidenceLevel.MissingData -> "Missing data"
    ConfidenceLevel.UserEntered -> "User-entered"
}
