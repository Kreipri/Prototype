package com.example.prototype

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class FacebookAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == "com.facebook.katana") {
            ScreenState.isFacebookOpen = true
        } else {
            ScreenState.isFacebookOpen = false
        }
    }

    override fun onInterrupt() {}
}

object ScreenState {
    @Volatile var isFacebookOpen = false
}