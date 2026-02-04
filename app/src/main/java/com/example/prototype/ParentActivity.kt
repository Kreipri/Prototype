package com.example.prototype

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog // Use AndroidX AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ParentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // UI References
    private lateinit var txtLogs: TextView
    private lateinit var txtStatus: TextView

    // Stats UI References
    private lateinit var txtHigh: TextView
    private lateinit var txtMed: TextView
    private lateinit var txtLow: TextView
    private lateinit var progressHigh: ProgressBar
    private lateinit var progressMed: ProgressBar
    private lateinit var progressLow: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent)

        // 1. Bind Views (Matching the XML IDs)
        txtLogs = findViewById(R.id.txtParentLogs)
        txtStatus = findViewById(R.id.txtTargetId)

        txtHigh = findViewById(R.id.txtHighCount)
        txtMed = findViewById(R.id.txtMedCount)
        txtLow = findViewById(R.id.txtLowCount)

        progressHigh = findViewById(R.id.progressHigh)
        progressMed = findViewById(R.id.progressMed)
        progressLow = findViewById(R.id.progressLow)

        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        var targetId = prefs.getString("target_id", "NOT_LINKED") ?: "NOT_LINKED"

        txtStatus.text = targetId

        // 2. Linking Button Logic
        findViewById<Button>(R.id.btnLinkChild).setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter Child ID"

            AlertDialog.Builder(this)
                .setTitle("Link Device")
                .setView(input)
                .setPositiveButton("Save") { _, _ -> // Explicitly ignore arguments
                    val newId = input.text.toString().trim()
                    if (newId.isNotEmpty()) {
                        prefs.edit().putString("target_id", newId).apply()
                        targetId = newId
                        txtStatus.text = newId
                        fetchLogs(newId) // Refresh immediately
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 3. Refresh & Logout Buttons
        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            fetchLogs(targetId)
        }

        findViewById<Button>(R.id.btnLogoutParent).setOnClickListener {
            // ✅ NEW: Wipes only the role, keeps the "target_id" you linked to
            getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
                .edit()
                .remove("role")
                .apply()

            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // 4. Initial Load
        fetchLogs(targetId)
    }

    private fun fetchLogs(targetId: String) {
        if (targetId == "NOT_LINKED") {
            txtLogs.text = "Please link a child device first."
            return
        }

        txtLogs.text = "Fetching data..."

        db.collection("monitor_sessions")
            .whereEqualTo("device_id", targetId)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    txtLogs.text = "No logs found for ID: $targetId"
                    return@addOnSuccessListener
                }

                val sb = StringBuilder()
                var high = 0
                var med = 0
                var low = 0

                for (doc in documents) {
                    val time = doc.get("uploaded_at")

                    // Safe Cast to List of Maps
                    val incidents = doc.get("incidents") as? List<Map<String, Any>>

                    if (incidents != null) {
                        for (incident in incidents) {
                            val sev = incident["severity"] as? String ?: "LOW"
                            val word = incident["word"] as? String ?: "?"

                            // Count stats
                            when (sev) {
                                "HIGH" -> high++
                                "MEDIUM" -> med++
                                "LOW" -> low++
                            }

                            sb.append("[$sev] Found '$word' on ${incident["app"]}\n")
                        }
                        sb.append("--- Uploaded: $time ---\n\n")
                    }
                }

                // Update Logs Text
                txtLogs.text = sb.toString()

                // Update Dashboard Graphs
                updateGraphs(high, med, low)
            }
            .addOnFailureListener { e ->
                txtLogs.text = "Error: ${e.message}"
            }
    }

    private fun updateGraphs(high: Int, med: Int, low: Int) {
        val total = high + med + low

        // Prevent divide by zero error
        if (total == 0) {
            progressHigh.progress = 0
            progressMed.progress = 0
            progressLow.progress = 0
            txtHigh.text = "0 events"
            txtMed.text = "0 events"
            txtLow.text = "0 events"
            return
        }

        // Calculate Percentages
        progressHigh.progress = (high * 100) / total
        progressMed.progress = (med * 100) / total
        progressLow.progress = (low * 100) / total

        txtHigh.text = "$high events"
        txtMed.text = "$med events"
        txtLow.text = "$low events"
    }
}