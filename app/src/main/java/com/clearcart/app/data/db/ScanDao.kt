package com.clearcart.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY dateScanned DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE barcode = :barcode LIMIT 1")
    suspend fun get(barcode: String): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scan: ScanEntity)

    @Update
    suspend fun update(scan: ScanEntity)

    @Delete
    suspend fun delete(scan: ScanEntity)

    @Query("DELETE FROM scans")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM scans")
    suspend fun scanCount(): Int

    @Query("SELECT COUNT(*) FROM scans WHERE favorite = 1")
    suspend fun favoriteCount(): Int

    @Query("SELECT AVG(score) FROM scans")
    suspend fun averageScore(): Double?

    @Query("SELECT productName FROM scans ORDER BY dateScanned DESC LIMIT 3")
    suspend fun latestProductNames(): List<String>

    @Query("SELECT COUNT(*) FROM scans WHERE dataConfidence != 'High'")
    suspend fun lowerConfidenceCount(): Int
}
