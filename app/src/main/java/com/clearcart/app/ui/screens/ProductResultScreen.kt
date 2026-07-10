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
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductScore
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.ScoreHeader
import com.clearcart.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun ProductResultScreen(container: AppContainer, navController: NavController, barcode: String) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var product by remember { mutableStateOf<Product?>(null) }
    var score by remember { mutableStateOf<ProductScore?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

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
            Text("Why this score?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            (s.positiveList + s.cautionList + s.explanationList).forEach {
                Text("- ${it.text}\n${it.evidence}${if (it.isPreferenceBased) " This reflects your preferences." else ""}")
            }
        }
        SectionCard {
            Text("Ingredient notes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val insights = remember(p, preferences) {
                container.ingredientInsightEngine.explain(p, preferences)
            }
            insights.forEach {
                Text(it.title, fontWeight = FontWeight.Medium)
                Text(it.detail, style = MaterialTheme.typography.bodySmall)
                Text("Found: ${it.evidence}${if (it.isPreferenceBased) " Based on your preferences." else ""}")
            }
        }
        SectionCard {
            Text("Score breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            s.subscores.forEach {
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
            val alternatives = container.alternativeEngine.suggestFor(p, preferences)
            if (alternatives.isEmpty()) {
                Text("No stronger alternatives are available from cached or mock data yet.")
            } else {
                alternatives.forEach {
                    Text("${it.product.name} / ${it.score.overallScore}")
                    Text(it.whyBetter)
                    it.tradeoff?.let { tradeoff -> Text(tradeoff) }
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
