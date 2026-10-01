package com.naze.motion.core.adapter

import com.naze.motion.core.action.ActionQuery
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.NodeHandle
import kotlinx.coroutines.delay

/**
 * AlightMotionAdapter (NMA-ARCH-007, first TargetApplicationAdapter).
 * Knows the package name and a best-effort vocabulary of common screen
 * elements; every interaction is delegated to the AutomationDriver so the
 * engine never sees app-specific logic. Queries are heuristics: when the
 * app UI differs, findKnown returns null and the planner falls back to
 * generic queries (NMA-ACTION-008 priority).
 *
 * The Play Store build is com.alightcreative.motion and the direct/trial
 * download is com.alightcreative.motion.trial; candidatePackages accepts
 * both so the device preflight stops reporting an installed app missing.
 */
class AlightMotionAdapter(
    private val driver: AutomationDriver,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val nowMs: () -> Long = System::currentTimeMillis,
) : TargetApplicationAdapter {

    override val displayName: String = "Alight Motion"
    override val packageName: String = "com.alightcreative.motion"
    override val candidatePackages: Set<String> = setOf(
        TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE,
        TargetAdapterRegistry.ALIGHT_MOTION_TRIAL_PACKAGE,
    )

    override suspend fun isOpen(): Boolean =
        candidatePackages.contains(driver.getCurrentPackage())

    override suspend fun open(): Boolean {
        if (isOpen()) return true
        for (candidate in candidatePackages) {
            if (!driver.launchApp(candidate)) continue
            if (waitForForeground(FOREGROUND_TIMEOUT_MS, FOREGROUND_POLL_MS)) return true
        }
        return false
    }

    override suspend fun waitForForeground(timeoutMs: Long, pollMs: Long): Boolean {
        if (pollMs <= 0) throw IllegalArgumentException("pollMs must be positive")
        val deadline = nowMs() + timeoutMs
        while (nowMs() <= deadline) {
            if (candidatePackages.contains(driver.getCurrentPackage())) return true
            sleep(pollMs)
        }
        return candidatePackages.contains(driver.getCurrentPackage())
    }

    override val knownQueryIds: Set<String> = KNOWN_QUERIES.keys

    override fun knownQuery(id: String): ActionQuery? = KNOWN_QUERIES[id]

    override suspend fun findKnown(id: String): NodeHandle? {
        val query = KNOWN_QUERIES[id] ?: return null
        return driver.find(query)
    }

    companion object {
        const val FOREGROUND_TIMEOUT_MS = 8_000L
        const val FOREGROUND_POLL_MS = 200L

        /**
         * Best-effort vocabulary of common Alight Motion UI elements.
         * Normalized text matches survive casing and surrounding labels.
         */
        private val KNOWN_QUERIES: Map<String, ActionQuery> = mapOf(
            "new_project" to ActionQuery(normalizedText = "new project"),
            "open_project" to ActionQuery(normalizedText = "open project"),
            "export" to ActionQuery(normalizedText = "export"),
            "add_media" to ActionQuery(normalizedText = "add media"),
            "add_text" to ActionQuery(normalizedText = "add text"),
            "add_layer" to ActionQuery(normalizedText = "add layer"),
            "play" to ActionQuery(contentDescription = "Play"),
            "pause" to ActionQuery(contentDescription = "Pause"),
            "undo" to ActionQuery(contentDescription = "Undo"),
            "redo" to ActionQuery(contentDescription = "Redo"),
        )
    }
}
