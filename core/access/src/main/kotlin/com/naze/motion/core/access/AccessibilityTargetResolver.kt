package com.naze.motion.core.access

import com.naze.motion.core.action.ActionQuery
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.ResolutionMethod
import com.naze.motion.core.action.ResolvedTarget
import com.naze.motion.core.action.TargetResolver

/**
 * Semantic target resolution on the accessibility tree (NMA-ACTION-008).
 * Priority order: resourceId, contentDescription, exact text, normalized
 * text, then explicit coordinate fallback which is always marked and
 * logged by the engine.
 */
class AccessibilityTargetResolver(
    private val driver: AutomationDriver,
) : TargetResolver {

    override suspend fun resolve(query: ActionQuery): ResolvedTarget? {
        val coordinateX = query.coordinateX
        val coordinateY = query.coordinateY
        if (coordinateX != null && coordinateY != null) {
            if (!query.coordinateFallbackAllowed) return null
            return ResolvedTarget(
                node = CoordinateNodeHandle(coordinateX, coordinateY),
                method = ResolutionMethod.COORDINATE_FALLBACK,
                usedCoordinateFallback = true,
            )
        }
        val handle = driver.find(query) ?: return null
        val method = when {
            query.resourceId != null -> ResolutionMethod.RESOURCE_ID
            query.contentDescription != null -> ResolutionMethod.CONTENT_DESCRIPTION
            query.text != null -> ResolutionMethod.EXACT_TEXT
            else -> ResolutionMethod.NORMALIZED_TEXT
        }
        return ResolvedTarget(handle, method, false)
    }
}
