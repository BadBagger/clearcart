package com.clearcart.app.data.repository

import com.clearcart.app.data.db.ScanDao
import com.clearcart.app.data.db.ScanEntity
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.domain.scoring.ScoringEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ProductRepositoryTest {
    @Test
    fun lookupFallsBackToSavedLocalSnapshotWhenProvidersMiss() = runBlocking {
        val dao = FakeScanDao()
        val repository = ProductRepository(
            providers = listOf(AlwaysMissingProvider),
            scanDao = dao,
            scoringEngine = ScoringEngine(),
        )
        val product = Product(
            barcode = "manual-local-1",
            name = "Local Label Product",
            brand = "Kitchen Test",
            category = "snack",
            imageUrl = null,
            ingredientsText = "Oats, apple, cinnamon.",
            allergens = emptyList(),
            labels = emptyList(),
            nutrition = Nutrition(
                energyKcal100g = null,
                sugar100g = 6.0,
                sodium100g = 0.1,
                saturatedFat100g = null,
                fiber100g = null,
                protein100g = null,
            ),
            additives = emptyList(),
            novaGroup = null,
            nutriScore = null,
            source = ProductSource.UserEntered,
            lastUpdated = null,
            confidenceLevel = ConfidenceLevel.UserEntered,
        )

        repository.saveManual(product, UserPreferences())
        val result = repository.lookupProduct("manual-local-1", UserPreferences())

        assertEquals(product.name, result.getOrThrow().name)
        assertEquals(ProductSource.UserEntered, result.getOrThrow().source)
    }
}

private object AlwaysMissingProvider : ProductDataProvider {
    override val name = "Missing"
    override suspend fun lookup(barcode: String): Product? = null
    override suspend fun search(query: String): List<Product> = emptyList()
}

private class FakeScanDao : ScanDao {
    private val rows = LinkedHashMap<String, ScanEntity>()
    private val flow = MutableStateFlow<List<ScanEntity>>(emptyList())

    override fun observeAll(): Flow<List<ScanEntity>> = flow

    override suspend fun get(barcode: String): ScanEntity? = rows[barcode]

    override suspend fun upsert(scan: ScanEntity) {
        rows[scan.barcode] = scan
        publish()
    }

    override suspend fun update(scan: ScanEntity) {
        rows[scan.barcode] = scan
        publish()
    }

    override suspend fun delete(scan: ScanEntity) {
        rows.remove(scan.barcode)
        publish()
    }

    override suspend fun clear() {
        rows.clear()
        publish()
    }

    override suspend fun scanCount(): Int = rows.size

    override suspend fun favoriteCount(): Int = rows.values.count { it.favorite }

    override suspend fun averageScore(): Double? = rows.values.map { it.score }.takeIf { it.isNotEmpty() }?.average()

    override suspend fun latestProductNames(): List<String> =
        rows.values.sortedByDescending { it.dateScanned }.take(3).map { it.productName }

    override suspend fun lowerConfidenceCount(): Int =
        rows.values.count { it.dataConfidence != ConfidenceLevel.High.name }

    private fun publish() {
        flow.value = rows.values.sortedByDescending { it.dateScanned }
    }
}
