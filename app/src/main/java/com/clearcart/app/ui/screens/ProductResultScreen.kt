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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.clearcart.app.data.model.AlternativeSuggestion
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductDataQuality
import com.clearcart.app.data.model.ProductDataQualityLabel
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.data.repository.MockProducts
import com.clearcart.app.domain.ingredients.IngredientInsight
import com.clearcart.app.domain.scoring.display
import com.clearcart.app.ui.components.ScoreHeader
import com.clearcart.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun ProductResultScreen(container: AppContainer, navController: NavController, barcode: String) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val cachedProducts by container.productRepository.observeProductsFromHistory().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<Product?>(null) }
    var score by remember { mutableStateOf<ProductScore?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hiddenAlternatives by remember(barcode) { mutableStateOf(setOf<String>()) }

    LaunchedEffect(barcode, preferences) {
        product = null
        score = null
        error = null
        container.productRepository.lookupProduct(barcode, preferences)
            .onSuccess {
                product = it
                score = container.scoringEngine.score(it, preferences)
            }
            .onFailure { error = it.message }
    }

    val p = product
    val s = score
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (p == null || s == null) {
            Text(error ?: "Loading product...")
            if (error != null) {
                Button(onClick = { navController.navigate("manual?barcode=${Uri.encode(barcode)}") }) {
                    Text("Add Product Manually")
                }
            }
            return@Column
        }
        Text(p.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(listOf(p.brand, p.category).filter { it.isNotBlank() }.joinToString(" / "))
        p.imageUrl?.let { AsyncImage(model = it, contentDescription = p.name, modifier = Modifier.fillMaxWidth()) }
        SectionCard { ScoreHeader(s) }
        SectionCard {
            Text("Score summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(s.explanationSummary)
        }
        SectionCard {
            val qualityLabel = ProductDataQuality.qualityLabel(p)
            Text("Data quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${qualityLabel.label} / ${p.dataCompletenessScore}% complete")
            if (qualityLabel != ProductDataQualityLabel.Complete) {
                Text("Some product details are missing. ClearCart can still help, but this score may be less confident.")
                Text(ProductDataQuality.missingDataCopy(p), style = MaterialTheme.typography.bodySmall)
            }
            Text("Source: ${p.dataSource.displayName()}", style = MaterialTheme.typography.bodySmall)
        }
        SectionCard {
            Text("Top reasons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            s.topReasons.ifEmpty { s.explanationList }.forEach {
                Text("- ${it.text}\n${it.evidence}${if (it.isPreferenceBased) " This reflects your preferences." else ""}")
            }
        }
        if (s.preferenceMatches.isNotEmpty() || s.preferenceConflicts.isNotEmpty()) {
            SectionCard {
                Text("Personal fit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                s.preferenceMatches.forEach {
                    Text("Match: ${it.text}", fontWeight = FontWeight.Medium)
                    Text(it.evidence, style = MaterialTheme.typography.bodySmall)
                }
                s.preferenceConflicts.forEach {
                    Text("Worth checking: ${it.text}", fontWeight = FontWeight.Medium)
                    Text(it.evidence, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SectionCard {
            Text("Why this score?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            (s.positiveList + s.cautionList + s.explanationList).forEach {
                Text("- ${it.text}\n${it.evidence}${if (it.isPreferenceBased) " This reflects your preferences." else ""}")
            }
        }
        SectionCard {
            Text("Ingredients", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (p.ingredientsText.isBlank()) {
                Text("Ingredient data is incomplete.")
            } else {
                Text(p.ingredientsText)
            }
            IngredientExplorer(
                ingredients = remember(p, preferences) {
                    container.ingredientInsightEngine.explainIngredients(p, preferences)
                },
                onAddAvoid = { ingredient ->
                    container.preferencesRepository.update { current ->
                        current.copy(
                            ingredientAvoidList = current.ingredientAvoidList + ingredient,
                            ingredientOkayList = current.ingredientOkayList - ingredient,
                        )
                    }
                },
                onAddOkay = { ingredient ->
                    container.preferencesRepository.update { current ->
                        current.copy(
                            ingredientOkayList = current.ingredientOkayList + ingredient,
                            ingredientAvoidList = current.ingredientAvoidList - ingredient,
                        )
                    }
                },
                onRemoveChoice = { ingredient ->
                    container.preferencesRepository.update { current ->
                        current.copy(
                            ingredientAvoidList = current.ingredientAvoidList - ingredient,
                            ingredientOkayList = current.ingredientOkayList - ingredient,
                        )
                    }
                },
            )
            val insights = remember(p, preferences) {
                container.ingredientInsightEngine.explain(p, preferences)
            }
            if (insights.isNotEmpty()) {
                Text("Ingredient notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                insights.forEach {
                    Text(it.title, fontWeight = FontWeight.Medium)
                    Text(it.detail, style = MaterialTheme.typography.bodySmall)
                    Text("Found: ${it.evidence}${if (it.isPreferenceBased) " Based on your preferences." else ""}")
                }
            }
        }
        SectionCard {
            Text("Score breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            s.scoreBreakdown.forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(it.name, fontWeight = FontWeight.Medium)
                        Text(it.detail, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${it.score}")
                }
            }
        }
        SectionCard {
            Text("Alternatives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val alternatives = remember(p, preferences, cachedProducts, hiddenAlternatives) {
                container.alternativeEngine.suggestFor(
                    product = p,
                    preferences = preferences,
                    candidates = cachedProducts + MockProducts.all(),
                    hiddenBarcodes = hiddenAlternatives,
                )
            }
            if (alternatives.isEmpty()) {
                Text("No strong alternatives found yet. Scan more products to improve suggestions.")
                Button(onClick = { navController.navigate("scanner") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan another product")
                }
            } else {
                alternatives.forEach {
                    AlternativeCard(
                        suggestion = it,
                        onSave = {
                            scope.launch {
                                container.productRepository.saveSearchedProduct(it.product, preferences)
                                container.productRepository.setFavorite(it.product.barcode, true)
                            }
                        },
                        onCompare = { navController.navigate("compare") },
                        onScanAnother = { navController.navigate("scanner") },
                        onHide = { hiddenAlternatives = hiddenAlternatives + it.product.barcode },
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = { scope.launch { container.productRepository.setFavorite(p.barcode, true) } },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.FavoriteBorder, null)
                Text("Favorite")
            }
            Button(onClick = { navController.navigate("compare") }, modifier = Modifier.weight(1f)) {
                Text("Compare")
            }
        }
        Text("For medical dietary needs, check with a professional.")
    }
}

@Composable
private fun AlternativeCard(
    suggestion: AlternativeSuggestion,
    onSave: () -> Unit,
    onCompare: () -> Unit,
    onScanAnother: () -> Unit,
    onHide: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(suggestion.product.name, fontWeight = FontWeight.SemiBold)
        Text(suggestion.product.brand.ifBlank { "Unknown brand" }, style = MaterialTheme.typography.bodySmall)
        Text("Score ${suggestion.score.overallScore} / Personal Fit ${suggestion.score.personalFitScore}")
        Text("Data confidence: ${suggestion.dataConfidence.display()}", style = MaterialTheme.typography.bodySmall)
        Text("Why suggested", fontWeight = FontWeight.Medium)
        Text(suggestion.whyBetter)
        Text("What is better", fontWeight = FontWeight.Medium)
        Text(suggestion.whatIsBetter)
        suggestion.concern?.let {
            Text("Worth checking", fontWeight = FontWeight.Medium)
            Text(it)
        }
        suggestion.tradeoff?.let {
            Text("Tradeoff", fontWeight = FontWeight.Medium)
            Text(it)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onSave, modifier = Modifier.weight(1f)) {
                Text("Save")
            }
            OutlinedButton(onClick = onCompare, modifier = Modifier.weight(1f)) {
                Text("Compare")
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onScanAnother, modifier = Modifier.weight(1f)) {
                Text("Scan another")
            }
            OutlinedButton(onClick = onHide, modifier = Modifier.weight(1f)) {
                Text("Hide")
            }
        }
    }
}

@Composable
private fun IngredientExplorer(
    ingredients: List<IngredientInsight>,
    onAddAvoid: (String) -> Unit,
    onAddOkay: (String) -> Unit,
    onRemoveChoice: (String) -> Unit,
) {
    var hideQuietIngredients by remember { mutableStateOf(false) }
    var selectedName by remember(ingredients) { mutableStateOf(ingredients.firstOrNull { it.isHighlighted }?.ingredient) }
    val visibleIngredients = if (hideQuietIngredients) ingredients.filter { it.isHighlighted } else ingredients
    val selected = ingredients.firstOrNull { it.ingredient == selectedName }

    if (ingredients.isEmpty()) {
        Text("No readable ingredients were available to explain.", style = MaterialTheme.typography.bodySmall)
        return
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Ingredient list", fontWeight = FontWeight.Medium)
        Row {
            Text("Hide quiet items", style = MaterialTheme.typography.bodySmall)
            Checkbox(checked = hideQuietIngredients, onCheckedChange = { hideQuietIngredients = it })
        }
    }
    visibleIngredients.forEach { ingredient ->
        OutlinedButton(
            onClick = { selectedName = ingredient.ingredient },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(ingredient.ingredient, fontWeight = FontWeight.Medium)
                Text(ingredient.tags.joinToString { it.label }, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
    selected?.let {
        Text("Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(it.ingredient, fontWeight = FontWeight.Medium)
        Text(it.detail)
        Text(it.evidence, style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onAddAvoid(it.ingredient) }, modifier = Modifier.weight(1f)) {
                Text("Avoid")
            }
            OutlinedButton(onClick = { onAddOkay(it.ingredient) }, modifier = Modifier.weight(1f)) {
                Text("Okay")
            }
            Button(onClick = { onRemoveChoice(it.ingredient) }, modifier = Modifier.weight(1f)) {
                Text("Clear")
            }
        }
    }
}

private fun com.clearcart.app.data.model.ProductSource.displayName(): String = when (this) {
    com.clearcart.app.data.model.ProductSource.OpenFoodFacts -> "Open Food Facts"
    com.clearcart.app.data.model.ProductSource.OpenBeautyFacts -> "Open Beauty Facts"
    com.clearcart.app.data.model.ProductSource.Mock -> "Mock data"
    com.clearcart.app.data.model.ProductSource.UserEntered -> "User-added"
    com.clearcart.app.data.model.ProductSource.Ocr -> "OCR label scan"
}
