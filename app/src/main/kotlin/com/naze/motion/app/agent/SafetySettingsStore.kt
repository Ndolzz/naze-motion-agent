package com.naze.motion.app.agent

import android.content.Context
import com.naze.motion.core.domain.ExecutionProfile

/**
 * SafetySettingsStore (Phase 22): stores the configurable safety profile
 * on this device only. Values are coerced into a legal range before they
 * are persisted, so a run can never be configured with an unlimited or
 * negative limit. AgentRuntime applies the loaded profile to every run.
 */
class SafetySettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): ExecutionProfile = ExecutionProfile.of(
        actionTimeoutMs = prefs.getLong(TIMEOUT, 5_000L),
        maxAttempts = prefs.getInt(ATTEMPTS, 2),
        recoveryMaxAttempts = prefs.getInt(RECOVERY, 2),
        recoveryBackoffBaseMs = prefs.getLong(BACKOFF, 400L),
    )

    fun save(
        actionTimeoutMs: Long,
        maxAttempts: Int,
        recoveryMaxAttempts: Int,
        recoveryBackoffBaseMs: Long,
    ): ExecutionProfile {
        val profile = ExecutionProfile.of(
            actionTimeoutMs,
            maxAttempts,
            recoveryMaxAttempts,
            recoveryBackoffBaseMs,
        )
        prefs.edit()
            .putLong(TIMEOUT, profile.actionTimeoutMs)
            .putInt(ATTEMPTS, profile.maxAttempts)
            .putInt(RECOVERY, profile.recoveryMaxAttempts)
            .putLong(BACKOFF, profile.recoveryBackoffBaseMs)
            .apply()
        return profile
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "naze_safety"
        private const val TIMEOUT = "actionTimeoutMs"
        private const val ATTEMPTS = "maxAttempts"
        private const val RECOVERY = "recoveryMaxAttempts"
        private const val BACKOFF = "recoveryBackoffBaseMs"
    }
}
