package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.model.ConfidenceLevel
import com.clearcart.app.data.model.Nutrition
import com.clearcart.app.data.model.Product
import com.clearcart.app.data.model.ProductSource
import com.clearcart.app.data.repository.AppContainer
import kotlinx.coroutines.launch

@Composable
fun ManualEntryScreen(
    container: AppContainer,
    navController: NavController,
    initialBarcode: String = "",
    initialName: String = "",
    initialIngredients: String = "",
) {
    val preferences by container.preferencesRepository.state.collectAsState()
    val scope = rememberCoroutineScope()
    var barcode by remember(initialBarcode) { mutableStateOf(initialBarcode.ifBlank { "manual-${System.currentTimeMillis()}" }) }
    var name by remember(initialName) { mutableStateOf(initialName) }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var ingredients by remember(initialIngredients) { mutableStateOf(initialIngredients) }
    var sugar by remember { mutableStateOf("") }
    var sodium by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Manual Product Entry", style = MaterialTheme.typography.headlineMedium)
        Text("Saved locally only. Confidence is marked as user-entered.")
        OutlinedTextField(barcode, { barcode = it }, label = { Text("Barcode or local ID") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(name, { name = it }, label = { Text("Product name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(brand, { brand = it }, label = { Text("Brand") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(category, { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(ingredients, { ingredients = it }, label = { Text("Ingredients") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
        OutlinedTextField(sugar, { sugar = it }, label = { Text("Sugar g / 100g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(sodium, { sodium = it }, label = { Text("Sodium g / 100g") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val product = Product(
                barcode = barcode.ifBlank { "manual-${System.currentTimeMillis()}" },
                name = name.ifBlank { "User-entered product" },
                brand = brand,
                category = category,
                imageUrl = null,
                ingredientsText = ingredients,
                allergens = emptyList(),
                labels = emptyList(),
                nutrition = Nutrition(null, sugar.toDoubleOrNull(), sodium.toDoubleOrNull(), null, null, null),
                additives = emptyList(),
                novaGroup = null,
                nutriScore = null,
                source = ProductSource.UserEntered,
                lastUpdated = null,
                confidenceLevel = ConfidenceLevel.UserEntered,
            )
            scope.launch {
                container.productRepository.saveManual(product, preferences)
                navController.navigate("product/${product.barcode}")
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save and Analyze") }
    }
}
