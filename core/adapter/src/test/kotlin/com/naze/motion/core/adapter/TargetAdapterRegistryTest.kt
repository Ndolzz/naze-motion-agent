package com.naze.motion.core.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetAdapterRegistryTest {

    @Test
    fun knownTargetsContainAlightMotionAndCapCut() {
        val alight = TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE
        val capcut = TargetAdapterRegistry.CAPCUT_PACKAGE
        assertTrue(TargetAdapterRegistry.isKnown(alight))
        assertTrue(TargetAdapterRegistry.isKnown(capcut))
        assertEquals("Alight Motion", TargetAdapterRegistry.displayNameFor(alight))
        assertEquals("CapCut", TargetAdapterRegistry.displayNameFor(capcut))
        assertEquals(2, TargetAdapterRegistry.knownTargets.size)
    }

    @Test
    fun unknownPackageIsNotKnownAndFallsBackToItelf() {
        assertFalse(TargetAdapterRegistry.isKnown("com.example.unknown"))
        assertEquals(
            "com.example.unknown",
            TargetAdapterRegistry.displayNameFor("com.example.unknown"),
        )
    }

    @Test
    fun adapterClassResolvesForKnownTargetsOnly() {
        val alight = TargetAdapterRegistry.adapterClassFor(
            TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE,
        )
        val capcut = TargetAdapterRegistry.adapterClassFor(
            TargetAdapterRegistry.CAPCUT_PACKAGE,
        )
        assertTrue(alight != null)
        assertTrue(alight!!.qualifiedName?.endsWith("AlightMotionAdapter") == true)
        assertTrue(capcut != null)
        assertTrue(capcut!!.qualifiedName?.endsWith("CapCutAdapter") == true)
        assertNull(TargetAdapterRegistry.adapterClassFor("com.example.unknown"))
    }

    @Test
    fun candidatesCoverEveryInstalledVariant() {
        val alight = TargetAdapterRegistry.candidatesFor(
            TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE,
        )
        val capcut = TargetAdapterRegistry.candidatesFor(
            TargetAdapterRegistry.CAPCUT_PACKAGE,
        )
        assertEquals(
            listOf(
                TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE,
                TargetAdapterRegistry.ALIGHT_MOTION_TRIAL_PACKAGE,
            ),
            alight,
        )
        assertEquals(
            listOf(
                TargetAdapterRegistry.CAPCUT_PACKAGE,
                TargetAdapterRegistry.CAPCUT_CHINA_PACKAGE,
            ),
            capcut,
        )
        assertEquals(
            listOf("com.example.unknown"),
            TargetAdapterRegistry.candidatesFor("com.example.unknown"),
        )
    }
}
