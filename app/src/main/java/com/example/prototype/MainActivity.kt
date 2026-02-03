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
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusOverlay: TextView
    private lateinit var statusAccess: TextView
    private lateinit var statusNotif: TextView

    private val NOTIF_REQ_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
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