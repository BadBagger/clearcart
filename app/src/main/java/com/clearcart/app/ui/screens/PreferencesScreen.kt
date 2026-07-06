package com.clearcart.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.data.repository.AppContainer

@Composable
fun PreferencesScreen(container: AppContainer, navController: NavController) {
    val preferences by container.preferencesRepository.state.collectAsState()
    var allergenText by remember(preferences.allergensToAvoid) { mutableStateOf(preferences.allergensToAvoid.joinToString(", ")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("My Preferences", style = MaterialTheme.typography.headlineMedium)
        Text("These affect warnings and suggestions, but raw facts remain visible.")
        OutlinedTextField(
            allergenText,
            {
                allergenText = it
                container.preferencesRepository.update { p -> p.copy(allergensToAvoid = it.split(',').map { value -> value.trim() }.filter { value -> value.isNotBlank() }.toSet()) }
            },
            label = { Text("Allergens to avoid") },
            modifier = Modifier.fillMaxWidth(),
        )
        PreferenceToggle("Vegan", preferences.vegan) { container.preferencesRepository.update { p -> p.copy(vegan = it) } }
        PreferenceToggle("Vegetarian", preferences.vegetarian) { container.preferencesRepository.update { p -> p.copy(vegetarian = it) } }
        PreferenceToggle("Gluten-free", preferences.glutenFree) { container.preferencesRepository.update { p -> p.copy(glutenFree = it) } }
        PreferenceToggle("Dairy-free", preferences.dairyFree) { container.preferencesRepository.update { p -> p.copy(dairyFree = it) } }
        PreferenceToggle("Low sugar", preferences.lowSugar) { container.preferencesRepository.update { p -> p.copy(lowSugar = it) } }
        PreferenceToggle("Low sodium", preferences.lowSodium) { container.preferencesRepository.update { p -> p.copy(lowSodium = it) } }
        PreferenceToggle("High protein", preferences.highProtein) { container.preferencesRepository.update { p -> p.copy(highProtein = it) } }
        PreferenceToggle("Avoid artificial colors", preferences.avoidArtificialColors) { container.preferencesRepository.update { p -> p.copy(avoidArtificialColors = it) } }
        PreferenceToggle("Avoid fragrance", preferences.avoidFragrance) { container.preferencesRepository.update { p -> p.copy(avoidFragrance = it) } }
        PreferenceToggle("Sensitive skin", preferences.sensitiveSkin) { container.preferencesRepository.update { p -> p.copy(sensitiveSkin = it) } }
        PreferenceToggle("Budget-conscious", preferences.budgetConscious) { container.preferencesRepository.update { p -> p.copy(budgetConscious = it) } }
        PreferenceToggle("Prefer simpler ingredient lists", preferences.simpleIngredients) { container.preferencesRepository.update { p -> p.copy(simpleIngredients = it) } }
        PreferenceToggle("Prefer organic", preferences.preferOrganic) { container.preferencesRepository.update { p -> p.copy(preferOrganic = it) } }
    }
}

@Composable
private fun PreferenceToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}
