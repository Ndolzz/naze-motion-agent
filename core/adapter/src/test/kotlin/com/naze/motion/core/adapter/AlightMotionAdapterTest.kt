package com.naze.motion.core.adapter

import com.naze.motion.core.engine.fake.FakeAutomationDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AlightMotionAdapterTest {

    private fun adapter(
        driver: FakeAutomationDriver,
        virtualNow: Array<Long> = arrayOf(0L, 10_000L),
    ) = AlightMotionAdapter(
        driver = driver,
        sleep = { /* no real waiting in tests */ },
        nowMs = {
            val value = virtualNow[0]
            virtualNow[0] += 1
            value
        },
    )

    @Test
    fun isOpenTrueWhenTargetPackageIsForeground() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        assertTrue(adapter.isOpen())
    }

    @Test
    fun isOpenFalseWhenAnotherPackageIsForeground() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = "com.android.launcher"
        val adapter = adapter(driver)
        assertFalse(adapter.isOpen())
    }

    @Test
    fun openLaunchesWhenClosed() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = "com.android.launcher"
        val adapter = adapter(driver)
        assertTrue(adapter.open())
        assertTrue(driver.calls.contains("launchApp"))
        assertEquals("com.alightcreative.motion", driver.currentPackage)
    }

    @Test
    fun openSkipsLaunchWhenAlreadyOpen() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        assertTrue(adapter.open())
        assertFalse(driver.calls.contains("launchApp"))
    }

    @Test
    fun openFailsWhenLaunchFails() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = "com.android.launcher"
        driver.launchFailuresRemaining = 1
        val adapter = adapter(driver)
        assertFalse(adapter.open())
    }

    @Test
    fun waitForForegroundTimesOutWhenPackageNeverMatches() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = "com.android.launcher"
        val adapter = adapter(driver)
        val result = adapter.waitForForeground(timeoutMs = 5_000L, pollMs = 200L)
        assertFalse(result)
    }

    @Test
    fun knownQueryIdsCoverCoreVocabulary() {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        assertTrue("new_project" in adapter.knownQueryIds)
        assertTrue("export" in adapter.knownQueryIds)
        assertNotNull(adapter.knownQuery("new_project"))
        assertNull(adapter.knownQuery("unknown_id"))
    }

    @Test
    fun findKnownResolvesThroughDriver() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        val handle = adapter.findKnown("new_project")
        assertNotNull(handle)
        assertTrue(driver.calls.contains("find"))
    }

    @Test
    fun findKnownUnknownIdReturnsNullWithoutDriverCall() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        assertNull(adapter.findKnown("unknown_id"))
        assertFalse(driver.calls.contains("find"))
    }

    @Test
    fun waitForForegroundRejectsNonPositivePoll() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = adapter(driver)
        var thrown = false
        try {
            adapter.waitForForeground(timeoutMs = 1_000L, pollMs = 0L)
        } catch (e: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
