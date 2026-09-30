package com.naze.motion.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExecutionProfileTest {

    @Test fun defaultsMatchHistoricalEngineLimits() {
        val profile = ExecutionProfile()
        assertEquals(5_000L, profile.actionTimeoutMs)
        assertEquals(2, profile.maxAttempts)
        assertEquals(2, profile.recoveryMaxAttempts)
        assertEquals(400L, profile.recoveryBackoffBaseMs)
    }

    @Test fun rejectsIllegalValues() {
        assertFailsWith<IllegalArgumentException> { ExecutionProfile(actionTimeoutMs = 0L) }
        assertFailsWith<IllegalArgumentException> { ExecutionProfile(maxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> { ExecutionProfile(recoveryMaxAttempts = 0) }
        assertFailsWith<IllegalArgumentException> { ExecutionProfile(recoveryBackoffBaseMs = -1L) }
    }

    @Test fun ofCoercesRawInputIntoRange() {
        val profile = ExecutionProfile.of(1L, 99, 0, 100_000L)
        assertEquals(100L, profile.actionTimeoutMs)
        assertEquals(5, profile.maxAttempts)
        assertEquals(1, profile.recoveryMaxAttempts)
        assertEquals(5_000L, profile.recoveryBackoffBaseMs)
    }

    @Test fun capsNeverExceedTheActionPolicy() {
        val profile = ExecutionProfile(actionTimeoutMs = 10_000L, maxAttempts = 5)
        val action = Action(
            id = "a1",
            type = ActionType.WAIT,
            timeoutMs = 1_000L,
            retryPolicy = RetryPolicy(maxAttempts = 2),
        )
        assertEquals(1_000L, profile.timeoutCapFor(action))
        assertEquals(2, profile.attemptsCapFor(action))
    }

    @Test fun capsTightenTheActionPolicy() {
        val profile = ExecutionProfile(actionTimeoutMs = 1_000L, maxAttempts = 1)
        val action = Action(
            id = "a1",
            type = ActionType.WAIT,
            timeoutMs = 30_000L,
            retryPolicy = RetryPolicy(maxAttempts = 5),
        )
        assertEquals(1_000L, profile.timeoutCapFor(action))
        assertEquals(1, profile.attemptsCapFor(action))
    }

    @Test fun backoffScalesWithAttemptNumber() {
        val profile = ExecutionProfile(recoveryBackoffBaseMs = 250L)
        assertEquals(250L, profile.recoveryBackoffMs(1))
        assertEquals(500L, profile.recoveryBackoffMs(2))
        assertEquals(0L, ExecutionProfile(recoveryBackoffBaseMs = 0L).recoveryBackoffMs(3))
    }
}
