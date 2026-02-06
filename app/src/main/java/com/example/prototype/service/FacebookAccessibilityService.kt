package com.example.prototype.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.example.prototype.utils.sendConsoleUpdate // Import extension

class FacebookAccessibilityService : AccessibilityService() {

    private var lastFbEventTime = 0L
    private val TAG = "FacebookAccess"

    override fun onServiceConnected() {
        super.onServiceConnected()
        sendConsoleUpdate("System: Accessibility Connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString()

        if (pkg != null && (pkg.contains("facebook") || pkg.contains("katana") || pkg.contains("lite"))) {

            // Only update/log if state CHANGED to True
            if (!ScreenCaptureService.ScreenState.isFacebookOpen) {
                ScreenCaptureService.ScreenState.isFacebookOpen = true
                sendConsoleUpdate("App Event: Facebook Opened")
            }
            lastFbEventTime = System.currentTimeMillis()

        } else if (System.currentTimeMillis() - lastFbEventTime > 2000) {
            // Timeout Logic: If no FB event for 2 seconds, assume we left
            if (ScreenCaptureService.ScreenState.isFacebookOpen) {
                ScreenCaptureService.ScreenState.isFacebookOpen = false
                sendConsoleUpdate("App Event: Facebook Closed")
            }
        }
    }

    override fun onInterrupt() {}
}