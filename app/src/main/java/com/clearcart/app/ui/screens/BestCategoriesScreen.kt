package com.clearcart.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.domain.categories.CategoryBestEngine
import com.clearcart.app.ui.components.ConfidenceBadge
import com.clearcart.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun BestCategoriesScreen(container: AppContainer, navController: NavController) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val products by container.productRepository.observeCategoryCandidates().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val engine = remember { CategoryBestEngine(container.scoringEngine) }
    val picks = remember(products, preferences) {
        engine.bestByCategory(products, preferences)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Best by Category", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Top ClearCart-rated options from products you have scanned, searched, or sample data. This is a practical ranking, not medical advice.")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { navController.navigate("search") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Search, null)
                Text("Find More")
            }
            Button(onClick = { navController.navigate("scanner") }, modifier = Modifier.weight(1f)) {
                Text("Scan")
            }
        }

        if (picks.isEmpty()) {
            SectionCard {
                Text("No category picks yet", fontWeight = FontWeight.SemiBold)
                Text("Search or scan more products to build stronger category rankings.")
            }
        }

        picks.forEach { pick ->
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    CategoryProductThumbnail(pick.product)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = MaterialTheme.colorScheme.primary)
                            Text(pick.categoryName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(pick.product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(listOf(pick.product.brand, pick.product.category).filter { it.isNotBlank() }.joinToString(" / "))
                        Text("ClearCart ${pick.score.overallScore} / Personal Fit ${pick.score.personalFitScore}")
                        ConfidenceBadge(pick.score.confidenceLevel)
                        Text(pick.whyPicked)
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
private fun CategoryProductThumbnail(product: Product) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (product.imageUrl.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
        } else {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
