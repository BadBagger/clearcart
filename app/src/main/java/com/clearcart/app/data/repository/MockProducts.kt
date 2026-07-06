package com.clearcart.app.data.repository

import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource

object MockProducts {
    private val products = listOf(
        Product(
            barcode = "0123456789012",
            name = "Bright Oat Crunch",
            brand = "Sample Pantry",
            category = "cereal",
            imageUrl = null,
            ingredientsText = "Whole grain oats, cane sugar, sunflower oil, sea salt, natural flavor.",
            allergens = listOf("gluten"),
            labels = listOf("vegetarian"),
            nutrition = Nutrition(390.0, 16.0, 0.42, 1.2, 8.0, 9.0),
            additives = emptyList(),
            novaGroup = 3,
            nutriScore = "b",
            source = ProductSource.Mock,
            lastUpdated = System.currentTimeMillis() / 1000,
            confidenceLevel = ConfidenceLevel.High,
        ),
        Product(
            barcode = "0099999999999",
            name = "Daily Fresh Body Wash",
            brand = "Sample Care",
            category = "personal care",
            imageUrl = null,
            ingredientsText = "Water, sodium laureth sulfate, cocamidopropyl betaine, fragrance, glycerin.",
            allergens = listOf("fragrance"),
            labels = emptyList(),
            nutrition = null,
            additives = emptyList(),
            novaGroup = null,
            nutriScore = null,
            source = ProductSource.Mock,
            lastUpdated = null,
            confidenceLevel = ConfidenceLevel.Medium,
            type = com.clearcart.app.data.model.ProductType.Cosmetic,
        ),
    )

    fun byBarcode(barcode: String): Product? = products.firstOrNull { it.barcode == barcode }
    fun alternatives(category: String): List<Product> = products.filter { it.category == category }
    fun search(query: String): List<Product> {
        val normalized = query.trim()
        if (normalized.isBlank()) return emptyList()
        return products.filter {
            it.name.contains(normalized, ignoreCase = true) ||
                it.brand.contains(normalized, ignoreCase = true) ||
                it.category.contains(normalized, ignoreCase = true) ||
                it.ingredientsText.contains(normalized, ignoreCase = true)
        }
    }
    fun all(): List<Product> = products
}
