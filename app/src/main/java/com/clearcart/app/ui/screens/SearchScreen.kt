package com.clearcart.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.ConfidenceBadge
import com.clearcart.app.ui.components.SectionCard
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(container: AppContainer, navController: NavController) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.length < 2 || loading) return
        loading = true
        searched = true
        error = null
        scope.launch {
            container.productRepository.searchProducts(trimmed)
                .onSuccess { results = it }
                .onFailure {
                    results = emptyList()
                    error = "Search is unavailable right now. Try scanning or entering a barcode."
                }
            loading = false
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Search Products", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Find products by name, brand, category, or ingredient. Barcode scanning is still available in the Scan tab.")
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                results = emptyList()
                searched = false
                error = null
            },
            label = { Text("Product, brand, or ingredient") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { runSearch() }, modifier = Modifier.weight(1f), enabled = query.trim().length >= 2 && !loading) {
                Text("Search")
            }
            OutlinedButton(onClick = { navController.navigate("scanner") }, modifier = Modifier.weight(1f)) {
                Text("Scan")
            }
        }
        if (loading) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Searching product databases...")
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && searched && results.isEmpty() && error == null) {
            SectionCard {
                Text("No products found", fontWeight = FontWeight.SemiBold)
                Text("Try a shorter product name, brand, or ingredient. If it is missing, you can add it manually.")
                OutlinedButton(onClick = { navController.navigate("manual") }) { Text("Add Manually") }
            }
        }
        results.forEach { product ->
            val score = container.scoringEngine.score(product, preferences)
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    ProductSearchThumbnail(product)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                product.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(product.source.displayName(), style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            listOf(product.brand, product.category)
                                .filter { it.isNotBlank() }
                                .joinToString(" / ")
                                .ifBlank { "Details limited" },
                        )
                        Text("Score ${score.overallScore} / ${score.scoreLabel.label}")
                        ConfidenceBadge(score.confidenceLevel)
                    }
                }
                Button(
                    onClick = {
                        scope.launch {
                            container.productRepository.saveSearchedProduct(product, preferences)
                            navController.navigate("product/${product.barcode}")
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
private fun ProductSearchThumbnail(product: Product) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(shape)
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

private fun ProductSource.displayName(): String = when (this) {
    ProductSource.OpenFoodFacts -> "Open Food Facts"
    ProductSource.OpenBeautyFacts -> "Open Beauty Facts"
    ProductSource.Mock -> "Mock data"
    ProductSource.UserEntered -> "User-entered"
    ProductSource.Ocr -> "Label scan"
}
