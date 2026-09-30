package com.naze.motion.core.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TargetAdapterRegistryTest {

    @Test
    fun knownTargetsContainAlightMotion() {
        val pkg = TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE
        assertTrue(TargetAdapterRegistry.isKnown(pkg))
        assertEquals("Alight Motion", TargetAdapterRegistry.displayNameFor(pkg))
        assertEquals(1, TargetAdapterRegistry.knownTargets.size)
    }

    @Test
    fun unknownPackageIsNotKnownAndFallsBackToItself() {
        assertFalse(TargetAdapterRegistry.isKnown("com.example.unknown"))
        assertEquals(
            "com.example.unknown",
            TargetAdapterRegistry.displayNameFor("com.example.unknown"),
        )
    }

    @Test
    fun adapterClassResolvesForKnownTargetsOnly() {
        val known = TargetAdapterRegistry.adapterClassFor(
            TargetAdapterRegistry.ALIGHT_MOTION_PACKAGE,
        )
        assertTrue(known != null)
        assertTrue(known!!.qualifiedName?.endsWith("AlightMotionAdapter") == true)
        assertNull(TargetAdapterRegistry.adapterClassFor("com.example.unknown"))
    }
}
