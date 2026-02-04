package com.example.prototype.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

/**
 * Monitors app state to detect when Facebook is launched.
 * Coordinates with OverlayState to manage debounced triggers.
 */
class FacebookAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FbAccessibility"
        private const val FB_PACKAGE_KEY = "facebook"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName.contains(FB_PACKAGE_KEY)) {
            handleFacebookDetected()
        } else {
            handleAppSwitch()
        }
    }

    private fun handleFacebookDetected() {
        ScreenState.isFacebookOpen = true

        if (OverlayState.canTrigger()) {
            val intent = Intent(this, OverlayService::class.java)
            startService(intent)
            OverlayState.isVisible = true
            Log.d(TAG, "Overlay triggered for Facebook.")
        }
    }

    private fun handleAppSwitch() {
        // Optional: Logic to handle when user leaves Facebook
        ScreenState.isFacebookOpen = false
    }

    override fun onInterrupt() {}

    // --- STATE MANAGEMENT ---

    object ScreenState {
        @Volatile var isFacebookOpen = false
    }

    object OverlayState {
        @Volatile var isVisible = false
        private var lastTriggerTime = 0L
        private const val DEBOUNCE_MS = 1000L

        fun canTrigger(): Boolean {
            val now = System.currentTimeMillis()
            val hasWaitPeriodPassed = (now - lastTriggerTime) > DEBOUNCE_MS

            if (!isVisible && hasWaitPeriodPassed) {
                lastTriggerTime = now
                return true
            }
            return false
        }
    }
}