package com.clearcart.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.domain.categories.CategoryBestEngine
import com.clearcart.app.domain.categories.defaultCategories
import com.clearcart.app.ui.components.CategoryThumbnail
import com.clearcart.app.ui.components.ConfidenceBadge
import com.clearcart.app.ui.components.ProductThumbnail
import com.clearcart.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun BestCategoriesScreen(container: AppContainer, navController: NavController) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val products by container.productRepository.observeCategoryCandidates().collectAsState(initial = emptyList())
    val engine = remember { CategoryBestEngine(container.scoringEngine) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Best by Category", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Choose a category to see the strongest ClearCart-ranked options.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { navController.navigate("search") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Search, null)
                Text("Find More")
            }
            Button(onClick = { navController.navigate("scanner") }, modifier = Modifier.weight(1f)) {
                Text("Scan")
            }
        }

        defaultCategories.forEach { category ->
            val rankedCount = remember(products, preferences, category) {
                engine.rankedProductsForCategory(category, products, preferences).size
            }
            val hiddenCount = remember(products, preferences, category) {
                engine.hiddenByPreferenceCount(category, products, preferences)
            }
            CategoryRow(
                category = category,
                count = rankedCount,
                hiddenCount = hiddenCount,
                hasAnyMatch = engine.hasCategoryMatch(category, products),
                onOpen = {
                    navController.navigate("best-category/${Uri.encode(category)}")
                },
            )
        }
    }
}

@Composable
fun BestCategoryProductsScreen(
    container: AppContainer,
    navController: NavController,
    category: String,
) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val products by container.productRepository.observeCategoryCandidates().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val engine = remember { CategoryBestEngine(container.scoringEngine) }
    val ranked = remember(products, preferences, category) {
        engine.rankedProductsForCategory(category, products, preferences)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(category, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Ranked with your preferences and available product data.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) {
                Text("Categories")
            }
            Button(onClick = { navController.navigate("search") }, modifier = Modifier.weight(1f)) {
                Text("Search")
            }
        }

        if (ranked.isEmpty()) {
            SectionCard {
                Text("No strong picks yet", fontWeight = FontWeight.SemiBold)
                Text("Scan or search more products in this category to improve the ranking.")
            }
        }

        ranked.forEachIndexed { index, pick ->
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProductThumbnail(pick.product)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("#${index + 1}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(pick.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(listOf(pick.product.brand, pick.product.category).filter { it.isNotBlank() }.joinToString(" / "))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ScoreChip("ClearCart", pick.score.overallScore)
                            ScoreChip("Fit", pick.score.personalFitScore)
                        }
                        ConfidenceBadge(pick.score.confidenceLevel)
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            container.productRepository.saveSearchedProduct(pick.product, preferences)
                            navController.navigate("product/${pick.product.barcode}")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Result")
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: String,
    count: Int,
    hiddenCount: Int,
    hasAnyMatch: Boolean,
    onOpen: () -> Unit,
) {
    SectionCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryThumbnail(category)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        count > 0 && hiddenCount > 0 -> "$count ranked, $hiddenCount hidden by filters"
                        count > 0 -> "$count ranked option${if (count == 1) "" else "s"}"
                        hiddenCount > 0 -> "Hidden by allergen filters"
                        hasAnyMatch -> "More data needed"
                        else -> "No products yet"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            OutlinedButton(onClick = onOpen) {
                Icon(Icons.Default.ChevronRight, null)
            }
        }
    }
}

@Composable
private fun ScoreChip(label: String, score: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "$label $score",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
