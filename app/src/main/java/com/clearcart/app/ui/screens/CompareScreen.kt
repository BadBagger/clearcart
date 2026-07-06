package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.domain.scoring.display
import com.clearcart.app.ui.components.SectionCard

@Composable
fun CompareScreen(container: AppContainer, navController: NavController) {
    val products by container.productRepository.observeProductsFromHistory().collectAsState(initial = emptyList())
    val preferences by container.preferencesRepository.state.collectAsState()
    var firstBarcode by remember { mutableStateOf<String?>(null) }
    var secondBarcode by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(products) {
        if (firstBarcode == null || products.none { it.barcode == firstBarcode }) firstBarcode = products.getOrNull(0)?.barcode
        if (secondBarcode == null || products.none { it.barcode == secondBarcode } || secondBarcode == firstBarcode) {
            secondBarcode = products.firstOrNull { it.barcode != firstBarcode }?.barcode
        }
    }

    val first = products.firstOrNull { it.barcode == firstBarcode }
    val second = products.firstOrNull { it.barcode == secondBarcode }
    val pair = listOfNotNull(first, second)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Compare Products", style = MaterialTheme.typography.headlineMedium)
        if (pair.size < 2) {
            Text("Scan at least two products to compare. Comparison avoids universal health claims when data is incomplete.")
            return@Column
        }
        SectionCard {
            Text("Choose products", fontWeight = FontWeight.SemiBold)
            products.forEach { product ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(product.name, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                firstBarcode = product.barcode
                                if (secondBarcode == product.barcode) secondBarcode = products.firstOrNull { it.barcode != product.barcode }?.barcode
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (firstBarcode == product.barcode) "Selected A" else "Set A")
                        }
                        OutlinedButton(
                            onClick = {
                                secondBarcode = product.barcode
                                if (firstBarcode == product.barcode) firstBarcode = products.firstOrNull { it.barcode != product.barcode }?.barcode
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(if (secondBarcode == product.barcode) "Selected B" else "Set B")
                        }
                    }
                }
            }
        }
        val scored = pair.map { it to container.scoringEngine.score(it, preferences) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            scored.forEach { (product, score) ->
                SectionCard(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text("Score ${score.overallScore} • ${score.grade.label}")
                    Text(product.brand.ifBlank { "Brand not listed" })
                    Text("Category: ${product.category.ifBlank { "Not listed" }}")
                    Text("Confidence: ${score.confidenceLevel.display()}")
                    CompareNutrition(product)
                    Text("Additives: ${product.additives.size}")
                    Text("Allergens: ${product.allergens.ifEmpty { listOf("None listed") }.joinToString()}")
                    Text("Price: placeholder")
                }
            }
        }
        val better = scored.maxBy { it.second.overallScore }
        val other = scored.minBy { it.second.overallScore }
        SectionCard {
            Text("Better match for your preferences", fontWeight = FontWeight.SemiBold)
            Text("${better.first.name} has the stronger ClearCart score among these two scanned products. This is based on available data, not a universal healthier claim.")
            val sugarA = better.first.nutrition?.sugar100g
            val sugarB = other.first.nutrition?.sugar100g
            val sodiumA = better.first.nutrition?.sodium100g
            val sodiumB = other.first.nutrition?.sodium100g
            if (sugarA != null && sugarB != null && sodiumA != null && sodiumB != null) {
                val tradeoff = when {
                    sugarA < sugarB && sodiumA > sodiumB -> "Tradeoff: lower sugar but higher sodium."
                    sugarA > sugarB && sodiumA < sodiumB -> "Tradeoff: lower sodium but higher sugar."
                    else -> "No major sugar/sodium tradeoff was found from available data."
                }
                Text(tradeoff)
            } else {
                Text("Some nutrition fields are missing, so the comparison is limited.")
            }
        }
    }
}

@Composable
private fun CompareNutrition(product: Product) {
    val nutrition = product.nutrition
    if (nutrition == null) {
        Text("Nutrition: missing")
        return
    }
    Text("Sugar: ${nutrition.sugar100g?.let { "$it g" } ?: "missing"}")
    Text("Sodium: ${nutrition.sodium100g?.let { "$it g" } ?: "missing"}")
    Text("Sat fat: ${nutrition.saturatedFat100g?.let { "$it g" } ?: "missing"}")
    Text("Fiber: ${nutrition.fiber100g?.let { "$it g" } ?: "missing"}")
    Text("Protein: ${nutrition.protein100g?.let { "$it g" } ?: "missing"}")
}
