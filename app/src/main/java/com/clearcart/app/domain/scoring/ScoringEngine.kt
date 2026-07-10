package com.clearcart.app.domain.scoring

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Grade
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductDataQuality
import com.clearcart.app.data.model.ProductType
import com.clearcart.app.data.model.ScoreLabel
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.model.ScoreNote
import com.clearcart.app.data.model.ScoreSubscore
import com.clearcart.app.data.model.UserPreferences
import java.text.DecimalFormat
import kotlin.math.roundToInt

class ScoringEngine(
    private val foodWeights: Map<String, Double> = mapOf(
        "nutritionBalance" to 0.25,
        "addedSugar" to 0.16,
        "sodium" to 0.12,
        "saturatedFat" to 0.11,
        "additives" to 0.12,
        "ingredientSimplicity" to 0.14,
        "allergenPreferenceMatch" to 0.10,
        "dataCompleteness" to 0.0,
    ),
    private val cosmeticWeights: Map<String, Double> = mapOf(
        "ingredientClarity" to 0.30,
        "fragranceFlag" to 0.22,
        "userPreferenceMatch" to 0.18,
        "sensitiveSkinPreferenceMatch" to 0.16,
        "ingredientConcernNotes" to 0.14,
        "dataCompleteness" to 0.0,
    ),
    private val householdWeights: Map<String, Double> = mapOf(
        "labelClarity" to 0.32,
        "fragranceFlag" to 0.22,
        "userPreferenceMatch" to 0.24,
        "safetyUsageNote" to 0.22,
        "dataCompleteness" to 0.0,
    ),
) {
    fun score(product: Product, preferences: UserPreferences): ProductScore {
        val normalized = ProductDataQuality.normalize(product)
        val subscores = when (normalized.productType) {
            ProductType.Cosmetic -> cosmeticSubscores(normalized, preferences)
            ProductType.Household -> householdSubscores(normalized, preferences)
            ProductType.Food, ProductType.Drink, ProductType.Unknown -> foodSubscores(normalized, preferences)
        }
        val weights = weightsFor(normalized.productType) + preferences.scoringWeightOverrides
        val overall = weightedScore(subscores, weights)
        val confidenceScore = confidenceScore(normalized)
        val scoreLabel = scoreLabelFor(overall, confidenceScore)
        val personalFitScore = personalFitScore(normalized, preferences, subscores)
        val personalFitLabel = scoreLabelFor(personalFitScore, confidenceScore)
        val positives = positiveNotes(normalized)
        val preferenceMatches = preferenceMatches(normalized, preferences)
        val preferenceConflicts = preferenceConflicts(normalized, preferences)
        val cautionList = cautionNotes(normalized, preferences) + preferenceConflicts
        val missingDataWarnings = missingDataWarnings(normalized)
        val explanations = explanationNotes(normalized)
        val topReasons = (cautionList + positives + missingDataWarnings + explanations).take(3)

        return ProductScore(
            overallScore = overall,
            grade = gradeFor(overall),
            confidenceLevel = normalized.confidenceLevel,
            subscores = subscores,
            explanationList = explanations,
            positiveList = positives,
            cautionList = cautionList,
            scoreLabel = scoreLabel,
            confidenceScore = confidenceScore,
            personalFitScore = personalFitScore,
            personalFitLabel = personalFitLabel,
            explanationSummary = explanationSummary(scoreLabel, missingDataWarnings.isNotEmpty()),
            scoreBreakdown = subscores,
            topReasons = topReasons,
            preferenceMatches = preferenceMatches,
            preferenceConflicts = preferenceConflicts,
            missingDataWarnings = missingDataWarnings,
        )
    }

    private fun foodSubscores(product: Product, preferences: UserPreferences): List<ScoreSubscore> {
        val n = product.nutrition
        val conflicts = allergenConflicts(product, preferences)
        return listOf(
            ScoreSubscore("Nutrition balance", scoreNutri(product.nutriScore), "Based on public nutrition grade when available."),
            ScoreSubscore("Added sugar", thresholdScore(n?.sugar100g, 5.0, 12.0, true), "${formatGrams(n?.sugar100g)} per 100g."),
            ScoreSubscore("Sodium", thresholdScore(n?.sodium100g, 0.12, 0.5, true), "${formatGrams(n?.sodium100g)} per 100g."),
            ScoreSubscore("Saturated fat", thresholdScore(n?.saturatedFat100g, 1.5, 5.0, true), "${formatGrams(n?.saturatedFat100g)} per 100g."),
            ScoreSubscore("Additives", (100 - product.additives.size * 12).coerceAtLeast(35), "${product.additives.size} additives listed."),
            ScoreSubscore("Ingredient simplicity", ingredientSimplicity(product), "Looks at ingredient list length and readability."),
            ScoreSubscore(
                "Allergen/preference match",
                if (conflicts.isEmpty()) 100 else 25,
                if (conflicts.isEmpty()) "No selected allergen conflict found." else "Matches ${conflicts.joinToString()}.",
            ),
            ScoreSubscore("Data completeness", completeness(product), "Affects confidence more than the product score."),
        )
    }

    private fun cosmeticSubscores(product: Product, preferences: UserPreferences): List<ScoreSubscore> {
        val hasFragrance = hasFragrance(product)
        return listOf(
            ScoreSubscore("Ingredient clarity", ingredientSimplicity(product), "Readable ingredient list signal."),
            ScoreSubscore("Fragrance/parfum flag", if (hasFragrance) 55 else 95, if (hasFragrance) "Fragrance or parfum wording is present." else "No fragrance or parfum wording found."),
            ScoreSubscore("User preference match", preferenceScore(product, preferences), "Reflects preferences you selected without hiding the raw facts."),
            ScoreSubscore("Sensitive-skin preference match", if (preferences.sensitiveSkin && hasFragrance) 45 else 90, "Shown as a preference fit signal."),
            ScoreSubscore("Ingredient concern notes", if (product.ingredientsText.isBlank()) 45 else 72, "Basic ingredient review, worded as context rather than a medical claim."),
            ScoreSubscore("Data completeness", completeness(product), "Affects confidence more than the product score."),
        )
    }

    private fun householdSubscores(product: Product, preferences: UserPreferences): List<ScoreSubscore> {
        val hasFragrance = hasFragrance(product)
        return listOf(
            ScoreSubscore("Label clarity", ingredientSimplicity(product), "Looks at whether label or ingredient details are available."),
            ScoreSubscore("Fragrance flag", if (hasFragrance) 58 else 92, if (hasFragrance) "Fragrance wording is present." else "No fragrance wording found."),
            ScoreSubscore("User preference match", preferenceScore(product, preferences), "Reflects preferences you selected."),
            ScoreSubscore("Safety/usage note", if (product.labels.isNotEmpty()) 78 else 58, if (product.labels.isNotEmpty()) "Usage labels are available." else "Usage details are limited in the current data."),
            ScoreSubscore("Data completeness", completeness(product), "Affects confidence more than the product score."),
        )
    }

    private fun weightsFor(type: ProductType): Map<String, Double> = when (type) {
        ProductType.Cosmetic -> cosmeticWeights
        ProductType.Household -> householdWeights
        ProductType.Food, ProductType.Drink, ProductType.Unknown -> foodWeights
    }

    private fun weightedScore(subscores: List<ScoreSubscore>, weights: Map<String, Double>): Int {
        val weighted = subscores.sumOf { (weights[it.nameKey()] ?: 0.0) * it.score }
        val activeWeight = subscores.sumOf { weights[it.nameKey()] ?: 0.0 }.takeIf { it > 0.0 } ?: 1.0
        return (weighted / activeWeight).roundToInt().coerceIn(0, 100)
    }

    private fun positiveNotes(product: Product): List<ScoreNote> = buildList {
        product.nutrition?.let {
            it.fiber100g?.let { value ->
                if (value >= 5) add(ScoreNote("Good fiber content", "${formatGrams(value)} fiber per 100g."))
            }
            it.saturatedFat100g?.let { value ->
                if (value <= 1.5) add(ScoreNote("Lower saturated fat", "${formatGrams(value)} saturated fat per 100g."))
            }
            it.sugar100g?.let { value ->
                if (value <= 5) add(ScoreNote("Lower sugar", "${formatGrams(value)} sugar per 100g."))
            }
        }
        if (product.additives.isEmpty() && product.ingredientsText.isNotBlank()) {
            add(ScoreNote("No listed additives", "The current source data does not list additives for this product."))
        }
    }

    private fun cautionNotes(product: Product, preferences: UserPreferences): List<ScoreNote> = buildList {
        product.nutrition?.let {
            it.sugar100g?.let { value ->
                if (value > 12) add(ScoreNote("Higher sugar", "${formatGrams(value)} sugar per 100g."))
            }
            it.sodium100g?.let { value ->
                if (value > 0.5) add(ScoreNote("Higher sodium", "${formatGrams(value)} sodium per 100g."))
            }
            it.saturatedFat100g?.let { value ->
                if (value > 5) add(ScoreNote("Higher saturated fat", "${formatGrams(value)} saturated fat per 100g."))
            }
        }
        if ((product.novaGroup ?: 0) >= 4) {
            add(ScoreNote("More processed product profile", "NOVA group ${product.novaGroup}; this is a food-processing signal, not a moral label."))
        }
        if (product.additives.size >= 3) {
            add(ScoreNote("Contains several additives", "${product.additives.size} additives are listed by the source database."))
        } else if (product.additives.isNotEmpty()) {
            add(ScoreNote("Contains listed additives", "${product.additives.size} additive entries are listed by the source database."))
        }
        if (preferences.lowSugar && (product.nutrition?.sugar100g ?: 0.0) > 8) {
            add(ScoreNote("Flagged based on your low sugar preference", "This is a preference fit signal.", true))
        }
        if (preferences.lowSodium && (product.nutrition?.sodium100g ?: 0.0) > 0.3) {
            add(ScoreNote("Flagged based on your low sodium preference", "Sodium is ${formatGrams(product.nutrition?.sodium100g)} per 100g.", true))
        }
        if ((preferences.avoidFragrance || preferences.sensitiveSkin) && hasFragrance(product)) {
            add(ScoreNote("Contains fragrance/parfum wording", "Some users with this preference choose fragrance-free products.", true))
        }
    }

    private fun preferenceMatches(product: Product, preferences: UserPreferences): List<ScoreNote> = buildList {
        if (preferences.lowSugar && (product.nutrition?.sugar100g ?: Double.MAX_VALUE) <= 5) {
            add(ScoreNote("Matches low sugar preference", "${formatGrams(product.nutrition?.sugar100g)} sugar per 100g.", true))
        }
        if (preferences.lowSodium && (product.nutrition?.sodium100g ?: Double.MAX_VALUE) <= 0.12) {
            add(ScoreNote("Matches low sodium preference", "${formatGrams(product.nutrition?.sodium100g)} sodium per 100g.", true))
        }
        if (preferences.simpleIngredients && product.ingredientsText.isNotBlank() && product.ingredientsText.split(',').size <= 8) {
            add(ScoreNote("Matches simpler ingredient preference", "The visible ingredient list is relatively short.", true))
        }
        if ((preferences.avoidFragrance || preferences.sensitiveSkin) && product.ingredientsText.isNotBlank() && !hasFragrance(product)) {
            add(ScoreNote("No fragrance wording found", "Matches the fragrance-related preferences you selected.", true))
        }
    }

    private fun preferenceConflicts(product: Product, preferences: UserPreferences): List<ScoreNote> = buildList {
        val allergenConflicts = allergenConflicts(product, preferences)
        if (allergenConflicts.isNotEmpty()) {
            add(ScoreNote("Contains allergens you chose to avoid", allergenConflicts.joinToString(), true))
        }
        if (preferences.lowSugar && (product.nutrition?.sugar100g ?: 0.0) > 8) {
            add(ScoreNote("Flagged based on your low sugar preference", "${formatGrams(product.nutrition?.sugar100g)} sugar per 100g.", true))
        }
        if (preferences.lowSodium && (product.nutrition?.sodium100g ?: 0.0) > 0.3) {
            add(ScoreNote("Flagged based on your low sodium preference", "${formatGrams(product.nutrition?.sodium100g)} sodium per 100g.", true))
        }
        val text = product.ingredientsText.lowercase()
        if (preferences.glutenFree && ("gluten" in product.allergens.map { it.lowercase() } || text.contains("wheat"))) {
            add(ScoreNote("Flagged based on your gluten-free preference", "Ingredient or allergen text includes gluten-related wording.", true))
        }
        if (preferences.dairyFree && ("milk" in product.allergens.map { it.lowercase() } || text.contains("milk"))) {
            add(ScoreNote("Flagged based on your dairy-free preference", "Ingredient or allergen text includes milk-related wording.", true))
        }
        if (preferences.avoidArtificialColors && text.contains("color")) {
            add(ScoreNote("Flagged based on your color additive preference", "Ingredient text includes color wording.", true))
        }
    }

    private fun missingDataWarnings(product: Product): List<ScoreNote> {
        val copy = ProductDataQuality.missingDataCopy(product)
        return if (copy.startsWith("Missing:")) {
            listOf(ScoreNote("Data incomplete", "$copy ClearCart can still help, but this score may be less confident."))
        } else {
            emptyList()
        }
    }

    private fun explanationNotes(product: Product): List<ScoreNote> = buildList {
        if (product.confidenceLevel != ConfidenceLevel.High) {
            add(ScoreNote("Confidence reflects available data", "Current confidence is ${product.confidenceLevel.display()}."))
        }
        add(ScoreNote("Score combines separate signals", "ClearCart keeps nutrition, ingredients, preferences, and data completeness visible."))
    }

    private fun explanationSummary(label: ScoreLabel, hasMissingData: Boolean): String = when {
        hasMissingData -> "Some product details are missing. ClearCart can still help, but this score may be less confident."
        label == ScoreLabel.GreatFit -> "This looks like a strong fit based on the product details ClearCart can read."
        label == ScoreLabel.GoodOption -> "This looks like a good option based on the available product details."
        label == ScoreLabel.WorthReviewing -> "A few details are worth reviewing before making this an everyday pick."
        label == ScoreLabel.LimitedData -> "Limited product data is available, so use the score as a starting point."
        else -> "This product has a mixed profile. Review the breakdown for the details."
    }

    private fun personalFitScore(product: Product, preferences: UserPreferences, subscores: List<ScoreSubscore>): Int {
        val preference = subscores.firstOrNull {
            it.name == "Allergen/preference match" || it.name == "User preference match"
        }?.score ?: 85
        var score = ((preference * 0.65) + (ingredientSimplicity(product) * 0.20) + (completeness(product) * 0.15)).roundToInt()
        if (preferences.lowSugar && (product.nutrition?.sugar100g ?: 0.0) > 8) score -= 18
        if (preferences.lowSodium && (product.nutrition?.sodium100g ?: 0.0) > 0.3) score -= 14
        if ((preferences.avoidFragrance || preferences.sensitiveSkin) && hasFragrance(product)) score -= 18
        if (allergenConflicts(product, preferences).isNotEmpty()) score -= 35
        return score.coerceIn(0, 100)
    }

    private fun scoreLabelFor(score: Int, confidenceScore: Int) = when {
        confidenceScore < 40 -> ScoreLabel.LimitedData
        score >= 85 -> ScoreLabel.GreatFit
        score >= 70 -> ScoreLabel.GoodOption
        score >= 55 -> ScoreLabel.Okay
        else -> ScoreLabel.WorthReviewing
    }

    private fun confidenceScore(product: Product): Int = when (product.confidenceLevel) {
        ConfidenceLevel.High -> product.dataCompletenessScore.coerceAtLeast(85)
        ConfidenceLevel.Medium -> product.dataCompletenessScore.coerceIn(55, 84)
        ConfidenceLevel.Low -> product.dataCompletenessScore.coerceIn(30, 54)
        ConfidenceLevel.UserEntered -> product.dataCompletenessScore.coerceIn(35, 60)
        ConfidenceLevel.MissingData -> product.dataCompletenessScore.coerceIn(0, 35)
    }

    private fun ScoreSubscore.nameKey() = name.lowercase()
        .replace("nutrition balance", "nutritionBalance")
        .replace("added sugar", "addedSugar")
        .replace("saturated fat", "saturatedFat")
        .replace("ingredient simplicity", "ingredientSimplicity")
        .replace("allergen/preference match", "allergenPreferenceMatch")
        .replace("ingredient clarity", "ingredientClarity")
        .replace("fragrance/parfum flag", "fragranceFlag")
        .replace("fragrance flag", "fragranceFlag")
        .replace("sensitive-skin preference match", "sensitiveSkinPreferenceMatch")
        .replace("ingredient concern notes", "ingredientConcernNotes")
        .replace("label clarity", "labelClarity")
        .replace("safety/usage note", "safetyUsageNote")
        .replace("user preference match", "userPreferenceMatch")
        .replace("data completeness", "dataCompleteness")
        .replace(" ", "")

    private fun scoreNutri(grade: String?) = when (grade?.lowercase()) {
        "a" -> 95
        "b" -> 82
        "c" -> 65
        "d" -> 45
        "e" -> 28
        else -> 65
    }

    private fun thresholdScore(value: Double?, good: Double, caution: Double, lowerIsBetter: Boolean): Int {
        if (value == null) return 65
        return if (lowerIsBetter) {
            when {
                value <= good -> 95
                value >= caution -> 35
                else -> 75
            }
        } else {
            when {
                value >= caution -> 95
                value >= good -> 78
                else -> 48
            }
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
        if ((preferences.avoidFragrance || preferences.sensitiveSkin) && hasFragrance(product)) score -= 25
        return score.coerceIn(20, 100)
    }

    private fun ingredientSimplicity(product: Product): Int = when {
        product.ingredientsText.isBlank() -> 65
        product.ingredientsText.length < 40 -> 78
        product.ingredientsText.split(',').size <= 8 -> 92
        product.ingredientsText.split(',').size <= 14 -> 74
        else -> 58
    }

    private fun completeness(product: Product): Int = product.dataCompletenessScore.coerceIn(0, 100)

    private fun allergenConflicts(product: Product, preferences: UserPreferences): Set<String> =
        product.allergens.map { it.lowercase() }.intersect(preferences.allergensToAvoid.map { it.lowercase() }.toSet())

    private fun hasFragrance(product: Product): Boolean =
        product.ingredientsText.contains("fragrance", true) || product.ingredientsText.contains("parfum", true)

    private fun gradeFor(score: Int) = when {
        score >= 86 -> Grade.Excellent
        score >= 72 -> Grade.Good
        score >= 56 -> Grade.Okay
        score >= 40 -> Grade.Caution
        else -> Grade.AvoidOften
    }

    private fun formatGrams(value: Double?): String {
        if (value == null) return "Missing"
        return "${DecimalFormat("0.##").format(value)} g"
    }
}

fun ConfidenceLevel.display(): String = when (this) {
    ConfidenceLevel.High -> "High confidence"
    ConfidenceLevel.Medium -> "Medium confidence"
    ConfidenceLevel.Low -> "Low confidence"
    ConfidenceLevel.MissingData -> "Missing data"
    ConfidenceLevel.UserEntered -> "User-entered"
}
