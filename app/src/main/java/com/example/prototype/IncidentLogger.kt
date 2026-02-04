package com.example.prototype

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object IncidentLogger {

    private const val FILE_NAME = "incidents_log.json"

    // --- NEW: COOLDOWN CONFIGURATION ---
    // If we see the same word again within 5 seconds, ignore it.
    private const val COOLDOWN_MS = 10000L

    // Stores the last time we logged a specific word.
    // Example: {"stupid": 170000500, "idiot": 170000900}
    private val lastLogTimeMap = ConcurrentHashMap<String, Long>()

    fun logIncident(context: Context, word: String, severity: String, appName: String) {
        val currentTime = System.currentTimeMillis()

        val lastTime = lastLogTimeMap[word] ?: 0L
        val timeDiff = currentTime - lastTime

        // --- THE CHANGE IS HERE ---
        if (timeDiff < COOLDOWN_MS) {
            // We still update the time!
            // This resets the countdown, meaning the word must be GONE for 5s
            // before we allow a new log.
            lastLogTimeMap[word] = currentTime

            Log.d("IncidentLogger", "⏳ EXTENDING Debounce for: '$word' (Still on screen)")
            return
        }
        // --------------------------

        lastLogTimeMap[word] = currentTime

        try {
            val jsonObject = JSONObject()
            jsonObject.put("word", word)
            jsonObject.put("severity", severity)
            jsonObject.put("app", appName)
            jsonObject.put("timestamp", currentTime)
            jsonObject.put("readable_time", getReadableTime())

            val entry = jsonObject.toString() + ",\n"

            val fileOutputStream: FileOutputStream = context.openFileOutput(FILE_NAME, Context.MODE_APPEND)
            fileOutputStream.write(entry.toByteArray())
            fileOutputStream.close()

            Log.d("IncidentLogger", "✅ Saved to local storage: $entry")
        } catch (e: Exception) {
            Log.e("IncidentLogger", "Failed to save incident", e)
        }
    }

    fun getAllLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.readText() else "No logs yet."
        } catch (e: Exception) {
            "Error reading logs."
        }
    }

    private fun getReadableTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}