package com.naze.motion.core.adapter

import com.naze.motion.core.action.ActionQuery
import com.naze.motion.core.action.AutomationDriver
import com.naze.motion.core.action.NodeHandle
import kotlinx.coroutines.delay

/**
 * CapCutAdapter (Phase 25): the second TargetApplicationAdapter, and the
 * first proof that the registry foundation really isolates target
 * knowledge. Structurally identical to AlightMotionAdapter: it knows the
 * package name and a best-effort vocabulary of common screen elements,
 * and delegates every interaction to the injected AutomationDriver so
 * the engine never sees app-specific logic. The vocabulary is a
 * heuristic and will be verified against real devices in a later phase;
 * when the app UI differs, findKnown returns null and the planner falls
 * back to generic queries.
 *
 * The global build is com.lemon.lvoverseas and the Chinese JianYing
 * build is com.lemon.lv; candidatePackages accepts both so the device
 * preflight stops reporting an installed app missing.
 */
class CapCutAdapter(
    private val driver: AutomationDriver,
    private val sleep: suspend (Long) -> Unit = { delay(it) },
    private val nowMs: () -> Long = System::currentTimeMillis,
) : TargetApplicationAdapter {

    override val displayName: String = "CapCut"
    override val packageName: String = "com.lemon.lvoverseas"
    override val candidatePackages: Set<String> = setOf(
        TargetAdapterRegistry.CAPCUT_PACKAGE,
        TargetAdapterRegistry.CAPCUT_CHINA_PACKAGE,
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
         * Best-effort vocabulary of common CapCut UI elements. Normalized
         * text matches survive casing and surrounding labels.
         */
        private val KNOWN_QUERIES: Map<String, ActionQuery> = mapOf(
            "new_project" to ActionQuery(normalizedText = "new project"),
            "export" to ActionQuery(normalizedText = "export"),
            "add_audio" to ActionQuery(normalizedText = "add audio"),
            "add_text" to ActionQuery(normalizedText = "add text"),
            "add_sticker" to ActionQuery(normalizedText = "add sticker"),
            "add_overlay" to ActionQuery(normalizedText = "add overlay"),
            "play" to ActionQuery(contentDescription = "Play"),
            "pause" to ActionQuery(contentDescription = "Pause"),
            "undo" to ActionQuery(contentDescription = "Undo"),
            "redo" to ActionQuery(contentDescription = "Redo"),
        )
    }
}
