package com.clearcart.app.ocr

data class OcrExtraction(
    val brandOrName: String = "",
    val ingredients: String = "",
    val nutritionFacts: String = "",
    val expirationDate: String = "",
)

object OcrTextAnalyzer {
    fun parse(raw: String): OcrExtraction {
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val ingredients = lines.firstOrNull { it.contains("ingredients", true) }.orEmpty()
        val nutrition = lines.filter { it.contains("sugar", true) || it.contains("sodium", true) || it.contains("protein", true) }.joinToString("\n")
        val expiration = lines.firstOrNull { it.contains("exp", true) || it.contains("best by", true) }.orEmpty()
        return OcrExtraction(
            brandOrName = lines.firstOrNull().orEmpty(),
            ingredients = ingredients,
            nutritionFacts = nutrition,
            expirationDate = expiration,
        )
    }
}
