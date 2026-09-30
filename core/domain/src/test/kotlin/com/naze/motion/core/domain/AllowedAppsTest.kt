package com.naze.motion.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AllowedAppsTest {

    @Test fun defaultListAllowsAlightMotion() {
        val apps = AllowedApps.DEFAULT
        assertTrue(apps.isAllowed("com.alightmotion.motion"))
        assertTrue(apps.isNotEmpty())
        assertEquals(1, apps.packages.size)
    }

    @Test fun validatesPackageNameStructure() {
        assertTrue(AllowedApps.isValidPackageName("com.alightmotion.motion"))
        assertTrue(AllowedApps.isValidPackageName("org.example.app2"))
        assertTrue(AllowedApps.isValidPackageName("a.b"))
        assertFalse(AllowedApps.isValidPackageName(""))
        assertFalse(AllowedApps.isValidPackageName("alightmotion"))
        assertFalse(AllowedApps.isValidPackageName("Com.Wrong"))
        assertFalse(AllowedApps.isValidPackageName("com."))
        assertFalse(AllowedApps.isValidPackageName(".com.a"))
        assertFalse(AllowedApps.isValidPackageName("com..a"))
        assertFalse(AllowedApps.isValidPackageName("com.alight motion"))
    }

    @Test fun plusIgnoresDuplicatesAndInvalidNames() {
        val apps = AllowedApps.DEFAULT
        assertNull(apps.plus("com.alightmotion.motion"))
        assertNull(apps.plus("not a package"))
        assertNull(apps.plus("single"))
        val grown = apps.plus("org.example.editor")
        assertTrue(grown!!.isAllowed("org.example.editor"))
        assertTrue(grown.isAllowed("com.alightmotion.motion"))
        assertEquals(2, grown.packages.size)
    }

    @Test fun plusTrimsSurroundingWhitespace() {
        val grown = AllowedApps.DEFAULT.plus("  org.example.editor  ")
        assertTrue(grown!!.isAllowed("org.example.editor"))
    }

    @Test fun listIsBounded() {
        var apps = AllowedApps(emptyList())
        for (i in 1..AllowedApps.MAX_ENTRIES) {
            apps = apps.plus("com.app$i")!!
        }
        assertNull(apps.plus("com.one.more"))
    }

    @Test fun minusCanEmptyTheList() {
        val apps = AllowedApps.DEFAULT.minus("com.alightmotion.motion")
        assertFalse(apps.isNotEmpty())
        assertFalse(apps.isAllowed("com.alightmotion.motion"))
    }

    @Test fun rejectsIllegalConstruction() {
        assertTrue(
            runCatching { AllowedApps(listOf("not a package")) }.isFailure,
        )
    }
}
