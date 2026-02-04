package com.example.prototype

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // UI References (Matching the NEW activity_main.xml)
    private lateinit var txtDeviceId: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Bind Views
        txtDeviceId = findViewById(R.id.txtChildDeviceId)
        txtStatus = findViewById(R.id.txtPermissionStatus)
        txtLogs = findViewById(R.id.txtChildLogs)

        // 2. Load & Display Device ID
        val prefs = getSharedPreferences("AppConfig", Context.MODE_PRIVATE)
        val myId = prefs.getString("device_id", "ERROR") ?: "ERROR"
        txtDeviceId.text = myId

        // 3. Test Link Button
        findViewById<Button>(R.id.btnTestLinkChild).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Pairing Code")
                .setMessage("Enter this code on the Parent Device:\n\n$myId")
                .setPositiveButton("OK", null)
                .show()
        }

        // 4. Force Upload Button
        findViewById<Button>(R.id.btnForceUpload).setOnClickListener {
            // Trigger your Sync Manager
            FirebaseSyncManager.syncPendingLogs(this)
            Toast.makeText(this, "Forcing Upload...", Toast.LENGTH_SHORT).show()
            updateLogs("Manual upload triggered.")
        }

        // 5. Settings Button (Placeholder)
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

        // 6. Logout Button
        findViewById<Button>(R.id.btnLogoutChild).setOnClickListener {
            // ❌ OLD: Wipes everything
            // prefs.edit().clear().apply()

            // ✅ NEW: Wipes only the role, keeps the ID
            prefs.edit().remove("role").apply()

            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        }

        // 7. Check Permissions
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions() // Re-check when coming back to app
    }

    private fun checkPermissions() {
        // Simple check for "Draw Overlays" (Required for your Screen Capture)
        if (Settings.canDrawOverlays(this)) {
            txtStatus.text = "🟢 Active (Monitoring...)"
            txtStatus.setBackgroundColor(Color.parseColor("#E8F5E9")) // Light Green
            txtStatus.setTextColor(Color.parseColor("#2E7D32"))     // Dark Green

            // Start your Service here if not running
            // startForegroundService(Intent(this, ScreenCaptureService::class.java))
        } else {
            txtStatus.text = "🔴 Permissions Missing (Tap to Fix)"
            txtStatus.setBackgroundColor(Color.parseColor("#FFEBEE")) // Light Red
            txtStatus.setTextColor(Color.parseColor("#D32F2F"))     // Dark Red

            txtStatus.setOnClickListener {
                Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }
    }

    // Helper to print logs to the UI
    private fun updateLogs(message: String) {
        val currentText = txtLogs.text.toString()
        val newText = "• $message\n$currentText"
        txtLogs.text = newText
    }
}