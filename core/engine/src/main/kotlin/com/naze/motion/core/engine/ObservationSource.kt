package com.naze.motion.core.engine

import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.domain.Observation
import com.naze.motion.core.domain.ScreenState
import com.naze.motion.core.domain.TargetApplication

/**
 * Captures an Observation through the AutomationDriver. Pure data mapping:
 * no interpretation, no retry. Returns null when the accessibility tree is
 * unavailable; the engine decides how to react (NMA-DOMAIN-007).
 */
class DriverObservationSource(private val driver: AutomationDriver) {

    suspend fun observe(target: TargetApplication): Observation? {
        val tree = driver.getAccessibilityTree() ?: return null
        val currentPackage = driver.getCurrentPackage()
        val screenState = when {
            currentPackage == null -> ScreenState.UNKNOWN
            currentPackage == target.packageName -> ScreenState.TARGET_APP
            else -> ScreenState.OTHER_APP
        }
        return Observation(
            currentPackage = currentPackage,
            visibleText = tree.visibleText,
            contentDescriptions = tree.contentDescriptions,
            screenState = screenState,
            timestampMs = System.currentTimeMillis(),
        )
    }
}
