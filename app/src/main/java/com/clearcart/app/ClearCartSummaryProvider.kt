package com.clearcart.app

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import androidx.room.Room
import com.clearcart.app.data.db.AppDatabase
import kotlinx.coroutines.runBlocking

class ClearCartSummaryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (uri.authority != AUTHORITY || uri.lastPathSegment != PATH_SUMMARY) return null
        val appContext = context?.applicationContext ?: return MatrixCursor(COLUMNS)
        val dao = Room.databaseBuilder(appContext, AppDatabase::class.java, "clearcart.db")
            .fallbackToDestructiveMigration(true)
            .build()
            .scanDao()
        val summary = runBlocking {
            val scans = dao.scanCount()
            val favorites = dao.favoriteCount()
            val average = dao.averageScore()?.toInt() ?: 0
            val lowerConfidence = dao.lowerConfidenceCount()
            val latest = dao.latestProductNames()
            val status = if (scans == 0) "No product scans yet" else "Product history ready"
            val alert = if (lowerConfidence > 0) "$lowerConfidence products have limited data confidence." else null
            Summary(status, "$scans scanned products, $average average score", alert, "$scans scans|$favorites favorites|$average avg score", latest.ifEmpty { listOf("No recent products") }.joinToString("|"))
        }
        return MatrixCursor(COLUMNS).apply {
            addRow(arrayOf(APP_ID, summary.status, summary.keyInfo, summary.alert.orEmpty(), summary.counts, summary.dueSoon, "just now", "ClearCart summary provider"))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private data class Summary(val status: String, val keyInfo: String, val alert: String?, val counts: String, val dueSoon: String)

    companion object {
        private const val AUTHORITY = "com.clearcart.app.summary"
        private const val PATH_SUMMARY = "summary"
        private const val APP_ID = "clearcart"
        private val COLUMNS = arrayOf("app_id", "status", "key_info", "alert", "counts", "due_soon", "last_updated", "source")
    }
}
