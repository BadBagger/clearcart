package com.clearcart.app.domain.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.clearcart.app.data.model.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.clearCartPreferencesDataStore by preferencesDataStore(name = "clearcart_preferences_datastore")

class PreferencesRepository(context: Context) {
    private val appContext = context.applicationContext
    private val legacyPrefs = appContext.getSharedPreferences("clearcart_preferences", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow(readLegacyDefaults())
    val state: StateFlow<UserPreferences> = _state.asStateFlow()

    init {
        scope.launch {
            migrateLegacyPreferencesIfNeeded()
            appContext.clearCartPreferencesDataStore.data
                .map(::read)
                .collectLatest { _state.value = it }
        }
    }

    fun update(transform: (UserPreferences) -> UserPreferences) {
        val next = transform(_state.value)
        _state.value = next
        scope.launch {
            appContext.clearCartPreferencesDataStore.edit { prefs ->
                write(prefs, next)
            }
        }
    }

    private suspend fun migrateLegacyPreferencesIfNeeded() {
        appContext.clearCartPreferencesDataStore.edit { prefs ->
            if (prefs[Migrated] == true || legacyPrefs.all.isEmpty()) return@edit
            write(prefs, readLegacyDefaults())
            prefs[Migrated] = true
        }
    }

    private fun read(prefs: Preferences) = UserPreferences(
        allergensToAvoid = prefs[Allergens].orEmpty(),
        vegan = prefs[Vegan] ?: false,
        vegetarian = prefs[Vegetarian] ?: false,
        glutenFree = prefs[GlutenFree] ?: false,
        dairyFree = prefs[DairyFree] ?: false,
        lowSugar = prefs[LowSugar] ?: false,
        lowSodium = prefs[LowSodium] ?: false,
        fewerAdditives = prefs[FewerAdditives] ?: false,
        highProtein = prefs[HighProtein] ?: false,
        avoidArtificialColors = prefs[AvoidArtificialColors] ?: false,
        avoidFragrance = prefs[AvoidFragrance] ?: false,
        sensitiveSkin = prefs[SensitiveSkin] ?: false,
        budgetConscious = prefs[BudgetConscious] ?: false,
        simpleIngredients = prefs[SimpleIngredients] ?: false,
        preferOrganic = prefs[PreferOrganic] ?: false,
        ingredientAvoidList = prefs[IngredientAvoidList].orEmpty(),
        ingredientOkayList = prefs[IngredientOkayList].orEmpty(),
        brandAvoidList = prefs[BrandAvoidList].orEmpty(),
        categoryAvoidList = prefs[CategoryAvoidList].orEmpty(),
        preferFewerWarningLabels = prefs[PreferFewerWarningLabels] ?: false,
    )

    private fun write(prefs: MutablePreferencesLike, value: UserPreferences) {
        prefs[Allergens] = value.allergensToAvoid
        prefs[Vegan] = value.vegan
        prefs[Vegetarian] = value.vegetarian
        prefs[GlutenFree] = value.glutenFree
        prefs[DairyFree] = value.dairyFree
        prefs[LowSugar] = value.lowSugar
        prefs[LowSodium] = value.lowSodium
        prefs[FewerAdditives] = value.fewerAdditives
        prefs[HighProtein] = value.highProtein
        prefs[AvoidArtificialColors] = value.avoidArtificialColors
        prefs[AvoidFragrance] = value.avoidFragrance
        prefs[SensitiveSkin] = value.sensitiveSkin
        prefs[BudgetConscious] = value.budgetConscious
        prefs[SimpleIngredients] = value.simpleIngredients
        prefs[PreferOrganic] = value.preferOrganic
        prefs[IngredientAvoidList] = value.ingredientAvoidList
        prefs[IngredientOkayList] = value.ingredientOkayList
        prefs[BrandAvoidList] = value.brandAvoidList
        prefs[CategoryAvoidList] = value.categoryAvoidList
        prefs[PreferFewerWarningLabels] = value.preferFewerWarningLabels
        prefs[Migrated] = true
    }

    private fun readLegacyDefaults() = UserPreferences(
        allergensToAvoid = legacyPrefs.getStringSet("allergens", emptySet()).orEmpty(),
        vegan = legacyPrefs.getBoolean("vegan", false),
        vegetarian = legacyPrefs.getBoolean("vegetarian", false),
        glutenFree = legacyPrefs.getBoolean("glutenFree", false),
        dairyFree = legacyPrefs.getBoolean("dairyFree", false),
        lowSugar = legacyPrefs.getBoolean("lowSugar", false),
        lowSodium = legacyPrefs.getBoolean("lowSodium", false),
        fewerAdditives = legacyPrefs.getBoolean("fewerAdditives", false),
        highProtein = legacyPrefs.getBoolean("highProtein", false),
        avoidArtificialColors = legacyPrefs.getBoolean("avoidArtificialColors", false),
        avoidFragrance = legacyPrefs.getBoolean("avoidFragrance", false),
        sensitiveSkin = legacyPrefs.getBoolean("sensitiveSkin", false),
        budgetConscious = legacyPrefs.getBoolean("budgetConscious", false),
        simpleIngredients = legacyPrefs.getBoolean("simpleIngredients", false),
        preferOrganic = legacyPrefs.getBoolean("preferOrganic", false),
        ingredientAvoidList = legacyPrefs.getStringSet("ingredientAvoidList", emptySet()).orEmpty(),
        ingredientOkayList = legacyPrefs.getStringSet("ingredientOkayList", emptySet()).orEmpty(),
    )

    private companion object {
        val Allergens = stringSetPreferencesKey("allergens")
        val Vegan = booleanPreferencesKey("vegan")
        val Vegetarian = booleanPreferencesKey("vegetarian")
        val GlutenFree = booleanPreferencesKey("gluten_free")
        val DairyFree = booleanPreferencesKey("dairy_free")
        val LowSugar = booleanPreferencesKey("low_sugar")
        val LowSodium = booleanPreferencesKey("low_sodium")
        val FewerAdditives = booleanPreferencesKey("fewer_additives")
        val HighProtein = booleanPreferencesKey("high_protein")
        val AvoidArtificialColors = booleanPreferencesKey("avoid_artificial_colors")
        val AvoidFragrance = booleanPreferencesKey("avoid_fragrance")
        val SensitiveSkin = booleanPreferencesKey("sensitive_skin")
        val BudgetConscious = booleanPreferencesKey("budget_conscious")
        val SimpleIngredients = booleanPreferencesKey("simple_ingredients")
        val PreferOrganic = booleanPreferencesKey("prefer_organic")
        val IngredientAvoidList = stringSetPreferencesKey("ingredient_avoid_list")
        val IngredientOkayList = stringSetPreferencesKey("ingredient_okay_list")
        val BrandAvoidList = stringSetPreferencesKey("brand_avoid_list")
        val CategoryAvoidList = stringSetPreferencesKey("category_avoid_list")
        val PreferFewerWarningLabels = booleanPreferencesKey("prefer_fewer_warning_labels")
        val Migrated = booleanPreferencesKey("migrated_from_shared_preferences")
    }
}

private typealias MutablePreferencesLike = androidx.datastore.preferences.core.MutablePreferences
