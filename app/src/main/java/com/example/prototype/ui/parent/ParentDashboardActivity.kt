package com.example.prototype.ui.parent

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype.R
import com.example.prototype.ui.welcome.RoleSelectionActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

/**
 * Dashboard for the PARENT role.
 *
 * Responsibilities:
 * 1. Managing the "Linked Child" ID.
 * 2. Fetching & Displaying logs from Firestore.
 * 3. Visualizing Incident Statistics (High/Med/Low severity).
 */
class ParentDashboardActivity : AppCompatActivity() {

    // --- CONSTANTS ---
    companion object {
        private const val PREFS_NAME = "AppConfig"
        private const val KEY_TARGET_ID = "target_id"
        private const val KEY_ROLE = "role"

        // Firestore Collections
        private const val COLLECTION_SESSIONS = "monitor_sessions"
        private const val SUBCOLLECTION_LOGS = "logs"

        // Severity Levels
        private const val SEVERITY_HIGH = "HIGH"
        private const val SEVERITY_MED = "MEDIUM"
        private const val SEVERITY_LOW = "LOW"
    }

    private val db = FirebaseFirestore.getInstance()

    // --- UI COMPONENTS ---
    private lateinit var txtLogs: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtHigh: TextView
    private lateinit var txtMed: TextView
    private lateinit var txtLow: TextView
    private lateinit var progressHigh: ProgressBar
    private lateinit var progressMed: ProgressBar
    private lateinit var progressLow: ProgressBar

    // --- STATE ---
    private var currentTargetId: String = "NOT_LINKED"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        initializeViews()
        loadSavedTargetId()
        setupClickListeners()

        // Initial Data Load
        refreshDashboard()
    }

    /**
     * Binds all XML views to local variables.
     */
    private fun initializeViews() {
        txtLogs = findViewById(R.id.txtParentLogs)
        txtStatus = findViewById(R.id.txtTargetId)

        // Stats Views
        txtHigh = findViewById(R.id.txtHighCount)
        txtMed = findViewById(R.id.txtMedCount)
        txtLow = findViewById(R.id.txtLowCount)

        progressHigh = findViewById(R.id.progressHigh)
        progressMed = findViewById(R.id.progressMed)
        progressLow = findViewById(R.id.progressLow)
    }

    /**
     * Retrieves the last linked Child ID from shared preferences.
     */
    private fun loadSavedTargetId() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        currentTargetId = prefs.getString(KEY_TARGET_ID, "NOT_LINKED") ?: "NOT_LINKED"
        txtStatus.text = currentTargetId
    }

    /**
     * Sets up all button interactions.
     */
    private fun setupClickListeners() {
        // 1. Link New Device
        findViewById<Button>(R.id.btnLinkChild).setOnClickListener {
            showLinkDeviceDialog()
        }

        // 2. Refresh Data manually
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            refreshDashboard()
        }

        // 3. Logout
        findViewById<Button>(R.id.btnLogoutParent).setOnClickListener {
            performLogout()
        }
    }

    private fun showLinkDeviceDialog() {
        val input = EditText(this)
        input.hint = "Enter Child Device ID"

        AlertDialog.Builder(this)
            .setTitle("Link Child Device")
            .setView(input)
            .setPositiveButton("Link") { _, _ ->
                val newId = input.text.toString().trim()
                if (newId.isNotEmpty()) {
                    updateTargetId(newId)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateTargetId(newId: String) {
        // Save to Disk
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit {
                putString(KEY_TARGET_ID, newId)
            }

        // Update State & UI
        currentTargetId = newId
        txtStatus.text = newId

        // Fetch new data immediately
        refreshDashboard()
    }

    private fun performLogout() {
        // We only remove the ROLE, so the parent doesn't lose the Child ID they just typed.
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit {
                remove(KEY_ROLE)
            }

        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }

    /**
     * Main Data Fetching Logic.
     * Connects to Firestore Sub-collection: monitor_sessions/{ID}/logs
     */
    private fun refreshDashboard() {
        if (currentTargetId == "NOT_LINKED") {
            txtLogs.text = "⚠️ No Child Device Linked.\nTap 'Test Linking' to connect."
            updateGraphs(0, 0, 0)
            return
        }

        txtLogs.text = "Fetching latest activity..."

        db.collection(COLLECTION_SESSIONS)
            .document(currentTargetId)
            .collection(SUBCOLLECTION_LOGS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    txtLogs.text = "No activity logs found for ID: $currentTargetId"
                    updateGraphs(0, 0, 0)
                    return@addOnSuccessListener
                }

                // Process Data
                processLogDocuments(documents)
            }
            .addOnFailureListener { e ->
                txtLogs.text = "Error fetching data: ${e.message}"
            }
    }

    /**
     * Processes raw Firestore documents into UI Text and Stats.
     */
    private fun processLogDocuments(documents: com.google.firebase.firestore.QuerySnapshot) {
        val sb = StringBuilder()
        var high = 0
        var med = 0
        var low = 0
        val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

        for (doc in documents) {
            val word = doc.getString("word") ?: "?"
            val severity = doc.getString("severity") ?: SEVERITY_LOW
            val app = doc.getString("app") ?: "Unknown"
            val ts = doc.getLong("timestamp") ?: 0L

            val dateStr = sdf.format(Date(ts))

            // Update Counters
            when (severity) {
                SEVERITY_HIGH -> high++
                SEVERITY_MED -> med++
                SEVERITY_LOW -> low++
                else -> low++
            }

            sb.append("[$dateStr] $severity: '$word' ($app)\n")
        }

        // Update UI
        txtLogs.text = sb.toString()
        updateGraphs(high, med, low)
    }

    private fun updateGraphs(high: Int, med: Int, low: Int) {
        val total = high + med + low

        // Handle "Zero Data" case to avoid divide-by-zero errors
        if (total == 0) {
            resetGraphs()
            return
        }

        // Calculate Percentages
        progressHigh.progress = (high * 100) / total
        progressMed.progress = (med * 100) / total
        progressLow.progress = (low * 100) / total

        txtHigh.text = "$high"
        txtMed.text = "$med"
        txtLow.text = "$low"
    }

    private fun resetGraphs() {
        progressHigh.progress = 0
        progressMed.progress = 0
        progressLow.progress = 0
        txtHigh.text = "0"
        txtMed.text = "0"
        txtLow.text = "0"
    }
}