package com.example.prototype.domain

import java.util.Locale

/**
 * Domain Service responsible for text analysis and keyword detection.
 *
 * It contains the business logic for determining if text is "harmful"
 * and assigning a severity score. It is purely logic and has no Android dependencies.
 */
object ContentAnalyzer {

    // --- CONFIGURATION ---
    private const val TAG = "ContentAnalyzer"
    private const val MIN_WORD_LENGTH = 3

    // In a production app, this would be loaded from a secure remote JSON or Database.
    private val WORD_DATABASE = mapOf(
        "stupid" to "LOW",
        "idiot" to "LOW",
        "scam" to "MEDIUM",
        "hate" to "MEDIUM",
        "kill" to "HIGH",
        "badword" to "HIGH"
    )

    // --- DATA MODELS ---

    /**
     * Result wrapper containing the verdict and any specific incidents found.
     */
    data class AnalysisResult(
        val isClean: Boolean,
        val incidents: List<IncidentDetail>
    )

    /**
     * Internal representation of a detected keyword.
     * We map this to the main 'Incident' model later in the data layer.
     */
    data class IncidentDetail(
        val word: String,
        val severity: String
    )

    // --- PUBLIC API ---

    /**
     * Scans raw OCR text for known keywords.
     *
     * @param rawText The messy text directly from the Tesseract OCR engine.
     * @return An [AnalysisResult] containing any matches found.
     */
    fun analyze(rawText: String): AnalysisResult {
        // 1. Normalize: Convert to lowercase to ensure case-insensitive matching
        val normalizedText = rawText.lowercase(Locale.getDefault())

        // 2. Sanitize: Remove symbols/punctuation (keep only letters and numbers)
        // Regex: "[^a-z0-9]" replaces anything NOT a letter/number with a space
        val scrubbedText = normalizedText.replace(Regex("[^a-z0-9]"), " ")

        // 3. Tokenize: Split by whitespace into a list of words
        val tokens = scrubbedText.split("\\s+".toRegex())
            .filter { it.isNotBlank() && it.length >= MIN_WORD_LENGTH }

        val detectedIncidents = mutableListOf<IncidentDetail>()

        // 4. Match: Check every token against our database
        for (token in tokens) {
            if (WORD_DATABASE.containsKey(token)) {
                val severity = WORD_DATABASE[token] ?: "LOW"
                detectedIncidents.add(IncidentDetail(token, severity))
            }
        }

        return AnalysisResult(
            isClean = detectedIncidents.isEmpty(),
            incidents = detectedIncidents
        )
    }
}