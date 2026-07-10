package com.clearcart.app.data.model

data class Product(
    val id: String = "",
    val barcode: String,
    val name: String,
    val brand: String,
    val category: String,
    val imageUrl: String?,
    val ingredientsText: String,
    val allergens: List<String>,
    val labels: List<String>,
    val nutrition: Nutrition?,
    val additives: List<String>,
    val novaGroup: Int?,
    val nutriScore: String?,
    val quantity: String? = null,
    val servingSize: String? = null,
    val source: ProductSource,
    val dataSource: ProductSource = source,
    val dataCompletenessScore: Int = 0,
    val lastUpdated: Long?,
    val confidenceLevel: ConfidenceLevel,
    val type: ProductType = ProductType.Food,
    val productType: ProductType = type,
    val userEdited: Boolean = false,
    val rawResponse: String? = null,
)

data class Nutrition(
    val energyKcal100g: Double?,
    val sugar100g: Double?,
    val sodium100g: Double?,
    val saturatedFat100g: Double?,
    val fiber100g: Double?,
    val protein100g: Double?,
)

enum class ProductType { Food, Drink, Cosmetic, Household, Unknown }
enum class ProductSource { OpenFoodFacts, OpenBeautyFacts, Mock, UserEntered, Ocr }
enum class ConfidenceLevel { High, Medium, Low, MissingData, UserEntered }

enum class ProductDataQualityLabel(val label: String) {
    Complete("Complete"),
    Partial("Partial"),
    MissingIngredients("Missing ingredients"),
    MissingNutrition("Missing nutrition"),
    UserAdded("User-added"),
    NeedsReview("Needs review"),
}

data class ProductScore(
    val overallScore: Int,
    val grade: Grade,
    val confidenceLevel: ConfidenceLevel,
    val subscores: List<ScoreSubscore>,
    val explanationList: List<ScoreNote>,
    val positiveList: List<ScoreNote>,
    val cautionList: List<ScoreNote>,
    val scoreLabel: ScoreLabel = ScoreLabel.Okay,
    val confidenceScore: Int = 0,
    val personalFitScore: Int = overallScore,
    val personalFitLabel: ScoreLabel = scoreLabel,
    val explanationSummary: String = "",
    val scoreBreakdown: List<ScoreSubscore> = subscores,
    val topReasons: List<ScoreNote> = emptyList(),
    val preferenceMatches: List<ScoreNote> = emptyList(),
    val preferenceConflicts: List<ScoreNote> = emptyList(),
    val missingDataWarnings: List<ScoreNote> = emptyList(),
)

data class ScoreSubscore(
    val name: String,
    val score: Int,
    val detail: String,
)

data class ScoreNote(
    val text: String,
    val evidence: String,
    val isPreferenceBased: Boolean = false,
)

enum class Grade(val label: String) {
    Excellent("Excellent"),
    Good("Good"),
    Okay("Okay"),
    Caution("Caution"),
    AvoidOften("Avoid Often"),
}

enum class ScoreLabel(val label: String) {
    GreatFit("Great fit"),
    GoodOption("Good option"),
    Okay("Okay"),
    WorthReviewing("Worth reviewing"),
    LimitedData("Limited data"),
}

data class UserPreferences(
    val allergensToAvoid: Set<String> = emptySet(),
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
    val glutenFree: Boolean = false,
    val dairyFree: Boolean = false,
    val lowSugar: Boolean = false,
    val lowSodium: Boolean = false,
    val highProtein: Boolean = false,
    val avoidArtificialColors: Boolean = false,
    val avoidFragrance: Boolean = false,
    val sensitiveSkin: Boolean = false,
    val budgetConscious: Boolean = false,
    val simpleIngredients: Boolean = false,
    val preferOrganic: Boolean = false,
    val scoringWeightOverrides: Map<String, Double> = emptyMap(),
)

data class AlternativeSuggestion(
    val product: Product,
    val score: ProductScore,
    val whyBetter: String,
    val tradeoff: String?,
)
