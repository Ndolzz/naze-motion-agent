package com.naze.motion.core.access

/**
 * Holds the live connection to MotionAccessibilityService (NMA-ACCESS-006).
 * The service attaches on connect and detaches on unbind or destroy so a
 * stale reference can never be used. Volatile reads, no locks needed.
 */
object AccessibilityConnection {
    @Volatile
    private var serviceRef: MotionAccessibilityService? = null

    @Volatile
    var connected: Boolean = false
        private set

    val service: MotionAccessibilityService?
        get() = serviceRef

    fun attach(service: MotionAccessibilityService) {
        serviceRef = service
        connected = true
    }

    fun detach(service: MotionAccessibilityService) {
        if (serviceRef === service) {
            serviceRef = null
            connected = false
        }
    }
}
