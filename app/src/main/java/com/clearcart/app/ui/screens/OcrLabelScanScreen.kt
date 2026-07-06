package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ocr.OcrTextAnalyzer

@Composable
fun OcrLabelScanScreen(container: AppContainer, navController: NavController) {
    var rawText by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Label OCR", style = MaterialTheme.typography.headlineMedium)
        Text("MVP OCR structure is ready. Paste recognized label text here, edit it, then save through manual entry.")
        OutlinedTextField(rawText, { rawText = it }, label = { Text("Editable extracted text") }, modifier = Modifier.fillMaxWidth(), minLines = 7)
        Button(onClick = {
            val extraction = OcrTextAnalyzer.parse(rawText)
            parsed = listOf(
                "Name/brand: ${extraction.brandOrName}",
                "Ingredients: ${extraction.ingredients}",
                "Nutrition: ${extraction.nutritionFacts}",
                "Expiration: ${extraction.expirationDate}",
            ).joinToString("\n")
        }, modifier = Modifier.fillMaxWidth()) { Text("Analyze Text") }
        if (parsed.isNotBlank()) Text(parsed)
        Button(onClick = { navController.navigate("manual") }, modifier = Modifier.fillMaxWidth()) { Text("Save as Manual Product") }
    }
}
