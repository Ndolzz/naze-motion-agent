package com.naze.motion.core.engine

import com.naze.motion.core.domain.VerificationResult
import kotlinx.coroutines.delay

/**
 * Bounded recovery (NMA-ERR-003, NMA-STATE-002 recovery path).
 * Rechecks the verification after a backoff, up to maxAttempts.
 * Never loops forever; cancellation is honored between attempts.
 */
class RecoveryManager(
    private val maxAttempts: Int = 2,
    private val backoffMs: (attempt: Int) -> Long = { attempt -> 400L * attempt },
    private val sleep: suspend (Long) -> Unit = { delay(it) },
) {
    init {
        require(maxAttempts >= 1) { "maxAttempts must be >= 1" }
    }

    data class RecoveryOutcome(
        val succeeded: Boolean,
        val attemptsUsed: Int,
        val lastReason: String?,
    )

    suspend fun attempt(
        isCancelled: () -> Boolean,
        recheck: suspend () -> VerificationResult,
    ): RecoveryOutcome {
        var last: VerificationResult? = null
        for (attemptNo in 1..maxAttempts) {
            if (isCancelled()) return RecoveryOutcome(false, attemptNo, "cancelled during recovery")
            sleep(backoffMs(attemptNo))
            if (isCancelled()) return RecoveryOutcome(false, attemptNo, "cancelled during recovery")
            last = recheck()
            if (last.passed) return RecoveryOutcome(true, attemptNo, null)
        }
        return RecoveryOutcome(false, maxAttempts, last?.reason)
    }
}
