package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.SectionCard

@Composable
fun PrivacyScreen(container: AppContainer, navController: NavController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Privacy", style = MaterialTheme.typography.headlineMedium)
        SectionCard {
            Text("No account required.")
            Text("Scan history is stored on your device using a local Room database.")
            Text("Product lookup sends the barcode to an external product database such as Open Food Facts.")
            Text("No ads or tracking are implemented in this MVP.")
            Text("You can clear local history from the History screen.")
        }
        SectionCard {
            Text("Data attribution", style = MaterialTheme.typography.titleMedium)
            Text("Food product data may come from Open Food Facts. Cosmetic and personal-care support is structured for Open Beauty Facts expansion.")
        }
    }
}
