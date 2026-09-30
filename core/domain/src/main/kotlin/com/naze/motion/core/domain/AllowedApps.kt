package com.naze.motion.core.domain

/**
 * AllowedApps (Phase 23): the allowlist of applications the agent may
 * open and drive. The list is pure data: the app layer persists it and
 * the runtime refuses a run whose target is not on the list
 * (NMA-SEC-004). Package names are validated structurally, the list is
 * deduplicated, and it is bounded so it can never grow unbounded.
 */
data class AllowedApps(val packages: List<String>) {
    init {
        require(packages.size <= MAX_ENTRIES) { "too many allowed apps" }
        require(packages.all { isValidPackageName(it) }) {
            "invalid package name in allowed list"
        }
    }

    /** True when the agent may open and drive the given package. */
    fun isAllowed(packageName: String): Boolean = packages.contains(packageName)

    /** False only when the list is empty (every run would be refused). */
    fun isNotEmpty(): Boolean = packages.isNotEmpty()

    /**
     * Returns a copy with the package added, or null when the name is
     * invalid, already present, or the list is full. Nothing is guessed.
     */
    fun plus(packageName: String): AllowedApps? {
        val clean = packageName.trim()
        if (!isValidPackageName(clean) || packages.contains(clean)) return null
        if (packages.size >= MAX_ENTRIES) return null
        return AllowedApps(packages + clean)
    }

    /** Returns a copy without the package; removing is always allowed. */
    fun minus(packageName: String): AllowedApps = AllowedApps(packages - packageName)

    companion object {
        const val MAX_ENTRIES = 20

        /** Default allowlist: the only adapter today is Alight Motion. */
        val DEFAULT = AllowedApps(listOf("com.alightmotion.motion"))

        /**
         * Structural Android package name check: lowercase segments
         * separated by dots, at least two segments, no empty segment.
         */
        fun isValidPackageName(name: String): Boolean =
            Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$").matches(name)
    }
}
