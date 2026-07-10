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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.clearcart.app.data.model.UserPreferences
import com.clearcart.app.data.repository.AppContainer
import com.clearcart.app.ui.components.SectionCard

@Composable
fun PreferencesScreen(container: AppContainer, navController: NavController) {
    val preferences by container.preferencesRepository.state.collectAsState()
    var allergenText by remember(preferences.allergensToAvoid) { mutableStateOf(preferences.allergensToAvoid.joinToString(", ")) }
    var ingredientAvoidText by remember(preferences.ingredientAvoidList) { mutableStateOf(preferences.ingredientAvoidList.joinToString(", ")) }
    var ingredientOkayText by remember(preferences.ingredientOkayList) { mutableStateOf(preferences.ingredientOkayList.joinToString(", ")) }
    var brandAvoidText by remember(preferences.brandAvoidList) { mutableStateOf(preferences.brandAvoidList.joinToString(", ")) }
    var categoryAvoidText by remember(preferences.categoryAvoidList) { mutableStateOf(preferences.categoryAvoidList.joinToString(", ")) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("My Preferences", style = MaterialTheme.typography.headlineMedium)
        Text("These affect Personal Fit, warnings, and suggestions. Raw product facts stay visible and preferences stay on this device.")

        SectionCard {
            SectionTitle("Food and drink")
            PreferenceToggle("Lower sugar", preferences.lowSugar) { container.preferencesRepository.update { p -> p.copy(lowSugar = it) } }
            PreferenceToggle("Lower sodium", preferences.lowSodium) { container.preferencesRepository.update { p -> p.copy(lowSodium = it) } }
            PreferenceToggle("Fewer additives", preferences.fewerAdditives) { container.preferencesRepository.update { p -> p.copy(fewerAdditives = it) } }
            PreferenceToggle("Higher protein", preferences.highProtein) { container.preferencesRepository.update { p -> p.copy(highProtein = it) } }
            PreferenceToggle("Simpler ingredients", preferences.simpleIngredients) { container.preferencesRepository.update { p -> p.copy(simpleIngredients = it) } }
            PreferenceToggle("Vegetarian", preferences.vegetarian) { container.preferencesRepository.update { p -> p.copy(vegetarian = it) } }
            PreferenceToggle("Vegan", preferences.vegan) { container.preferencesRepository.update { p -> p.copy(vegan = it) } }
            PreferenceToggle("Gluten-free preference", preferences.glutenFree) { container.preferencesRepository.update { p -> p.copy(glutenFree = it) } }
            PreferenceToggle("Dairy-free preference", preferences.dairyFree) { container.preferencesRepository.update { p -> p.copy(dairyFree = it) } }
        }

        SectionCard {
            SectionTitle("Avoid lists")
            PreferenceTextField("Ingredients to avoid", ingredientAvoidText) {
                ingredientAvoidText = it
                container.preferencesRepository.update { p -> p.copy(ingredientAvoidList = it.toPreferenceSet()) }
            }
            PreferenceTextField("Allergens to flag", allergenText) {
                allergenText = it
                container.preferencesRepository.update { p -> p.copy(allergensToAvoid = it.toPreferenceSet()) }
            }
            PreferenceTextField("Brands to avoid", brandAvoidText) {
                brandAvoidText = it
                container.preferencesRepository.update { p -> p.copy(brandAvoidList = it.toPreferenceSet()) }
            }
            PreferenceTextField("Categories to avoid", categoryAvoidText) {
                categoryAvoidText = it
                container.preferencesRepository.update { p -> p.copy(categoryAvoidList = it.toPreferenceSet()) }
            }
            PreferenceTextField("Ingredients marked okay", ingredientOkayText) {
                ingredientOkayText = it
                container.preferencesRepository.update { p -> p.copy(ingredientOkayList = it.toPreferenceSet()) }
            }
        }

        SectionCard {
            SectionTitle("Cosmetics")
            PreferenceToggle("Avoid fragrance", preferences.avoidFragrance) { container.preferencesRepository.update { p -> p.copy(avoidFragrance = it) } }
            PreferenceToggle("Prefer simpler ingredient lists", preferences.simpleIngredients) { container.preferencesRepository.update { p -> p.copy(simpleIngredients = it) } }
            PreferenceToggle("Sensitive-skin friendly preference", preferences.sensitiveSkin) { container.preferencesRepository.update { p -> p.copy(sensitiveSkin = it) } }
            Text("Use Ingredients to avoid for specific cosmetic ingredients.", style = MaterialTheme.typography.bodySmall)
        }

        SectionCard {
            SectionTitle("Household")
            PreferenceToggle("Avoid fragrance", preferences.avoidFragrance) { container.preferencesRepository.update { p -> p.copy(avoidFragrance = it) } }
            PreferenceToggle("Prefer fewer warning labels if data exists", preferences.preferFewerWarningLabels) { container.preferencesRepository.update { p -> p.copy(preferFewerWarningLabels = it) } }
            PreferenceToggle("Prefer simpler ingredient lists", preferences.simpleIngredients) { container.preferencesRepository.update { p -> p.copy(simpleIngredients = it) } }
        }
    }
}

private fun String.toPreferenceSet(): Set<String> =
    split(',')
        .map { value -> value.trim() }
        .filter { value -> value.isNotBlank() }
        .toSet()

@Composable
private fun PreferenceToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f))
        Checkbox(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SectionTitle(label: String) {
    Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PreferenceTextField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        supportingText = { Text("Separate items with commas.") },
    )
}
