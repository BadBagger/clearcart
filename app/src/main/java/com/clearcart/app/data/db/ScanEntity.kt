package com.clearcart.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scans")
data class ScanEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val brand: String,
    val score: Int,
    val dateScanned: Long,
    val productType: String,
    val favorite: Boolean,
    val rawApiResponse: String?,
    val dataConfidence: String,
    val category: String,
    val imageUrl: String?,
    val ingredientsText: String,
    val productJson: String?,
)
