package com.clearcart.app.data.model

object ProductDataQuality {
    fun normalize(product: Product): Product {
        val productType: ProductType? = product.productType
        val legacyType: ProductType? = product.type
        val normalizedType = productType ?: legacyType ?: ProductType.Unknown
        val explicitSource: ProductSource? = product.dataSource
        val legacySource: ProductSource? = product.source
        val source = explicitSource ?: legacySource ?: ProductSource.UserEntered
        val normalized = product.copy(
            id = product.id.ifBlank { product.barcode.ifBlank { "local-${stableLocalId(product)}" } },
            name = product.name.ifBlank { "Unnamed product" },
            source = source,
            dataSource = source,
            type = normalizedType,
            productType = normalizedType,
            dataCompletenessScore = completenessScore(product),
            confidenceLevel = confidenceFor(product),
        )
        return normalized.copy(dataCompletenessScore = completenessScore(normalized))
    }

    fun qualityLabel(product: Product): ProductDataQualityLabel {
        val normalized = normalize(product)
        if (normalized.dataSource == ProductSource.Ocr) {
            return ProductDataQualityLabel.NeedsReview
        }
        if (normalized.userEdited || normalized.dataSource == ProductSource.UserEntered) {
            return ProductDataQualityLabel.UserAdded
        }
        val hasIngredients = normalized.ingredientsText.isNotBlank()
        val needsNutrition = normalized.productType == ProductType.Food || normalized.productType == ProductType.Drink
        val hasNutrition = normalized.nutrition != null
        return when {
            !hasIngredients -> ProductDataQualityLabel.MissingIngredients
            needsNutrition && !hasNutrition -> ProductDataQualityLabel.MissingNutrition
            normalized.dataCompletenessScore >= 85 -> ProductDataQualityLabel.Complete
            normalized.dataCompletenessScore >= 55 -> ProductDataQualityLabel.Partial
            else -> ProductDataQualityLabel.NeedsReview
        }
    }

    fun missingDataCopy(product: Product): String {
        val normalized = normalize(product)
        val missing = buildList {
            if (normalized.ingredientsText.isBlank()) add("ingredients")
            if ((normalized.productType == ProductType.Food || normalized.productType == ProductType.Drink) && normalized.nutrition == null) add("nutrition")
            if (normalized.category.isBlank()) add("category")
            if (normalized.quantity.isNullOrBlank()) add("quantity")
            if (normalized.servingSize.isNullOrBlank()) add("serving size")
        }
        return if (missing.isEmpty()) {
            "Product details look complete enough for a higher-confidence review."
        } else {
            "Missing: ${missing.joinToString()}."
        }
    }

    private fun completenessScore(product: Product): Int {
        var points = 0
        if (product.name.isNotBlank()) points += 12
        if (product.brand.isNotBlank()) points += 8
        if (product.category.isNotBlank()) points += 10
        if (product.imageUrl != null) points += 5
        if (product.ingredientsText.isNotBlank()) points += 22
        if (product.nutrition != null) points += 18
        if (product.allergens.isNotEmpty()) points += 5
        if (product.labels.isNotEmpty()) points += 4
        if (product.additives.isNotEmpty()) points += 4
        if (!product.quantity.isNullOrBlank()) points += 5
        if (!product.servingSize.isNullOrBlank()) points += 4
        if (product.lastUpdated != null) points += 3
        return points.coerceIn(0, 100)
    }

    private fun confidenceFor(product: Product): ConfidenceLevel {
        if (product.dataSource == ProductSource.Ocr) return ConfidenceLevel.Low
        if (product.userEdited || product.dataSource == ProductSource.UserEntered) return ConfidenceLevel.UserEntered
        val score = completenessScore(product)
        return when {
            score >= 80 -> ConfidenceLevel.High
            score >= 55 -> ConfidenceLevel.Medium
            score > 0 -> ConfidenceLevel.Low
            else -> ConfidenceLevel.MissingData
        }
    }

    private fun stableLocalId(product: Product): String =
        "${product.name}|${product.brand}|${product.ingredientsText}".hashCode().toUInt().toString()
}
