package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.SectionCard

@Composable
fun HomeScreen(container: AppContainer, navController: NavController) {
    val history by container.productRepository.observeHistory().collectAsState(initial = emptyList())
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("ClearCart", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Scan before you buy, with calm explanations and local scan history.", style = MaterialTheme.typography.bodyLarge)
        Button(onClick = { navController.navigate("scanner") }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Icon(Icons.Default.CameraAlt, null)
            Spacer(Modifier.padding(6.dp))
            Text("Scan Product")
        }
        OutlinedButton(onClick = { navController.navigate("search") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.Search, null)
            Spacer(Modifier.padding(6.dp))
            Text("Search Products")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ElevatedButton(onClick = { navController.navigate("compare") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CompareArrows, null)
                Text("Compare")
            }
            ElevatedButton(onClick = { navController.navigate("preferences") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Tune, null)
                Text("My Preferences")
            }
        }
        SectionCard {
            Text("Recent scans", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (history.isEmpty()) Text("No scans yet. Try barcode 0123456789012 for mock food data.")
            history.take(4).forEach {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(it.productName, modifier = Modifier.weight(1f))
                    Text("${it.score}")
                }
            }
            OutlinedButton(onClick = { navController.navigate("history") }) {
                Icon(Icons.Default.History, null)
                Text("Open History")
            }
        }
        SectionCard {
            Text("Saved favorites", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            val favorites = history.filter { it.favorite }
            if (favorites.isEmpty()) Text("Favorites will appear here.")
            favorites.take(3).forEach { Text(it.productName) }
            OutlinedButton(onClick = { navController.navigate("history") }) {
                Icon(Icons.Default.Favorite, null)
                Text("Manage Favorites")
            }
        }
        SectionCard {
            Text("No account required. Scan history stays on your device.")
            OutlinedButton(onClick = { navController.navigate("privacy") }) {
                Icon(Icons.Default.PrivacyTip, null)
                Text("Privacy")
            }
        }
    }
}
