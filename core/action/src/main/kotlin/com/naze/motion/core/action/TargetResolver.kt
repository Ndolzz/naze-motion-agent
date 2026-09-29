package com.naze.motion.core.action

/**
 * Target resolution priority (NMA-ACTION-008):
 * 1 resourceId
 * 2 contentDescription
 * 3 exact text
 * 4 normalized text
 * 5 accessibility hierarchy
 * 6 semantic relationship
 * 7 coordinate fallback (explicit, logged, bounded, optional)
 */
enum class ResolutionMethod {
    RESOURCE_ID,
    CONTENT_DESCRIPTION,
    EXACT_TEXT,
    NORMALIZED_TEXT,
    ACCESSIBILITY_HIERARCHY,
    SEMANTIC_RELATIONSHIP,
    COORDINATE_FALLBACK,
}

data class ResolvedTarget(
    val node: NodeHandle,
    val method: ResolutionMethod,
    /** True when coordinate fallback was used; must always be logged. */
    val usedCoordinateFallback: Boolean = false,
)

interface TargetResolver {
    suspend fun resolve(query: ActionQuery): ResolvedTarget?
}
