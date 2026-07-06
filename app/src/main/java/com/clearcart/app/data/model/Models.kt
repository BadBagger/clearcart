package com.clearcart.app.data.model

data class Product(
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
    val source: ProductSource,
    val lastUpdated: Long?,
    val confidenceLevel: ConfidenceLevel,
    val type: ProductType = ProductType.Food,
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

enum class ProductType { Food, Cosmetic, Household }
enum class ProductSource { OpenFoodFacts, OpenBeautyFacts, Mock, UserEntered, Ocr }
enum class ConfidenceLevel { High, Medium, Low, MissingData, UserEntered }

data class ProductScore(
    val overallScore: Int,
    val grade: Grade,
    val confidenceLevel: ConfidenceLevel,
    val subscores: List<ScoreSubscore>,
    val explanationList: List<ScoreNote>,
    val positiveList: List<ScoreNote>,
    val cautionList: List<ScoreNote>,
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
