package com.example.prototype

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FacebookAccessibilityService : AccessibilityService() {

    private var lastFbEventTime = 0L
    private val TAG = "FacebookAccessDebug"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service CONNECTED and READY.")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()

        // Log every event package to ensure we are receiving them
        // Log.v(TAG, "Event from package: $pkg")

        if (pkg != null && pkg.contains("facebook")) {
            Log.d(TAG, "DETECTED FACEBOOK! Package: $pkg")

            lastFbEventTime = System.currentTimeMillis()
            ScreenState.isFacebookOpen = true

            if (OverlayState.canTrigger()) {
                Log.d(TAG, "Triggering OverlayService...")
                try {
                    val intent = Intent(applicationContext, OverlayService::class.java)
                    startService(intent)
                    OverlayState.isVisible = true
                    Log.d(TAG, "startService(OverlayService) called successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "FAILED to start OverlayService", e)
                }
            } else {
                Log.d(TAG, "Overlay trigger debounced (waiting/already visible).")
            }

        } else if (System.currentTimeMillis() - lastFbEventTime > 2000) {
            if (ScreenState.isFacebookOpen) {
                Log.d(TAG, "Facebook closed (timeout).")
                ScreenState.isFacebookOpen = false
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Service Interrupted")
    }
}

object ScreenState {
    @Volatile var isFacebookOpen = false
}

object OverlayState {
    @Volatile var isVisible = false
    private var lastTriggerTime = 0L
    private const val DEBOUNCE_MS = 500L

    fun canTrigger(): Boolean {
        val now = System.currentTimeMillis()
        return !isVisible && now - lastTriggerTime > DEBOUNCE_MS
            .also { lastTriggerTime = now }
    }
}

