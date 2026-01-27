package com.example.prototype

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FacebookAccessibilityService : AccessibilityService() {

    private var lastFbEventTime = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        Log.d("FacebookAccessibility", "Event from: $pkg") // log all events
        if (pkg.contains("facebook")) {
            lastFbEventTime = System.currentTimeMillis()
            ScreenState.isFacebookOpen = true
        } else if (System.currentTimeMillis() - lastFbEventTime > 2000) {
            ScreenState.isFacebookOpen = false
        }
    }

    override fun onInterrupt() {}
}

object ScreenState {
    @Volatile var isFacebookOpen = false
}