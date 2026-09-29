package com.naze.motion.core.access

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent

/**
 * The user enabled accessibility service. No automation, AI, or target app
 * logic lives here (NMA-ACCESS-007): this class only tracks connection
 * state and forwards nothing. All interaction goes through
 * AndroidAccessibilityDriver against this service instance.
 */
class MotionAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityConnection.attach(this)
    }

    override fun onUnbind(intent: Intent): Boolean {
        AccessibilityConnection.detach(this)
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AccessibilityConnection.detach(this)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Intentionally empty: the driver pulls state on demand.
    }

    override fun onInterrupt() {
        // Nothing to interrupt: the engine owns cancellation.
    }
}
