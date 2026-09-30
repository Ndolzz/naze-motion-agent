package com.naze.motion.core.adapter

import kotlin.reflect.KClass

/**
 * TargetAdapterRegistry (Phase 24, second target in Phase 25): the
 * multi-adapter foundation. Every target application the agent knows how
 * to drive is registered here with its package name, a human readable
 * display name, and the adapter class that implements it. The engine,
 * the runtime, and the UI ask the registry instead of hardcoding package
 * names, so adding a target application means registering metadata plus
 * one adapter class here, and nothing else.
 *
 * The package string must match the adapter's packageName property and
 * must be the real installed package name, or the preflight install
 * check on the device fails even though the app is installed.
 */
object TargetAdapterRegistry {

    const val ALIGHT_MOTION_PACKAGE = "com.alightcreative.motion"
    const val CAPCUT_PACKAGE = "com.lemon.lvoverseas"

    data class TargetApp(
        val displayName: String,
        val packageName: String,
    )

    /** All target applications this build knows how to drive. */
    val knownTargets: List<TargetApp> = listOf(
        TargetApp("Alight Motion", ALIGHT_MOTION_PACKAGE),
        TargetApp("CapCut", CAPCUT_PACKAGE),
    )

    fun isKnown(packageName: String): Boolean =
        knownTargets.any { it.packageName == packageName }

    /**
     * Human readable name for logs and UI. Unknown packages fall back to
     * the raw package name so messages are never empty.
     */
    fun displayNameFor(packageName: String): String =
        knownTargets.firstOrNull { it.packageName == packageName }?.displayName
            ?: packageName

    /**
     * Adapter class that drives the given package, or null when unknown.
     * Callers construct the adapter with their own driver instance.
     */
    fun adapterClassFor(packageName: String): KClass<out TargetApplicationAdapter>? =
        when (packageName) {
            ALIGHT_MOTION_PACKAGE -> AlightMotionAdapter::class
            CAPCUT_PACKAGE -> CapCutAdapter::class
            else -> null
        }
}
