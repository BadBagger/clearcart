package com.clearcart.app.data.repository

import com.clearcart.app.data.db.ScanDao
import com.clearcart.app.data.db.ScanEntity
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductDataQuality
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.scoring.ScoringEngine
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProductRepository(
    private val providers: List<ProductDataProvider>,
    private val scanDao: ScanDao,
    private val scoringEngine: ScoringEngine,
    private val gson: Gson = Gson(),
) {
    private val refreshScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeHistory(): Flow<List<ScanEntity>> = scanDao.observeAll()

    suspend fun lookupProduct(barcode: String, preferences: UserPreferences): Result<Product> =
        lookupByBarcode(barcode, preferences)

    suspend fun lookupByBarcode(
        barcode: String,
        preferences: UserPreferences = UserPreferences(),
    ): Result<Product> = runCatching {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) throw ProductNotFoundException(barcode)

        val cached = getCachedProduct(normalizedBarcode)
        if (cached != null) {
            refreshCacheFromApi(normalizedBarcode, preferences)
            return@runCatching cached
        }

        val product = fetchFromProviders(normalizedBarcode)
            ?: MockProducts.byBarcode(normalizedBarcode)
            ?: throw ProductNotFoundException(normalizedBarcode)
        saveToCache(product, preferences)
        ProductDataQuality.normalize(product)
    }

    suspend fun lookupFoodProduct(
        barcode: String,
        preferences: UserPreferences = UserPreferences(),
    ): Result<Product> = runCatching {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) throw ProductNotFoundException(barcode)
        val product = getCachedProduct(normalizedBarcode)
            ?: runCatching { providers.filterIsInstance<OpenFoodFactsProvider>().firstOrNull()?.lookup(normalizedBarcode) }.getOrNull()
            ?: throw ProductNotFoundException(normalizedBarcode)
        saveToCache(product, preferences)
        ProductDataQuality.normalize(product)
    }

    suspend fun lookupBeautyProduct(
        barcode: String,
        preferences: UserPreferences = UserPreferences(),
    ): Result<Product> = runCatching {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) throw ProductNotFoundException(barcode)
        val product = getCachedProduct(normalizedBarcode)
            ?: runCatching { providers.filterIsInstance<OpenBeautyFactsProvider>().firstOrNull()?.lookup(normalizedBarcode) }.getOrNull()
            ?: throw ProductNotFoundException(normalizedBarcode)
        saveToCache(product, preferences)
        ProductDataQuality.normalize(product)
    }

    suspend fun searchProducts(query: String): Result<List<Product>> = runCatching {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@runCatching emptyList()
        val remoteResults = providers.flatMap { provider ->
            runCatching { provider.search(trimmed) }.getOrDefault(emptyList())
        }
        (remoteResults + MockProducts.search(trimmed))
            .map { ProductDataQuality.normalize(it) }
            .distinctBy { it.barcode }
            .take(30)
    }

    suspend fun saveSearchedProduct(product: Product, preferences: UserPreferences) {
        saveToCache(product, preferences)
    }

    suspend fun saveManual(product: Product, preferences: UserPreferences) {
        saveManualProduct(product, preferences)
    }

    suspend fun saveToCache(product: Product, preferences: UserPreferences = UserPreferences()) {
        saveScan(ProductDataQuality.normalize(product), preferences)
    }

    suspend fun saveManualProduct(product: Product, preferences: UserPreferences = UserPreferences()) {
        val localSource = if (product.dataSource == ProductSource.Ocr || product.source == ProductSource.Ocr) {
            ProductSource.Ocr
        } else {
            ProductSource.UserEntered
        }
        saveScan(
            ProductDataQuality.normalize(
                product.copy(
                    source = localSource,
                    dataSource = localSource,
                    userEdited = true,
                )
            ),
            preferences,
        )
    }

    suspend fun updateUserCorrection(product: Product, preferences: UserPreferences = UserPreferences()) {
        saveScan(
            ProductDataQuality.normalize(
                product.copy(userEdited = true)
            ),
            preferences,
        )
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
            runCatching { gson.fromJson(snapshot, Product::class.java) }
                .getOrNull()
                ?.let { return ProductDataQuality.normalize(it) }
        }
        return ProductDataQuality.normalize(Product(
            id = entity.barcode,
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
            dataSource = ProductSource.Mock,
            lastUpdated = null,
            confidenceLevel = ConfidenceLevel.valueOf(entity.dataConfidence),
            type = runCatching { com.clearcart.app.data.model.ProductType.valueOf(entity.productType) }.getOrDefault(com.clearcart.app.data.model.ProductType.Unknown),
            productType = runCatching { com.clearcart.app.data.model.ProductType.valueOf(entity.productType) }.getOrDefault(com.clearcart.app.data.model.ProductType.Unknown),
            rawResponse = entity.rawApiResponse,
        ))
    }

    suspend fun getProductSnapshot(barcode: String): Product? {
        return getCachedProduct(barcode)
    }

    suspend fun getCachedProduct(barcode: String): Product? {
        val normalizedBarcode = barcode.trim()
        if (normalizedBarcode.isBlank()) return null
        val entity = scanDao.get(normalizedBarcode) ?: return null
        return productFromEntity(entity)
    }

    fun observeFavoriteProducts(): Flow<List<Product>> = scanDao.observeAll().map { rows ->
        rows.filter { it.favorite }.map { productFromEntity(it) }
    }

    private suspend fun saveScan(product: Product, preferences: UserPreferences) {
        val normalizedProduct = ProductDataQuality.normalize(product)
        val score = scoringEngine.score(normalizedProduct, preferences)
        val existing = scanDao.get(normalizedProduct.barcode)
        scanDao.upsert(
            ScanEntity(
                barcode = normalizedProduct.barcode,
                productName = normalizedProduct.name,
                brand = normalizedProduct.brand,
                score = score.overallScore,
                dateScanned = System.currentTimeMillis(),
                productType = normalizedProduct.productType.name,
                favorite = existing?.favorite ?: false,
                rawApiResponse = normalizedProduct.rawResponse,
                dataConfidence = normalizedProduct.confidenceLevel.name,
                category = normalizedProduct.category,
                imageUrl = normalizedProduct.imageUrl,
                ingredientsText = normalizedProduct.ingredientsText,
                productJson = gson.toJson(normalizedProduct),
            )
        )
    }

    private fun refreshCacheFromApi(barcode: String, preferences: UserPreferences) {
        refreshScope.launch {
            val remote = fetchFromProviders(barcode) ?: return@launch
            val cached = getCachedProduct(barcode)
            if (cached == null || remote.dataCompletenessScore >= cached.dataCompletenessScore) {
                saveToCache(remote, preferences)
            }
        }
    }

    private suspend fun fetchFromProviders(barcode: String): Product? =
        providers.firstNotNullOfOrNull { provider ->
            runCatching { provider.lookup(barcode) }.getOrNull()?.let { ProductDataQuality.normalize(it) }
        }
}

class ProductNotFoundException(barcode: String) : Exception("No product found for barcode $barcode")
