package com.example.prototype.ui.child

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.prototype.R
import com.example.prototype.data.remote.FirebaseSyncManager
import com.example.prototype.ui.welcome.RoleSelectionActivity
import androidx.core.content.edit
import androidx.core.graphics.toColorInt

/**
 * Dashboard for the CHILD role.
 *
 * Responsibilities:
 * 1. Displaying the unique Device ID (for pairing).
 * 2. Checking/Requesting critical permissions (Overlay, etc.).
 * 3. Providing Debug controls (Force Upload, Reset).
 */
class ChildDashboardActivity : AppCompatActivity() {

    // --- CONSTANTS ---
    companion object {
        private const val PREFS_NAME = "AppConfig"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_ROLE = "role"
    }

    // --- UI COMPONENTS ---
    private lateinit var txtDeviceId: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtLogs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_child_dashboard)

        initializeViews()
        setupDashboardInfo()
        setupClickListeners()

        // Initial Permission Check
        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions every time the app comes into focus
        checkPermissions()
    }

    private fun initializeViews() {
        txtDeviceId = findViewById(R.id.txtChildDeviceId)
        txtStatus = findViewById(R.id.txtPermissionStatus)
        txtLogs = findViewById(R.id.txtChildLogs)
    }

    private fun setupDashboardInfo() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val myId = prefs.getString(KEY_DEVICE_ID, "ERROR") ?: "ERROR"
        txtDeviceId.text = myId
    }

    private fun setupClickListeners() {
        // 1. Show Pairing Code Dialog
        findViewById<Button>(R.id.btnTestLinkChild).setOnClickListener {
            showPairingDialog()
        }

        // 2. Debug: Force Cloud Upload
        findViewById<Button>(R.id.btnForceUpload).setOnClickListener {
            triggerManualSync()
        }

        // 3. Placeholder: Settings
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            Toast.makeText(this, "Settings feature pending...", Toast.LENGTH_SHORT).show()
        }

        // 4. Logout / Reset
        findViewById<Button>(R.id.btnLogoutChild).setOnClickListener {
            performLogout()
        }
    }

    private fun showPairingDialog() {
        val currentId = txtDeviceId.text.toString()

        AlertDialog.Builder(this)
            .setTitle("Pairing Code")
            .setMessage("Enter this code on the Parent Device:\n\n$currentId")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun triggerManualSync() {
        FirebaseSyncManager.syncPendingLogs(this)
        Toast.makeText(this, "Manual Sync Triggered", Toast.LENGTH_SHORT).show()
        appendLogToUI("Manual upload request sent.")
    }

    private fun performLogout() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        // LOGOUT LOGIC:
        // We remove 'role' so the Welcome Screen appears again.
        // We KEEP 'device_id' so the child doesn't get a new ID every time they restart.
        prefs.edit { remove(KEY_ROLE) }

        startActivity(Intent(this, RoleSelectionActivity::class.java))
        finish()
    }

    /**
     * Checks if the app has the necessary Android permissions.
     * Updates the Status UI accordingly.
     */
    private fun checkPermissions() {
        // 1. Check "Draw Over Other Apps" (Required for VirtualDisplay capture)
        if (Settings.canDrawOverlays(this)) {
            updateStatusUI(isActive = true)
            // Note: Service start logic would go here in production
        } else {
            updateStatusUI(isActive = false)
        }
    }

    private fun updateStatusUI(isActive: Boolean) {
        if (isActive) {
            txtStatus.text = "🟢 Active (Monitoring...)"
            txtStatus.setBackgroundColor("#E8F5E9".toColorInt()) // Light Green
            txtStatus.setTextColor("#2E7D32".toColorInt())     // Dark Green

            // Remove click listener if fixed
            txtStatus.setOnClickListener(null)
        } else {
            txtStatus.text = "🔴 Permissions Missing (Tap to Fix)"
            txtStatus.setBackgroundColor("#FFEBEE".toColorInt()) // Light Red
            txtStatus.setTextColor("#D32F2F".toColorInt())     // Dark Red

            // Add "Fix It" Action
            txtStatus.setOnClickListener {
                Toast.makeText(this, "Please allow 'Display over other apps'", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        }
    }

    /**
     * Helper to append debug messages to the on-screen log view.
     */
    private fun appendLogToUI(message: String) {
        val currentText = txtLogs.text.toString()
        val newText = "• $message\n$currentText"
        txtLogs.text = newText
    }
}