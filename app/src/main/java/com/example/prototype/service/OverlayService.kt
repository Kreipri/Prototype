package com.example.prototype.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.example.prototype.R
import com.example.prototype.ui.child.CapturePermissionActivity

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var blockerView: View? = null
    private val TAG = "OverlayServiceDebug"

    private val captureStartReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.prototype.CAPTURE_STARTED") {
                Log.d(TAG, "Broadcast received: Capture started. Removing blocker.")
                Handler(Looper.getMainLooper()).post {
                    removeBlocker()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate called")
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val filter = IntentFilter("com.example.prototype.CAPTURE_STARTED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val exportFlag = if (Build.VERSION.SDK_INT >= 33) RECEIVER_NOT_EXPORTED else 0
            registerReceiver(captureStartReceiver, filter, exportFlag)
        } else {
            registerReceiver(captureStartReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")

        val isCaptureRunning = ScreenCaptureService.CaptureState.isRunning
        Log.d(TAG, "Is Capture Running? $isCaptureRunning")

        if (isCaptureRunning) {
            Log.d(TAG, "Capture is running, ensuring blocker is removed.")
            removeBlocker()
        } else {
            Log.d(TAG, "Capture NOT running. Attempting to show blocker.")
            showBlockingOverlay()
        }
        return START_STICKY
    }

    private fun showBlockingOverlay() {
        if (blockerView != null) {
            Log.d(TAG, "Blocker view already exists. Skipping.")
            return
        }

        // CRITICAL CHECK: Do we actually have permission?
        if (!Settings.canDrawOverlays(this)) {
            Log.e(TAG, "ERROR: Permission 'Draw over other apps' is MISSING. Cannot show overlay.")
            return
        }

        try {
            blockerView = LayoutInflater.from(this).inflate(R.layout.overlay_blocker, null)

            blockerView!!.findViewById<Button>(R.id.enableBtn).setOnClickListener {
                sendBroadcast(Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS))
                startPermissionActivity()
            }

            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT // Use TRANSLUCENT so the alpha in XML works
            )

            // Force the view to catch touches
            blockerView!!.isClickable = true
            blockerView!!.isFocusable = true

            windowManager.addView(blockerView, params)
            Log.d(TAG, "SUCCESS: Overlay added with BLOCKING flags.")

        } catch (e: Exception) {
            Log.e(TAG, "CRASH while adding overlay", e)
        }
    }

    private fun startPermissionActivity() {
        Log.d(TAG, "Starting CapturePermissionActivity...")
        val intent = Intent(this, CapturePermissionActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    private fun removeBlocker() {
        try {
            blockerView?.let {
                Log.d(TAG, "Removing blocker view.")
                windowManager.removeView(it)
                blockerView = null
            } ?: Log.d(TAG, "removeBlocker called, but view was null.")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing view", e)
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called")
        try {
            unregisterReceiver(captureStartReceiver)
        } catch (e: Exception) { }
        removeBlocker()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null
}