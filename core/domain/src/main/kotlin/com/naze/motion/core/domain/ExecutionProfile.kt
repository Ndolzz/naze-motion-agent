package com.naze.motion.core.domain

/**
 * ExecutionProfile (Phase 22): user configurable safety caps for one run.
 * Every value is an upper bound: the engine never lets an action wait
 * longer, retry more, or recover more often than the profile allows, no
 * matter what the plan or the provider requested (NMA-SEC-004, bounded
 * retry NMA-DOMAIN-004). Defaults match the historical hardcoded limits.
 */
data class ExecutionProfile(
    val actionTimeoutMs: Long = 5_000L,
    val maxAttempts: Int = 2,
    val recoveryMaxAttempts: Int = 2,
    val recoveryBackoffBaseMs: Long = 400L,
) {
    init {
        require(actionTimeoutMs > 0) { "actionTimeoutMs must be > 0" }
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
        require(recoveryMaxAttempts >= 1) { "recoveryMaxAttempts must be >= 1" }
        require(recoveryBackoffBaseMs >= 0) { "recoveryBackoffBaseMs must be >= 0" }
    }

    /** Effective timeout for one action: never above the action's own timeout. */
    fun timeoutCapFor(action: Action): Long = minOf(action.timeoutMs, actionTimeoutMs)

    /** Effective retry attempts for one action: never above its own policy. */
    fun attemptsCapFor(action: Action): Int =
        minOf(action.retryPolicy.maxAttempts, maxAttempts)

    /** Backoff before the given recovery attempt (1 based). */
    fun recoveryBackoffMs(attempt: Int): Long = recoveryBackoffBaseMs * attempt

    companion object {
        /** Coerces raw user input into a legal profile. */
        fun of(
            actionTimeoutMs: Long,
            maxAttempts: Int,
            recoveryMaxAttempts: Int,
            recoveryBackoffBaseMs: Long,
        ): ExecutionProfile = ExecutionProfile(
            actionTimeoutMs = actionTimeoutMs.coerceIn(100L, 60_000L),
            maxAttempts = maxAttempts.coerceIn(1, 5),
            recoveryMaxAttempts = recoveryMaxAttempts.coerceIn(1, 5),
            recoveryBackoffBaseMs = recoveryBackoffBaseMs.coerceIn(0L, 5_000L),
        )
    }
}
