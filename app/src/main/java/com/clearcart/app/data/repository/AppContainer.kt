package com.clearcart.app.data.repository

import android.content.Context
import androidx.room.Room
import com.clearcart.app.data.api.OpenBeautyFactsApi
import com.clearcart.app.data.api.OpenFoodFactsApi
import com.clearcart.app.data.db.AppDatabase
import com.clearcart.app.domain.alternatives.AlternativeEngine
import com.clearcart.app.domain.confidence.ConfidenceEngine
import com.clearcart.app.domain.preferences.PreferencesRepository
import com.clearcart.app.domain.scoring.ScoringEngine
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class AppContainer(context: Context) {
    private val db = Room.databaseBuilder(context, AppDatabase::class.java, "clearcart.db")
        .fallbackToDestructiveMigration(true)
        .build()

    private val openFoodFactsApi = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenFoodFactsApi::class.java)

    private val openBeautyFactsApi = Retrofit.Builder()
        .baseUrl("https://world.openbeautyfacts.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenBeautyFactsApi::class.java)

    val preferencesRepository = PreferencesRepository(context)
    val scoringEngine = ScoringEngine()
    val confidenceEngine = ConfidenceEngine()
    val productRepository = ProductRepository(
        providers = listOf(
            OpenFoodFactsProvider(openFoodFactsApi, confidenceEngine),
            OpenBeautyFactsProvider(openBeautyFactsApi, confidenceEngine),
        ),
        scanDao = db.scanDao(),
        scoringEngine = scoringEngine,
    )
    val alternativeEngine = AlternativeEngine(scoringEngine)
}
