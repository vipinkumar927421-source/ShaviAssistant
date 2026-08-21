package com.vipin.shavi.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Only active once the user manually enables it in
 * Settings > Accessibility > Shavi. Used sparingly, only for actions Android
 * does not expose any other API for (e.g. tapping a specific button inside
 * a third-party app on the user's explicit command). This is intentionally
 * left as a scaffold — implement performGlobalAction / findAccessibilityNodeInfosByText
 * calls only for the specific, narrow actions your app actually needs, and
 * always confirm destructive actions with the user first.
 */
class ShaviAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intentionally empty scaffold — wire up specific UI-automation
        // actions here only as needed, scoped tightly to user-requested commands.
    }

    override fun onInterrupt() {}
}
