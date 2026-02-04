package com.example.prototype

import android.util.Log
import java.util.Locale

object TextAnalyzer {

    private const val TAG = "TextAnalyzer"

    // Map Word -> Severity
    private val WORD_DB = mapOf(
        "stupid" to "LOW",
        "idiot" to "LOW",
        "scam" to "MEDIUM",
        "hate" to "MEDIUM",
        "kill" to "HIGH",
        "badword" to "HIGH"
    )

    data class Incident(
        val word: String,
        val severity: String
    )

    data class AnalysisResult(
        val isClean: Boolean,
        val incidents: List<Incident>
    )

    fun analyze(rawText: String): AnalysisResult {
        val lowerCaseText = rawText.lowercase(Locale.getDefault())
        val scrubbedText = lowerCaseText.replace(Regex("[^a-z0-9]"), " ")

        // Tokenize and filter short noise
        val tokens = scrubbedText.split("\\s+".toRegex())
            .filter { it.isNotBlank() && it.length > 2 }

        val foundIncidents = mutableListOf<Incident>()

        // Check tokens against our DB
        for (token in tokens) {
            if (WORD_DB.containsKey(token)) {
                val severity = WORD_DB[token] ?: "LOW"
                foundIncidents.add(Incident(token, severity))
            }
        }

        val distinctIncidents = foundIncidents.distinctBy { it.word }

        return AnalysisResult(
            isClean = distinctIncidents.isEmpty(),
            incidents = distinctIncidents
        )
    }
}