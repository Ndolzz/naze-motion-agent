package com.naze.motion.core.adapter

import com.naze.motion.core.action.ActionQuery

/**
 * Target-specific behavior lives behind this interface so the engine and
 * AI planner never import app-specific knowledge (NMA-ARCH-007).
 * Implementations must be stateless regarding automation: all interaction
 * goes through the injected AutomationDriver, so the adapter stays testable
 * with FakeAutomationDriver on the JVM.
 */
interface TargetApplicationAdapter {

    /** Human readable app name, e.g. for logs and UI. */
    val displayName: String

    /** Package the adapter drives, e.g. com.alightmotion.motion. */
    val packageName: String

    /** True when the target package is currently in the foreground. */
    suspend fun isOpen(): Boolean

    /**
     * Brings the target app to the foreground. Returns true when the app
     * is open and in the foreground within the default timeout. Never
     * throws: failures are false results (NMA-ACCESS-006 style).
     */
    suspend fun open(): Boolean

    /**
     * Polls the foreground package until it matches, bounded by timeoutMs.
     * Returns false on timeout without throwing.
     */
    suspend fun waitForForeground(timeoutMs: Long, pollMs: Long): Boolean

    /** Stable ids of the best-effort known UI queries. */
    val knownQueryIds: Set<String>

    /** Best-effort query for a known UI element, or null for unknown ids. */
    fun knownQuery(id: String): ActionQuery?

    /** Resolves a known UI element through the driver, or null. */
    suspend fun findKnown(id: String): com.naze.motion.core.action.NodeHandle?
}
