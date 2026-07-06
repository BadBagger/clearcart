package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(container: AppContainer, navController: NavController) {
    val history by container.productRepository.observeHistory().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }
    val filtered = history.filter { it.productName.contains(search, true) || it.brand.contains(search, true) || it.barcode.contains(search) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(search, { search = it }, label = { Text("Search history") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
        if (filtered.isEmpty()) Text("No matching scans.")
        filtered.forEach { row ->
            SectionCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(row.productName, fontWeight = FontWeight.SemiBold)
                        Text("${row.brand} • ${row.score} • ${row.dataConfidence}")
                    }
                    IconButton(onClick = { scope.launch { container.productRepository.setFavorite(row.barcode, !row.favorite) } }) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = if (row.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                    }
                    IconButton(onClick = { scope.launch { container.productRepository.delete(row.barcode) } }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                OutlinedButton(onClick = { navController.navigate("product/${row.barcode}") }) { Text("Open") }
            }
        }
        OutlinedButton(onClick = { scope.launch { container.productRepository.clearHistory() } }) { Text("Clear History") }
    }
}
