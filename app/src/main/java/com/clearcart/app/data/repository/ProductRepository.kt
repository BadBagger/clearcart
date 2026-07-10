package com.clearcart.app.data.repository

import com.clearcart.app.data.db.ScanDao
import com.clearcart.app.data.db.ScanEntity
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.scoring.ScoringEngine
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProductRepository(
    private val providers: List<ProductDataProvider>,
    private val scanDao: ScanDao,
    private val scoringEngine: ScoringEngine,
    private val gson: Gson = Gson(),
) {
    fun observeHistory(): Flow<List<ScanEntity>> = scanDao.observeAll()

    suspend fun lookupProduct(barcode: String, preferences: UserPreferences): Result<Product> = runCatching {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) throw ProductNotFoundException(barcode)
        val product = providers.firstNotNullOfOrNull { provider ->
            runCatching { provider.lookup(normalizedBarcode) }.getOrNull()
        } ?: getProductSnapshot(normalizedBarcode)
            ?: MockProducts.byBarcode(normalizedBarcode)
            ?: throw ProductNotFoundException(normalizedBarcode)
        saveScan(product, preferences)
        product
    }

    suspend fun searchProducts(query: String): Result<List<Product>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@runCatching emptyList()
        val remoteResults = providers.flatMap { provider ->
            runCatching { provider.search(trimmed) }.getOrDefault(emptyList())
        }
        (remoteResults + MockProducts.search(trimmed))
            .distinctBy { it.barcode }
            .take(30)
    }

    suspend fun saveSearchedProduct(product: Product, preferences: UserPreferences) {
        saveScan(product, preferences)
    }

    suspend fun saveManual(product: Product, preferences: UserPreferences) {
        saveScan(product, preferences)
    }

    suspend fun setFavorite(barcode: String, favorite: Boolean) {
        val entity = scanDao.get(barcode) ?: return
        scanDao.update(entity.copy(favorite = favorite))
    }

    suspend fun delete(barcode: String) {
        scanDao.get(barcode)?.let { scanDao.delete(it) }
    }

    suspend fun clearHistory() = scanDao.clear()

    fun observeProductsFromHistory(): Flow<List<Product>> = scanDao.observeAll().map { rows ->
        rows.map {
            productFromEntity(it)
        }
    }

    private fun productFromEntity(entity: ScanEntity): Product {
        entity.productJson?.let { snapshot ->
            runCatching { gson.fromJson(snapshot, Product::class.java) }.getOrNull()?.let { return it }
        }
        return Product(
            barcode = entity.barcode,
            name = entity.productName,
            brand = entity.brand,
            category = entity.category,
            imageUrl = entity.imageUrl,
            ingredientsText = entity.ingredientsText,
            allergens = emptyList(),
            labels = emptyList(),
            nutrition = null,
            additives = emptyList(),
            novaGroup = null,
            nutriScore = null,
            source = ProductSource.Mock,
            lastUpdated = null,
            confidenceLevel = ConfidenceLevel.valueOf(entity.dataConfidence),
            rawResponse = entity.rawApiResponse,
        )
    }

    suspend fun getProductSnapshot(barcode: String): Product? {
        val entity = scanDao.get(barcode) ?: return null
        return productFromEntity(entity)
    }

    fun observeFavoriteProducts(): Flow<List<Product>> = scanDao.observeAll().map { rows ->
        rows.filter { it.favorite }.map {
            Product(
                barcode = it.barcode,
                name = it.productName,
                brand = it.brand,
                category = it.category,
                imageUrl = it.imageUrl,
                ingredientsText = it.ingredientsText,
                allergens = emptyList(),
                labels = emptyList(),
                nutrition = null,
                additives = emptyList(),
                novaGroup = null,
                nutriScore = null,
                source = ProductSource.Mock,
                lastUpdated = null,
                confidenceLevel = ConfidenceLevel.valueOf(it.dataConfidence),
                rawResponse = it.rawApiResponse,
            )
        }
    }

    private suspend fun saveScan(product: Product, preferences: UserPreferences) {
        val score = scoringEngine.score(product, preferences)
        val existing = scanDao.get(product.barcode)
        scanDao.upsert(
            ScanEntity(
                barcode = product.barcode,
                productName = product.name,
                brand = product.brand,
                score = score.overallScore,
                dateScanned = System.currentTimeMillis(),
                productType = product.type.name,
                favorite = existing?.favorite ?: false,
                rawApiResponse = product.rawResponse,
                dataConfidence = product.confidenceLevel.name,
                category = product.category,
                imageUrl = product.imageUrl,
                ingredientsText = product.ingredientsText,
                productJson = gson.toJson(product),
            )
        )
    }
}

class ProductNotFoundException(barcode: String) : Exception("No product found for barcode $barcode")
