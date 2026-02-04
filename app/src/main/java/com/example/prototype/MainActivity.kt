package com.example.prototype

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var statusOverlay: TextView
    private lateinit var statusAccess: TextView
    private lateinit var statusNotif: TextView

    private val NOTIF_REQ_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {

        // Force Firestore to tell us what is wrong
        com.google.firebase.firestore.FirebaseFirestore.setLoggingEnabled(true)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI Views
        statusOverlay = findViewById(R.id.statusOverlay)
        statusAccess = findViewById(R.id.statusAccessibility)
        statusNotif = findViewById(R.id.statusNotification)

        // Button Listeners to open respective settings
        findViewById<Button>(R.id.btnReqOverlay).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        }

        findViewById<Button>(R.id.btnReqAccess).setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnReqNotif).setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIF_REQ_CODE)
            }
        }

        findViewById<Button>(R.id.conLogBtn).setOnClickListener {
            val file = File(filesDir, "incidents_log.json")
            if (file.exists()) {
                // 1. Check if the file is physically growing
                val size = file.length()
                val lastMod = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(file.lastModified()))

                Log.d("DEBUG_LOG", "--------------------------------")
                Log.d("DEBUG_LOG", "📂 File Size: $size bytes")
                Log.d("DEBUG_LOG", "🕒 Last Modified: $lastMod")

                // 2. Read only the LAST 5 lines (The newest data)
                val lines = file.readLines()
                val tail = lines.takeLast(5)

                Log.d("DEBUG_LOG", "📜 Last 5 Entries:")
                tail.forEach { line ->
                    Log.d("DEBUG_LOG", "   $line")
                }
            } else {
                Log.e("DEBUG_LOG", "❌ File not found")
            }
        }

        findViewById<Button>(R.id.logBtn).setOnClickListener {
            showLogDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        updateDashboard()
    }

    private fun updateDashboard() {
        // 1. Check Overlay Permission
        if (Settings.canDrawOverlays(this)) {
            statusOverlay.text = "Overlay: GRANTED ✅"
            statusOverlay.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            statusOverlay.text = "Overlay: MISSING ❌"
            statusOverlay.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // 2. Check Accessibility Service
        if (isAccessibilityServiceEnabled(FacebookAccessibilityService::class.java)) {
            statusAccess.text = "Accessibility: ACTIVE ✅"
            statusAccess.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            statusAccess.text = "Accessibility: INACTIVE ❌"
            statusAccess.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // 3. Check Notification Permission (Android 13+)
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                statusNotif.text = "Notifications: GRANTED ✅"
                statusNotif.setTextColor(getColor(android.R.color.holo_green_dark))
            } else {
                statusNotif.text = "Notifications: MISSING ⚠️"
                statusNotif.setTextColor(getColor(android.R.color.holo_orange_dark))
            }
        } else {
            statusNotif.text = "Notifications: N/A (Granted) ✅"
        }
    }

    private fun showLogDialog() {
        val file = File(filesDir, "incidents_log.json")
        val logContent = if (file.exists()) {
            // Read the file and maybe format it a bit
            val raw = file.readText()
            if (raw.isBlank()) "No logs found." else raw
        } else {
            "No logs file created yet."
        }

        // 1. Create a TextView to hold the logs
        val textView = TextView(this).apply {
            text = logContent
            textSize = 14f
            setPadding(40, 40, 40, 40)
            setTextColor(android.graphics.Color.BLACK)
            setTextIsSelectable(true) // Allows you to copy text!
        }

        // 2. Wrap it in a ScrollView (So you can scroll long logs)
        val scrollView = android.widget.ScrollView(this).apply {
            addView(textView)
        }

        // 3. Build and Show the Alert
        android.app.AlertDialog.Builder(this)
            .setTitle("Incident History")
            .setView(scrollView) // Put the scrollview inside the dialog
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear Logs") { _, _ ->
                // Optional: Feature to wipe logs
                if (file.exists()) file.writeText("") // Clear file
                android.widget.Toast.makeText(this, "Logs Cleared", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    // Helper to check if your specific Accessibility Service is on
    private fun isAccessibilityServiceEnabled(service: Class<out Any>): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        val myService = ComponentName(packageName, service.name).flattenToString()

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(myService, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    // Helper for ComponentName
    private fun ComponentName(pkg: String, cls: String): android.content.ComponentName {
        return android.content.ComponentName(pkg, cls)
    }
}