package com.naze.motion.app.agent

import android.content.Context
import com.naze.motion.core.adapter.TargetAdapterRegistry

/**
 * TargetSelectionStore (Phase 26): persists which target application the
 * agent drives, on this device only. The stored value is a registry package
 * name; anything unknown on load falls back to Alight Motion, and save
 * refuses packages the registry does not know, so the selection can never
 * point at an adapter that does not exist.
 */
class TargetSelectionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The selected target package, always a registry known package. */
    fun load(): String {
        val stored = prefs.getString(KEY, null) ?: return defaultPackage()
        return if (TargetAdapterRegistry.isKnown(stored)) stored else defaultPackage()
    }

    /** Returns true when the package is a known target and was stored. */
    fun save(packageName: String): Boolean {
        if (!TargetAdapterRegistry.isKnown(packageName)) return false
        prefs.edit().putString(KEY, packageName).apply()
        return true
    }

    private fun defaultPackage(): String = TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE

    companion object {
        private const val PREFS_NAME = "naze_target_selection"
        private const val KEY = "targetPackage"
    }
}
