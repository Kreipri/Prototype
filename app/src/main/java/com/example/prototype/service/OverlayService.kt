package com.example.prototype.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.example.prototype.R
import com.example.prototype.ui.child.CapturePermissionActivity

// --- JETPACK COMPOSE UI ---
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*

// --- COMPOSE MATERIAL & ICONS ---
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

// --- COMPOSE RUNTIME & TOOLS ---
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*

/**
 * Manages the full-screen blocking overlay.
 * Uses a BroadcastReceiver to self-dismiss once capture is verified.
 */
class OverlayService : Service() {

    companion object {
        private const val TAG = "OverlayService"
        const val ACTION_CAPTURE_STARTED = "com.example.prototype.CAPTURE_STARTED"
    }

    private lateinit var windowManager: WindowManager
    private var blockerView: View? = null

    /**
     * Listener that removes the overlay once the ScreenCaptureService
     * confirms it has successfully started.
     */
    private val captureStartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_CAPTURE_STARTED) {
                removeBlocker()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val filter = IntentFilter(ACTION_CAPTURE_STARTED)
        registerReceiver(captureStartReceiver, filter, RECEIVER_NOT_EXPORTED)

        showBlocker()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun showBlocker() {
        if (blockerView != null) return

        try {
            blockerView = LayoutInflater.from(this).inflate(R.layout.service_overlay_blocker, null)

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            blockerView?.findViewById<Button>(R.id.enableBtn)?.setOnClickListener {
                startPermissionActivity()
            }

            windowManager.addView(blockerView, params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show overlay: ${e.message}")
        }
    }

    private fun startPermissionActivity() {
        val intent = Intent(this, CapturePermissionActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun removeBlocker() {
        blockerView?.let {
            windowManager.removeView(it)
            blockerView = null
            stopSelf() // Stop the service once overlay is gone
        }
    }

    override fun onDestroy() {
        unregisterReceiver(captureStartReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}

// --- COMPOSABLE ---

@Composable
fun MonitoringBlocker(onEnableClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Monitoring Required", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text("To continue using Facebook, you must enable screen monitoring.", fontSize = 16.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onEnableClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Enable Monitor", color = Color.White)
                }
            }
        }
    }
}

// --- PREVIEW ---
@Preview(showBackground = true)
@Composable
fun MonitoringBlockerPreview() {
    MaterialTheme {
        // We wrap it in a Box to simulate the full-screen overlay effect
        Box(Modifier.fillMaxSize()) {
            MonitoringBlocker(onEnableClick = {})
        }
    }
}