package com.naze.motion.core.adapter

import com.naze.motion.core.engine.fake.FakeAutomationDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TargetAuditorTest {

    private fun capcut(driver: FakeAutomationDriver) = CapCutAdapter(
        driver = driver,
        sleep = { /* no real waiting in tests */ },
        nowMs = { 0L },
    )

    private fun alight(driver: FakeAutomationDriver) = AlightMotionAdapter(
        driver = driver,
        sleep = { /* no real waiting in tests */ },
        nowMs = { 0L },
    )

    @Test
    fun auditMarksEveryKnownIdFoundWhenDriverResolves() = runTest {
        val adapter = capcut(FakeAutomationDriver())
        val entries = TargetAuditor.audit(adapter)
        assertEquals(adapter.knownQueryIds.size, entries.size)
        assertTrue(entries.all { it.found })
    }

    @Test
    fun auditMarksEntriesNotFoundWhenResolutionFails() = runTest {
        val driver = FakeAutomationDriver()
        driver.findFailuresRemaining = Int.MAX_VALUE
        val adapter = alight(driver)
        val entries = TargetAuditor.audit(adapter)
        assertEquals(adapter.knownQueryIds.size, entries.size)
        assertTrue(entries.none { it.found })
    }

    @Test
    fun auditEntriesAreSortedAndComplete() = runTest {
        val adapter = alight(FakeAutomationDriver())
        val entries = TargetAuditor.audit(adapter)
        assertEquals(adapter.knownQueryIds.sorted(), entries.map { it.id })
    }

    @Test
    fun auditMarksMixedResultsPerEntry() = runTest {
        val driver = FakeAutomationDriver()
        val adapter = alight(driver)
        // Every resolution after the first one fails, so exactly one
        // entry is found.
        driver.findFailuresRemaining = adapter.knownQueryIds.sorted().size - 1
        val entries = TargetAuditor.audit(adapter)
        assertEquals(1, entries.count { it.found })
        assertFalse(entries.any { it.id.isBlank() })
    }
}
