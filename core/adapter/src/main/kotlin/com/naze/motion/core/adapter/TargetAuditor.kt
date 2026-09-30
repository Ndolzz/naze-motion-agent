package com.naze.motion.core.adapter

/**
 * One vocabulary audit result (Phase 26): whether the known UI element
 * with this id resolved on the screen that was audited.
 */
data class VocabularyAuditEntry(
    val id: String,
    val found: Boolean,
)

/**
 * TargetAuditor (Phase 26): verifies a target adapter's best-effort
 * vocabulary against a real screen through the injected driver. For every
 * known query id it resolves the element the same way a run would and
 * reports found or not found per entry. The auditor is a pure pass over
 * the adapter: it holds no state, adds nothing to the vocabulary, and is
 * fully testable on the JVM with FakeAutomationDriver. On a real device
 * the result depends on the screen that was in the foreground during the
 * audit, so the runtime launches the selected target first and gives it
 * a moment to settle before reading the tree.
 */
object TargetAuditor {

    /**
     * Audits every known query id of the adapter, sorted by id for a
     * stable report order. Never throws: resolution failures are reported
     * as not found entries.
     */
    suspend fun audit(adapter: TargetApplicationAdapter): List<VocabularyAuditEntry> =
        adapter.knownQueryIds.sorted().map { id ->
            val found = runCatching { adapter.findKnown(id) != null }.getOrDefault(false)
            VocabularyAuditEntry(id = id, found = found)
        }
}
