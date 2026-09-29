package com.naze.motion.core.access

import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.engine.DriverObservationSource
import com.naze.motion.core.engine.ObservationProvider

/**
 * AccessibilityObservationProvider (NMA-ACCESS-005): observation over the
 * accessibility tree. Vision based observation stays pluggable later and
 * is not a dependency here.
 */
class AccessibilityObservationProvider(
    driver: AutomationDriver,
) : ObservationProvider by DriverObservationSource(driver)
