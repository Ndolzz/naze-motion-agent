package com.naze.motion.app.agent

import android.content.Context

/**
 * OnboardingStore (PART 2): persistent onboarding state. Keeps track of
 * whether the guided setup finished, whether the user skipped it (so the
 * dashboard can show a friendly reminder), and whether Beginner Mode is
 * on (default: on). The wizard itself never trusts the flag alone: every
 * step re-checks the real device state on entry.
 */
class OnboardingStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isCompleted(): Boolean = prefs.getBoolean(COMPLETED, false)

    fun setCompleted(done: Boolean) {
        prefs.edit().putBoolean(COMPLETED, done).apply()
    }

    fun isSkipped(): Boolean = prefs.getBoolean(SKIPPED, false)

    fun setSkipped(skipped: Boolean) {
        prefs.edit().putBoolean(SKIPPED, skipped).apply()
    }

    fun beginnerMode(): Boolean = prefs.getBoolean(BEGINNER, true)

    fun setBeginnerMode(enabled: Boolean) {
        prefs.edit().putBoolean(BEGINNER, enabled).apply()
    }

    companion object {
        private const val PREFS_NAME = "naze_onboarding"
        private const val COMPLETED = "onboardingCompleted"
        private const val SKIPPED = "setupSkipped"
        private const val BEGINNER = "beginnerMode"
    }
}
