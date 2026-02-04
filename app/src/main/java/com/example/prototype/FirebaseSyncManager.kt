package com.example.prototype

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FirebaseSyncManager {

    private const val TAG = "FirebaseSync"
    private const val PREFS_NAME = "SyncPrefs"
    private const val KEY_LAST_SYNC = "last_sync_time"

    // 1. Upload logs that happened AFTER the last sync
    fun syncPendingLogs(context: Context) {
        val lastSyncTime = getLastSyncTime(context)
        val allLogs = readLocalLogs(context)

        // FILTER: Only get logs that are newer than our last upload
        val newLogs = allLogs.filter { it.timestamp > lastSyncTime }

        if (newLogs.isEmpty()) {
            Log.d(TAG, "☁️ Nothing to sync. (0 Writes used)")
            return
        }

        Log.d(TAG, "☁️ Found ${newLogs.size} new incidents. Batching upload...")

        // 2. BATCH UPLOAD: Create ONE document containing all these logs
        // Structure: Collection "DailyReports" -> Document "Session_ID"
        val db = FirebaseFirestore.getInstance()

        val sessionData = hashMapOf(
            "uploaded_at" to System.currentTimeMillis(),
            "device_id" to "child_device_01", // You can make this dynamic
            "incidents" to newLogs // This sends the entire list as one array
        )

        // 3. THE ONE WRITE OPERATION
        db.collection("monitor_sessions")
            .add(sessionData)
            .addOnSuccessListener {
                Log.d(TAG, "✅ Batch Upload Success! Saved ${newLogs.size} logs in 1 write.")

                // Save the new "High Score" (timestamp) so we don't upload these again
                saveLastSyncTime(context, System.currentTimeMillis())
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Upload Failed", e)
            }
    }

    // --- Helper: Read and Parse Local JSON ---
    data class LogEntry(
        val word: String,
        val severity: String,
        val app: String,
        val timestamp: Long
    )

    private fun readLocalLogs(context: Context): List<LogEntry> {
        val entries = mutableListOf<LogEntry>()
        val file = File(context.filesDir, "incidents_log.json") // Must match IncidentLogger filename
        if (!file.exists()) return entries

        try {
            // The file is a list of JSON objects separated by newlines/commas
            // We need to parse it manually since it's "pseudo-json"
            val content = file.readText()
            // Wrap in brackets to make it a valid JSON array for parsing
            val jsonArrayString = "[$content]"
                .replace(",\n]", "]") // Fix trailing comma if exists
                .replace(",]", "]")   // Extra safety

            // Note: If your logger writes line-by-line, parsing line-by-line is safer:
            file.readLines().forEach { line ->
                if (line.trim().isNotEmpty()) {
                    // Remove trailing comma
                    val cleanLine = line.trim().removeSuffix(",")
                    try {
                        val obj = JSONObject(cleanLine)
                        entries.add(LogEntry(
                            word = obj.getString("word"),
                            severity = obj.getString("severity"),
                            app = obj.getString("app"),
                            timestamp = obj.getLong("timestamp")
                        ))
                    } catch (e: Exception) { /* Ignore bad lines */ }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing local logs", e)
        }
        return entries
    }

    // --- Helper: Shared Preferences for Last Sync Time ---
    private fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    private fun saveLastSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }
}