package com.example.prototype.data.model

data class Incident(
    val word: String,
    val severity: String, // "HIGH", "MEDIUM", "LOW"
    val appName: String,
    val timestamp: Long = System.currentTimeMillis()
)