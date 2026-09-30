package com.naze.motion.app.agent

import android.content.Context
import com.naze.motion.core.domain.AllowedApps

/**
 * AllowedAppsStore (Phase 23): persists the allowed applications list on
 * this device only. Validation lives in the core AllowedApps model; this
 * store only reads and writes already legal package names. The runtime
 * refuses any run whose target is not on the list.
 */
class AllowedAppsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<String> {
        val stored = prefs.getStringSet(KEY, null)
        if (stored == null) return AllowedApps.DEFAULT.packages
        // Keep a stable order: the on disk set is unordered.
        return stored.sorted()
    }

    /** Returns true when the name was valid, new, and stored. */
    fun add(packageName: String): Boolean {
        val current = AllowedApps(load())
        val next = current.plus(packageName) ?: return false
        persist(next.packages)
        return true
    }

    /** Removing is always allowed, even down to an empty list. */
    fun remove(packageName: String) {
        persist(AllowedApps(load()).minus(packageName).packages)
    }

    fun isAllowed(packageName: String): Boolean =
        AllowedApps(load()).isAllowed(packageName)

    private fun persist(packages: List<String>) {
        prefs.edit().putStringSet(KEY, packages.toSet()).apply()
    }

    companion object {
        private const val PREFS_NAME = "naze_allowed_apps"
        private const val KEY = "packages"
    }
}
