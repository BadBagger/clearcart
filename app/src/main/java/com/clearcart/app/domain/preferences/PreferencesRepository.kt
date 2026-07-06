package com.clearcart.app.domain.preferences

import android.content.Context
import com.clearcart.app.data.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("clearcart_preferences", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value)
        prefs.edit()
            .putStringSet("allergens", next.allergensToAvoid)
            .putBoolean("vegan", next.vegan)
            .putBoolean("vegetarian", next.vegetarian)
            .putBoolean("glutenFree", next.glutenFree)
            .putBoolean("dairyFree", next.dairyFree)
            .putBoolean("lowSugar", next.lowSugar)
            .putBoolean("lowSodium", next.lowSodium)
            .putBoolean("highProtein", next.highProtein)
            .putBoolean("avoidArtificialColors", next.avoidArtificialColors)
            .putBoolean("avoidFragrance", next.avoidFragrance)
            .putBoolean("sensitiveSkin", next.sensitiveSkin)
            .putBoolean("budgetConscious", next.budgetConscious)
            .putBoolean("simpleIngredients", next.simpleIngredients)
            .putBoolean("preferOrganic", next.preferOrganic)
            .apply()
        _state.value = next
    }

    private fun read() = UserPreferences(
        allergensToAvoid = prefs.getStringSet("allergens", emptySet()) ?: emptySet(),
        vegan = prefs.getBoolean("vegan", false),
        vegetarian = prefs.getBoolean("vegetarian", false),
        glutenFree = prefs.getBoolean("glutenFree", false),
        dairyFree = prefs.getBoolean("dairyFree", false),
        lowSugar = prefs.getBoolean("lowSugar", false),
        lowSodium = prefs.getBoolean("lowSodium", false),
        highProtein = prefs.getBoolean("highProtein", false),
        avoidArtificialColors = prefs.getBoolean("avoidArtificialColors", false),
        avoidFragrance = prefs.getBoolean("avoidFragrance", false),
        sensitiveSkin = prefs.getBoolean("sensitiveSkin", false),
        budgetConscious = prefs.getBoolean("budgetConscious", false),
        simpleIngredients = prefs.getBoolean("simpleIngredients", false),
        preferOrganic = prefs.getBoolean("preferOrganic", false),
    )
}
