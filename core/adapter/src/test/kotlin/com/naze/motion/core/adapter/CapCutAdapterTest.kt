package com.naze.motion.core.adapter

import com.naze.motion.core.engine.fake.FakeAutomationDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CapCutAdapterTest {

    private val capcutPackage = "com.lemon.lvoverseas"

    private fun adapter(driver: FakeAutomationDriver) = CapCutAdapter(
        driver = driver,
        sleep = { /* no real waiting in tests */ },
        nowMs = { 0L },
    )

    @Test
    fun isOpenTrueWhenCapCutIsForeground() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = capcutPackage
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
        assertEquals(capcutPackage, driver.currentPackage)
    }

    @Test
    fun knownQueryIdsCoverCoreVocabulary() {
        val adapter = adapter(FakeAutomationDriver())
        assertTrue("new_project" in adapter.knownQueryIds)
        assertTrue("export" in adapter.knownQueryIds)
        assertTrue("add_text" in adapter.knownQueryIds)
        assertNotNull(adapter.knownQuery("new_project"))
        assertNull(adapter.knownQuery("unknown_id"))
    }

    @Test
    fun findKnownResolvesThroughDriver() = runTest {
        val driver = FakeAutomationDriver()
        driver.currentPackage = capcutPackage
        val adapter = adapter(driver)
        val handle = adapter.findKnown("new_project")
        assertNotNull(handle)
        assertTrue(driver.calls.contains("find"))
    }
}
