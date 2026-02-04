package com.example.prototype.data.remote // <--- Note the new package!

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.File

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
            Log.d(TAG, "☁️ Nothing to sync.")
            return
        }

        Log.d(TAG, "☁️ Found ${newLogs.size} new incidents. Saving to Sub-Collection...")

        val db = FirebaseFirestore.getInstance()
        val prefs = context.getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val myDeviceId = prefs.getString("device_id", "unknown_device") ?: "unknown_device"

        // 2. TARGET: monitor_sessions/{DEVICE_ID}/logs/
        val userDocRef = db.collection("monitor_sessions").document(myDeviceId)

        // Use a BATCH write for safety (All or Nothing)
        val batch = db.batch()

        for (log in newLogs) {
            // Create a new document ID automatically for each log
            val newLogRef = userDocRef.collection("logs").document()

            val logData = hashMapOf(
                "word" to log.word,
                "severity" to log.severity,
                "app" to log.app,
                "timestamp" to log.timestamp
            )

            batch.set(newLogRef, logData)
        }

        // 3. COMMIT THE BATCH
        batch.commit()
            .addOnSuccessListener {
                Log.d(TAG, "✅ Sync Success! Saved ${newLogs.size} logs.")
                // Update the "High Score" timestamp
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
        val file = File(context.filesDir, "incidents_log.json")
        if (!file.exists()) return entries

        try {
            file.readLines().forEach { line ->
                if (line.trim().isNotEmpty()) {
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

    // --- Helper: Shared Preferences ---
    private fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    private fun saveLastSyncTime(context: Context, time: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC, time).apply()
    }
}