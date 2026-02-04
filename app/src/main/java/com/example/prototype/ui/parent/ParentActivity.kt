package com.example.prototype.ui.parent // <--- Note the new package!

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype.R
import com.example.prototype.ui.welcome.WelcomeActivity // Import your WelcomeActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParentActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()

    // UI References
    private lateinit var txtLogs: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtHigh: TextView
    private lateinit var txtMed: TextView
    private lateinit var txtLow: TextView
    private lateinit var progressHigh: ProgressBar
    private lateinit var progressMed: ProgressBar
    private lateinit var progressLow: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent)

        // 1. Bind Views
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

        // 2. Link Button
        findViewById<Button>(R.id.btnLinkChild).setOnClickListener {
            val input = EditText(this)
            input.hint = "Enter Child ID"
            AlertDialog.Builder(this)
                .setTitle("Link Device")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newId = input.text.toString().trim()
                    if (newId.isNotEmpty()) {
                        prefs.edit().putString("target_id", newId).apply()
                        targetId = newId
                        txtStatus.text = newId
                        fetchLogs(newId)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // 3. Refresh & Logout
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { fetchLogs(targetId) }

        findViewById<Button>(R.id.btnLogoutParent).setOnClickListener {
            // Only remove role, keep the target_id for convenience
            prefs.edit().remove("role").apply()
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        fetchLogs(targetId)
    }

    private fun fetchLogs(targetId: String) {
        if (targetId == "NOT_LINKED") {
            txtLogs.text = "Please link a child device first."
            return
        }

        txtLogs.text = "Fetching data..."

        // --- NEW SUB-COLLECTION QUERY ---
        db.collection("monitor_sessions")
            .document(targetId)
            .collection("logs") // <--- Going into the folder
            .orderBy("timestamp", Query.Direction.DESCENDING) // Sort by newest
            .limit(50)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    txtLogs.text = "No logs found for ID: $targetId"
                    updateGraphs(0, 0, 0)
                    return@addOnSuccessListener
                }

                val sb = StringBuilder()
                var high = 0
                var med = 0
                var low = 0
                val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())

                for (doc in documents) {
                    // Extract data fields
                    val word = doc.getString("word") ?: "?"
                    val severity = doc.getString("severity") ?: "LOW"
                    val app = doc.getString("app") ?: "Unknown"
                    val ts = doc.getLong("timestamp") ?: 0L

                    val dateStr = sdf.format(Date(ts))

                    // Stats
                    when (severity) {
                        "HIGH" -> high++
                        "MEDIUM" -> med++
                        "LOW" -> low++
                    }

                    sb.append("[$dateStr] $severity: '$word' in $app\n")
                }

                txtLogs.text = sb.toString()
                updateGraphs(high, med, low)
            }
            .addOnFailureListener { e ->
                txtLogs.text = "Error: ${e.message}"
            }
    }

    private fun updateGraphs(high: Int, med: Int, low: Int) {
        val total = high + med + low
        if (total == 0) {
            progressHigh.progress = 0
            progressMed.progress = 0
            progressLow.progress = 0
            txtHigh.text = "0"
            txtMed.text = "0"
            txtLow.text = "0"
            return
        }
        progressHigh.progress = (high * 100) / total
        progressMed.progress = (med * 100) / total
        progressLow.progress = (low * 100) / total

        txtHigh.text = "$high"
        txtMed.text = "$med"
        txtLow.text = "$low"
    }
}